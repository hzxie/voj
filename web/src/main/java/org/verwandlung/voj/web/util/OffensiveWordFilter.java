/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.web.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;

/**
 * Filters offensive words out of user supplied text.
 *
 * <p>The offensive words are organized into a trie (prefix tree) built once at construction time.
 * Filtering performs a single left-to-right scan of the text, greedily matching the longest
 * offensive word starting at each position, which runs in {@code O(n * L)} time where {@code n} is
 * the text length and {@code L} the length of the longest offensive word.
 *
 * @author Zhou Yihao
 */
@Component
public class OffensiveWordFilter {
  /**
   * The constructor of the OffensiveWordFilter class.
   *
   * @param optionMapper - the autowired optionMapper object, used to get the offensive word list
   *     from the database
   */
  @Autowired
  private OffensiveWordFilter(OptionMapper optionMapper) {
    Option offensiveWordOption = optionMapper.getOption(OFFENSIVE_WORD_OPTION_KEY);
    if (offensiveWordOption != null) {
      List<String> offensiveWords =
          JsonUtils.toList(offensiveWordOption.getOptionValue(), String.class);
      for (String offensiveWord : offensiveWords) {
        addOffensiveWord(offensiveWord);
      }
    }
  }

  /**
   * Replaces every offensive word found in the text with asterisks.
   *
   * @param text - the string to filter
   * @return the filtered string
   */
  public String filter(String text) {
    return filter(text, '*');
  }

  /**
   * Replaces every offensive word found in the text with the given character.
   *
   * @param text - the string to filter
   * @param replaceChar - the character used to mask each offensive character
   * @return the filtered string
   */
  private String filter(String text, char replaceChar) {
    StringBuilder result = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); ) {
      int length = matchLength(text, i);
      if (length > 0) {
        for (int j = 0; j < length; ++j) {
          result.append(replaceChar);
        }
        i += length;
      } else {
        result.append(text.charAt(i));
        ++i;
      }
    }
    return result.toString();
  }

  /**
   * Returns the length of the longest offensive word that starts at {@code beginIndex}, or 0 if no
   * offensive word starts there.
   *
   * @param text - the text being filtered
   * @param beginIndex - the position to match from
   * @return the length of the matched offensive word, or 0 if there is no match
   */
  private int matchLength(String text, int beginIndex) {
    TrieNode node = root;
    int matchedLength = 0;
    for (int i = beginIndex; i < text.length(); ++i) {
      node = node.children.get(text.charAt(i));
      if (node == null) {
        break;
      }
      if (node.isEnd) {
        // Keep going to greedily prefer the longest offensive word (e.g. "ab" over "abc").
        matchedLength = i - beginIndex + 1;
      }
    }
    return matchedLength;
  }

  /**
   * Inserts a single offensive word into the trie. For the words "ab", "abc" and "ade" the resulting
   * trie is: a -&gt; (b[end] -&gt; c[end]), (d -&gt; e[end]).
   *
   * @param offensiveWord - the offensive word to insert
   */
  private void addOffensiveWord(String offensiveWord) {
    if (offensiveWord == null || offensiveWord.isEmpty()) {
      return;
    }
    TrieNode node = root;
    for (int i = 0; i < offensiveWord.length(); ++i) {
      node = node.children.computeIfAbsent(offensiveWord.charAt(i), key -> new TrieNode());
    }
    node.isEnd = true;
  }

  /** A node of the offensive word trie. */
  private static final class TrieNode {
    /** The child nodes keyed by their character. */
    private final Map<Character, TrieNode> children = new HashMap<>();

    /** Whether the path from the root to this node spells a complete offensive word. */
    private boolean isEnd;
  }

  /** The root of the offensive word trie. */
  private final TrieNode root = new TrieNode();

  /** The system setting key for offensive words. */
  public static final String OFFENSIVE_WORD_OPTION_KEY = "offensiveWords";
}
