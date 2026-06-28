#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Import a curated subset of DeepMind CodeContests into the VOJ sample data.

Source : https://huggingface.co/datasets/deepmind/code_contests  (licensed CC BY 4.0).
Output : idempotently splices problems/checkpoints/relationships into sql/demo.sql
         (wrapped in `-- >>> CODECONTESTS BEGIN/END` markers) and rebuilds the
         `voj_problem_tags` vocabulary in sql/seed.sql from the selected problems'
         cf_tags. Re-running replaces the previous import.

It pulls rows from the HuggingFace datasets-server API (no bulk download), keeps only
Codeforces-style problems that can be judged by exact match (interactive / floating-point
/ "print any" problems are skipped), parses each statement into description + input format
+ output format (dropping the Example/Note block, since samples live in their own fields),
and takes up to MAX_TESTS of the smallest available test cases per problem.

Usage:  python scripts/import_codecontests.py
Requires only the Python standard library.
"""

import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEMO = os.path.join(ROOT, "sql", "demo.sql")
SEED = os.path.join(ROOT, "sql", "seed.sql")

API = "https://datasets-server.huggingface.co/rows"
DATASET = "deepmind/code_contests"

PROBLEM_BASE_ID = 2000
TAG_BASE_ID = 3  # seed ships tag ids 1 (greedy) and 2 (dynamic-programming)
EXISTING_TAGS = {
    "greedy": 1,
    "dp": 2,
    "dynamic programming": 2,
    "dynamic-programming": 2,
}
PER_TIER = 8  # easy / medium / hard
MAX_TESTS = 10  # checkpoints per problem
TEST_SIZE_CAP = 8192  # skip a single test whose input+output exceeds this
TIERS = [(1, 800, 1200), (2, 1300, 1700), (3, 1800, 2600)]

SOURCE_NAME = {
    0: "competitive programming archives",
    1: "CodeChef",
    2: "Codeforces",
    3: "HackerEarth",
    4: "Google Code Jam",
    5: "AtCoder",
    6: "Aizu",
}

# Phrases that signal a non-exact-match (special judge / floating point / multiple answers).
SPECIAL = (
    "absolute or relative error",
    "relative or absolute error",
    "absolute error",
    "relative error",
    "considered correct if",
    "interactive problem",
    "print any",
    "output any",
    "any of them",
    "any correct",
    "any valid",
    "any suitable",
    "if there are several",
    "if there are multiple",
    "if there are many",
    "you can print",
    "you may print",
    "any solution",
    "multiple solutions",
)
BAD_TAGS = {
    "interactive",
    "*special",
    "schedules",
    "games",
    "probabilities",
    "ternary search",
    "geometry",
}

HEADER_INPUT = re.compile(r"(?m)^[ \t]*Input[ \t]*$")
HEADER_OUTPUT = re.compile(r"(?m)^[ \t]*Output[ \t]*$")
HEADER_CUT = re.compile(r"(?m)^[ \t]*(?:Examples?|Notes?|Scoring)[ \t]*$")
PREFIX = re.compile(r"^\d+_[A-Za-z]+\d*\.\s*")


def fetch(split, offset, length=100):
    qs = urllib.parse.urlencode(
        {
            "dataset": DATASET,
            "config": "default",
            "split": split,
            "offset": offset,
            "length": length,
        }
    )
    req = urllib.request.Request(
        API + "?" + qs, headers={"User-Agent": "voj-importer/1.0"}
    )
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=90) as r:
                return json.loads(r.read().decode("utf-8"))
        except Exception as e:  # noqa: BLE001 - best effort with retries
            sys.stderr.write("retry %s@%s: %s\n" % (split, offset, e))
            time.sleep(2 + attempt * 2)
    return {"rows": []}


def clean(s):
    return (s or "").replace("\r", "").replace("<image>", "(figure omitted)")


def parse_statement(desc):
    """Return (description, input_format, output_format) or None if not parseable."""
    m_in = HEADER_INPUT.search(desc)
    if not m_in:
        return None
    m_out = HEADER_OUTPUT.search(desc, m_in.end())
    if not m_out:
        return None
    m_cut = HEADER_CUT.search(desc, m_out.end())
    end = m_cut.start() if m_cut else len(desc)
    statement = desc[: m_in.start()].strip()
    in_fmt = desc[m_in.end() : m_out.start()].strip()
    out_fmt = desc[m_out.end() : end].strip()
    if not statement or not in_fmt or not out_fmt:
        return None
    return statement, in_fmt, out_fmt


def gather_tests(row):
    """checkpoint 0 = first public test; fill up to MAX_TESTS with the smallest others."""

    def pairs(key):
        t = row.get(key) or {}
        ins, outs = t.get("input") or [], t.get("output") or []
        return [
            (clean(ins[i]), clean(outs[i])) for i in range(min(len(ins), len(outs)))
        ]

    pub = pairs("public_tests")
    if not pub or not pub[0][0].strip() or not pub[0][1].strip():
        return None
    chosen = [pub[0]]
    pool = [
        p
        for p in (pub[1:] + pairs("private_tests") + pairs("generated_tests"))
        if p[0].strip() and p[1].strip() and len(p[0]) + len(p[1]) <= TEST_SIZE_CAP
    ]
    pool.sort(key=lambda p: len(p[0]) + len(p[1]))
    for p in pool:
        if len(chosen) >= MAX_TESTS:
            break
        chosen.append(p)
    return chosen


def collect():
    seen, cands = set(), []
    plan = [("test", 165), ("valid", 117), ("train", 2000)]
    for split, total in plan:
        off = 0
        while off < total:
            rows = fetch(split, off, 100).get("rows", [])
            if not rows:
                break
            for item in rows:
                r = item.get("row", {})
                name = (r.get("name") or "").strip()
                desc = clean(r.get("description"))
                rating = int(r.get("cf_rating") or 0)
                tags = [t.lower() for t in (r.get("cf_tags") or [])]
                if not name or name in seen or rating < 800 or rating > 2600:
                    continue
                if len(desc) < 200 or len(desc) > 8000:
                    continue
                low = desc.lower()
                if any(b in tags for b in BAD_TAGS) or any(s in low for s in SPECIAL):
                    continue
                parsed = parse_statement(desc)
                if not parsed:
                    continue
                tests = gather_tests(r)
                if not tests:
                    continue
                seen.add(name)
                tl = r.get("time_limit") or {}
                secs = tl.get("seconds", 1) if isinstance(tl, dict) else 1
                nanos = tl.get("nanos", 0) if isinstance(tl, dict) else 0
                ms = int(secs) * 1000 + int(nanos / 1e6) or 1000
                mem_kb = max(
                    65536, int(r.get("memory_limit_bytes") or 268435456) // 1024
                )
                cands.append(
                    {
                        "name": PREFIX.sub("", name)[:120],
                        "rating": rating,
                        "tags": tags,
                        "ms": ms,
                        "mem_kb": mem_kb,
                        "source": SOURCE_NAME.get(
                            r.get("source", 0), "competitive programming archives"
                        ),
                        "statement": parsed[0],
                        "in_fmt": parsed[1],
                        "out_fmt": parsed[2],
                        "tests": tests,
                    }
                )
            off += 100
            time.sleep(0.4)
        have = {
            t: sum(1 for c in cands if lo <= c["rating"] <= hi) for t, lo, hi in TIERS
        }
        if all(have[t] >= PER_TIER + 4 for t, _, _ in TIERS):
            break
    return cands


def pick(cands):
    out = []
    for tier, lo, hi in TIERS:
        b = sorted(
            [c for c in cands if lo <= c["rating"] <= hi],
            key=lambda c: (c["rating"], c["name"]),
        )
        if len(b) > PER_TIER:
            idx = sorted(
                set(round(i * (len(b) - 1) / (PER_TIER - 1)) for i in range(PER_TIER))
            )
            b = [b[i] for i in idx]
        out.extend((c, tier) for c in b[:PER_TIER])
    return out


def slugify(t):
    return (re.sub(r"[^a-z0-9]+", "-", t.lower()).strip("-")[:32]) or "misc"


def sql_str(s):
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def html_safe(s):
    # Statements render via th:utext (unescaped) + client-side markdown, so a raw
    # "<" in the prose (e.g. "p_i < p_{i+1}") is parsed as an HTML tag and swallows
    # the rest of the page. Escape the markup-significant characters for display.
    # (Checkpoint data is left raw - it is judged byte-for-byte, never rendered.)
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def split_scores(n):
    sc = [100 // n] * n
    sc[0] += 100 - sum(sc)
    return sc


def build_tag_vocab(selected):
    tag_id, nxt = {}, TAG_BASE_ID
    for c, _ in selected:
        for t in c["tags"]:
            if t in EXISTING_TAGS:
                tag_id[t] = EXISTING_TAGS[t]
            elif t not in tag_id:
                tag_id[t] = nxt
                nxt += 1
    new_tags = sorted([(i, t) for t, i in tag_id.items() if i >= TAG_BASE_ID])
    return tag_id, new_tags


def build_fragment(selected, tag_id):
    out = [
        "-- >>> CODECONTESTS BEGIN (generated by scripts/import_codecontests.py; do not edit)",
        "-- Problems from DeepMind CodeContests (https://huggingface.co/datasets/deepmind/code_contests),",
        "-- licensed CC BY 4.0. Statements (c) their original authors; attributed per problem.",
    ]
    cols = (
        "`problem_id`, `problem_is_public`, `problem_name`, `problem_time_limit`, `problem_memory_limit`, "
        "`problem_description`, `problem_input_format`, `problem_output_format`, `problem_sample_input`, "
        "`problem_sample_output`, `problem_hint`"
    )
    out.append("INSERT INTO `voj_problems` (%s) VALUES" % cols)
    prows, meta, pid = [], [], PROBLEM_BASE_ID
    for c, tier in selected:
        desc = (
            html_safe(c["statement"])
            + "\n\n---\n*Source: %s problem, via DeepMind CodeContests (CC BY 4.0).*"
            % c["source"]
        )
        si, so = c["tests"][0]
        prows.append(
            "(%d, 1, %s, %d, %d, %s, %s, %s, %s, %s, NULL)"
            % (
                pid,
                sql_str(c["name"]),
                c["ms"],
                c["mem_kb"],
                sql_str(desc),
                sql_str(html_safe(c["in_fmt"])),
                sql_str(html_safe(c["out_fmt"])),
                sql_str(html_safe(si)),
                sql_str(html_safe(so)),
            )
        )
        meta.append((pid, c, tier))
        pid += 1
    out.append(",\n".join(prows) + ";")

    for tier, _, _ in TIERS:
        ids = [str(p) for p, c, t in meta if t == tier]
        if ids:
            out.append(
                "UPDATE `voj_problems` SET `problem_difficulty_id` = %d WHERE `problem_id` IN (%s);"
                % (tier, ", ".join(ids))
            )

    out.append(
        "INSERT INTO `voj_problem_checkpoints` (`problem_id`, `checkpoint_id`, "
        "`checkpoint_exactly_match`, `checkpoint_score`, `checkpoint_input`, `checkpoint_output`) VALUES"
    )
    crows = []
    for p, c, t in meta:
        scores = split_scores(len(c["tests"]))
        for i, (ti, to) in enumerate(c["tests"]):
            crows.append(
                "(%d, %d, 0, %d, %s, %s)" % (p, i, scores[i], sql_str(ti), sql_str(to))
            )
    out.append(",\n".join(crows) + ";")

    out.append(
        "INSERT INTO `voj_problem_category_relationships` (`problem_id`, `problem_category_id`) VALUES"
    )
    catrows = []
    for p, c, t in meta:
        catrows.append("(%d, 1)" % p)
        if any(x in ("dp", "dynamic programming") for x in c["tags"]):
            catrows.append("(%d, 2)" % p)
    out.append(",\n".join(catrows) + ";")

    trows = []
    for p, c, t in meta:
        for x in c["tags"]:
            if x in tag_id:
                trows.append("(%d, %d)" % (p, tag_id[x]))
    if trows:
        out.append(
            "INSERT INTO `voj_problem_tag_relationships` (`problem_id`, `problem_tag_id`) VALUES"
        )
        out.append(",\n".join(trows) + ";")
    out.append("-- <<< CODECONTESTS END")
    return meta, "\n".join(out) + "\n", len(crows)


def splice_demo(fragment):
    with io.open(DEMO, "r", encoding="utf-8") as f:
        demo = f.read()
    demo = re.sub(
        r"-- >>> CODECONTESTS BEGIN.*?-- <<< CODECONTESTS END\n", "", demo, flags=re.S
    )
    marker = "\nSET FOREIGN_KEY_CHECKS = 1;"
    idx = demo.rfind(marker)
    demo = demo[:idx] + "\n" + fragment + demo[idx:]
    with io.open(DEMO, "w", encoding="utf-8", newline="\n") as f:
        f.write(demo)


def rebuild_seed_tags(new_tags):
    with io.open(SEED, "r", encoding="utf-8") as f:
        seed = f.read()
    lines = [
        "(1, 'greedy', 'Greedy')",
        "(2, 'dynamic-programming', 'Dynamic Programming')",
    ]
    for tid, t in new_tags:
        lines.append("(%d, %s, %s)" % (tid, sql_str(slugify(t)), sql_str(t.title())))
    insert = (
        "INSERT INTO `voj_problem_tags` (`problem_tag_id`, `problem_tag_slug`, `problem_tag_name`) VALUES\n"
        + ",\n".join(lines)
        + ";"
    )
    seed = re.sub(
        r"INSERT INTO `voj_problem_tags`.*?;", insert, seed, count=1, flags=re.S
    )
    with io.open(SEED, "w", encoding="utf-8", newline="\n") as f:
        f.write(seed)


def main():
    cands = collect()
    selected = pick(cands)
    tag_id, new_tags = build_tag_vocab(selected)
    meta, fragment, n_ckpt = build_fragment(selected, tag_id)
    splice_demo(fragment)
    rebuild_seed_tags(new_tags)

    by_tier = {t: 0 for t, _, _ in TIERS}
    for _, c, t in meta:
        by_tier[t] += 1
    print("candidates:", len(cands))
    print("selected:", {("easy", "medium", "hard")[t - 1]: by_tier[t] for t in by_tier})
    print("problem ids: %d..%d" % (PROBLEM_BASE_ID, PROBLEM_BASE_ID + len(meta) - 1))
    print("checkpoints:", n_ckpt, " (avg %.1f/problem)" % (n_ckpt / max(1, len(meta))))
    print("new tags:", len(new_tags), " -> total voj_problem_tags =", 2 + len(new_tags))
    print("NEW_TAG_COUNT=%d" % (2 + len(new_tags)))
    for p, c, t in meta:
        print(
            "  %d  d%d  r%-4d  %2d cp  %s"
            % (p, t, c["rating"], len(c["tests"]), c["name"])
        )


if __name__ == "__main__":
    main()
