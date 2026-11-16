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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.mapper.OffensiveWordMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;

/**
 * The offensive word filtering class.
 *
 * <p>The offensive words are loaded once from the database into an in-memory DFA; the actual
 * filtering never touches the database. The dictionary can be refreshed at runtime - for example
 * after an administrator re-imports the external word lists - through {@link #reload()}.
 *
 * <p>Matching is case-insensitive: both the dictionary and the scanned text are case-folded
 * character by character, while the masking is applied to the original text in place.
 *
 * @author Zhou Yihao
 */
@Component
public class OffensiveWordFilter {
  /**
   * The constructor of the OffensiveWordFilter class.
   *
   * @param offensiveWordMapper - the autowired OffensiveWordMapper object, used to get the offensive
   *     words from the database
   */
  @Autowired
  private OffensiveWordFilter(OffensiveWordMapper offensiveWordMapper, OptionMapper optionMapper) {
    this.offensiveWordMapper = offensiveWordMapper;
    this.optionMapper = optionMapper;
  }

  /**
   * Reloads the offensive word dictionary from the database and rebuilds the DFA. The freshly built
   * model is swapped in atomically, so concurrent {@link #censor(String)} calls keep using the
   * previous model until the new one is ready and never observe a half-built state.
   */
  public void reload() {
    this.offensiveWordMap = loadOffensiveWordMap();
  }

  /**
   * Returns the DFA model, building it from the database on first use. The dictionary is loaded
   * lazily rather than in the constructor so that the filter never touches the database while the
   * Spring context is still wiring up (the schema may not be provisioned yet at that point).
   *
   * @return the DFA model
   */
  private Map<?, ?> getOffensiveWordMap() {
    HashMap<?, ?> map = offensiveWordMap;
    if (map == null) {
      synchronized (this) {
        map = offensiveWordMap;
        if (map == null) {
          map = loadOffensiveWordMap();
          offensiveWordMap = map;
        }
      }
    }
    return map;
  }

  /**
   * Loads the offensive words from the database and builds a fresh DFA model.
   *
   * @return the freshly built DFA model
   */
  private HashMap<?, ?> loadOffensiveWordMap() {
    return buildOffensiveWordMap(new HashSet<>(offensiveWordMapper.getOffensiveWords()));
  }

  /**
   * Censors offensive words in user-generated text that is about to be displayed, honouring the
   * {@code autoCensorOffensiveWords} system option. This is the entry point that callers should use
   * for every displayable, user-supplied field (display names, bios, discussion titles and replies,
   * and so on) - source code is the deliberate exception, as masking it would corrupt the program.
   *
   * <p>When the option is disabled the original text is returned untouched, so censoring can be
   * toggled at runtime without rewriting stored content; the censoring happens at display time
   * rather than at write time so refreshed dictionaries ({@link #reload()}) and option changes take
   * effect retroactively.
   *
   * @param text - the string to censor, may be {@code null}
   * @return the censored string, or the original text when censoring is disabled or the text is
   *     {@code null}/empty
   */
  public String censor(String text) {
    return censor(text, "*");
  }

  /**
   * Censors offensive words in user-generated text that is rendered as Markdown (discussion content,
   * bios, ...). Identical to {@link #censor(String)} except each masked character becomes an escaped
   * asterisk ({@code \*}) so the mask still reads as {@code ***} but the Markdown renderer treats it
   * as literal text - an unescaped {@code ***} alone on a line would otherwise become a horizontal
   * rule, and {@code **}/{@code *} would turn into bold/italic.
   *
   * @param text - the Markdown string to censor, may be {@code null}
   * @return the censored string, or the original text when censoring is disabled or the text is
   *     {@code null}/empty
   */
  public String censorMarkdown(String text) {
    return censor(text, "\\*");
  }

  /**
   * Shared implementation of {@link #censor(String)} and {@link #censorMarkdown(String)}: masks
   * offensive words with the given replacement when the {@code autoCensorOffensiveWords} option is
   * enabled, otherwise returns the text untouched.
   *
   * @param text - the string to censor, may be {@code null}
   * @param replaceChar - the string each offensive character is replaced with
   * @return the censored string, or the original text when censoring is disabled or the text is
   *     {@code null}/empty
   */
  private String censor(String text, String replaceChar) {
    if (text == null || text.isEmpty() || !isAutoCensorEnabled()) {
      return text;
    }
    return mask(text, replaceChar);
  }

  /**
   * Reads the {@code autoCensorOffensiveWords} system option, treating a missing option as enabled
   * so that censoring is on by default.
   *
   * @return whether automatic censoring of offensive words is enabled
   */
  private boolean isAutoCensorEnabled() {
    Option option = optionMapper.getOption(OPTION_AUTO_CENSOR);
    if (option == null || option.getOptionValue() == null) {
      return true;
    }
    return "1".equals(option.getOptionValue().trim());
  }

  /**
   * Masks every offensive word of a text, matching the longest dictionary entry at each position.
   *
   * @param text - the string to filter
   * @param replaceChar - the string each offensive character is replaced with
   * @return the filtered string
   */
  private String mask(String text, String replaceChar) {
    // Get the positions of all offensive words in the text
    List<Position> offensiveWordsPosition = getOffensiveWordsPosition(text);
    // Replace all offensive words in the text with replaceChar
    StringBuilder resultStringBuilder = new StringBuilder(text);

    Iterator<Position> iterator = offensiveWordsPosition.iterator();
    while (iterator.hasNext()) {
      Position now = iterator.next();
      resultStringBuilder.replace(
          now.start, now.start + now.length, getReplaceChars(replaceChar, now.length));
    }
    return resultStringBuilder.toString();
  }

  /**
   * Gets the positions of the offensive words.
   *
   * @param text - the string to filter
   * @return the positions of the offensive words
   */
  private List<Position> getOffensiveWordsPosition(String text) {
    List<Position> offensiveWordsPosition = new ArrayList<>();
    // Traverse the string to be filtered, and check whether the prefix of the substring of text
    // starting at i is an offensive word
    for (int i = 0; i < text.length(); ++i) {
      int length = checkOffensiveWord(text, i);
      if (length > 0 && hasWordBoundary(text, i, length)) {
        Position position = new Position(i, length);
        offensiveWordsPosition.add(position);
        // Skip the already-matched offensive word; subtract one because the for loop increments
        i = i + length - 1;
      }
    }
    return offensiveWordsPosition;
  }

  /**
   * Checks that an ASCII-alphanumeric match is a whole word rather than a fragment of a longer one.
   * The DFA matches character by character, which is what Chinese (which has no word delimiters)
   * needs, but for ASCII text it would otherwise mask {@code b} inside {@code dashboard} or
   * {@code ass} inside {@code class}. So when the match begins or ends with an ASCII letter/digit, we
   * require the adjacent character on that side not to be one too; CJK matches carry no such
   * requirement and keep matching anywhere, exactly as before.
   *
   * @param text - the text being filtered
   * @param start - the start position of the match
   * @param length - the length of the match
   * @return whether the match sits on word boundaries and should therefore be masked
   */
  private static boolean hasWordBoundary(String text, int start, int length) {
    int end = start + length;
    if (isAsciiWordChar(text.charAt(start)) && start > 0 && isAsciiWordChar(text.charAt(start - 1))) {
      return false;
    }
    if (isAsciiWordChar(text.charAt(end - 1)) && end < text.length() && isAsciiWordChar(text.charAt(end))) {
      return false;
    }
    return true;
  }

  /**
   * Reports whether a character is an ASCII letter or digit, i.e. the kind of character that forms
   * English words and therefore demands word-boundary matching. CJK characters are deliberately
   * excluded so that Chinese offensive words keep matching without delimiters.
   *
   * @param c - the character to test
   * @return whether the character is an ASCII letter or digit
   */
  private static boolean isAsciiWordChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }

  /**
   * Gets the length of the longest offensive word the string starting at beginIndex begins with.
   *
   * @param text - the text to filter
   * @param beginIndex - the start position of this check
   * @return the length of the offensive word if one exists, or 0 if none exists
   */
  @SuppressWarnings("rawtypes")
  private int checkOffensiveWord(String text, int beginIndex) {
    /*
     * matchedLength is the length of the longest offensive word matched so far.
     * matchingLength is the length of the offensive word currently being matched.
     * When a terminal symbol is matched, matchingLength is assigned to matchedLength, and matching
     * continues in case a longer offensive word starts with it.
     */
    int matchedLength = 0;
    int matchingLength = 0;

    char nowWord = 0;
    Map nowMap = getOffensiveWordMap();
    for (int i = beginIndex; i < text.length(); ++i) {
      // The dictionary is stored case-folded (see buildOffensiveWordMap), so fold each input
      // character the same way. Folding one char at a time keeps the match positions aligned with
      // the original text, which is masked in place and otherwise left untouched.
      nowWord = foldCase(text.charAt(i));
      // The follow set of the current character
      nowMap = (Map) nowMap.get(nowWord);
      if (nowMap != null) {
        ++matchingLength;
        if ("1".equals(nowMap.get(IS_END))) { // Matched a terminal symbol; update the current max matched length
          matchedLength = matchingLength;
        }
      } else { // If the follow set of the current character is empty, the match ends
        break;
      }
    }
    return matchedLength;
  }

  /**
   * Folds a character for case-insensitive matching, so that {@code FUCK} or {@code Fuck} matches a
   * lower-case dictionary entry. The folding is applied per character (rather than with {@link
   * String#toLowerCase()}, which may change the length of the string for some locales and code
   * points) so that a match in the folded text always has the same position and length in the
   * original text.
   *
   * @param c - the character to fold
   * @return the case-folded character
   */
  static char foldCase(char c) {
    return Character.toLowerCase(Character.toUpperCase(c));
  }

  /**
   * Gets the string used to replace an offensive word. All the characters in the offensive word are
   * replaced with replaceChar, generated based on the length and character, e.g. "****".
   *
   * @param replaceChar the character used to replace the offensive word
   * @param length the length of the offensive word
   * @return the string used to replace the offensive word
   */
  private static String getReplaceChars(String replaceChar, int length) {
    StringBuilder resultChars = new StringBuilder(length * replaceChar.length());
    for (int i = 0; i < length; ++i) {
      resultChars.append(replaceChar);
    }
    return resultChars.toString();
  }

  /**
   * Reads the offensive word library, puts the offensive words into a HashSet, and builds a DFA
   * algorithm model. For example, for the words "ab", "abc" and "ade", the model is: a = { isEnd = 0
   * b = { isEnd = 1 c = { isEnd = 1 } } d = { isEnd = 0 e = { isEnd = 1 } } }.
   *
   * @param offensiveWordSet - the Set of offensive words
   * @return the root HashMap of the freshly built DFA model
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static HashMap buildOffensiveWordMap(Set<String> offensiveWordSet) {
    // Initialize the offensive word HashMap, with a size equal to the Set.
    HashMap offensiveWordMap = new HashMap((int) (offensiveWordSet.size() * 1.5));

    // After reading a key from the nowMap state and reaching a new state, create newWordMap
    String key = null;
    Map nowMap = null;
    Map newWordMap = null;
    // Iterate over the OffensiveWordSet
    Iterator<String> iterator = offensiveWordSet.iterator();
    while (iterator.hasNext()) {
      key = iterator.next();
      // Start traversal from the top-level HashMap
      nowMap = offensiveWordMap;

      // Iterate over each character in the current offensive word
      for (int i = 0; i < key.length(); ++i) {
        char keyChar = foldCase(key.charAt(i));
        Object wordMap = nowMap.get(keyChar);

        if (wordMap != null) {
          // The current character of the current offensive word already exists; go to the next state
          nowMap = (Map) wordMap;
        } else {
          // The current character of the current offensive word does not exist; create a new Map (state)
          newWordMap = new HashMap();
          // By default, the current character of the current offensive word is not a terminal symbol
          newWordMap.put(IS_END, "0");
          // Add the newly created Map (state) to the current Map (state)
          nowMap.put(keyChar, newWordMap);
          // Go to the next state after reading the current character
          nowMap = newWordMap;
        }
        if (i == key.length() - 1) {
          nowMap.put(IS_END, "1");
        }
      }
    }
    return offensiveWordMap;
  }

  /**
   * The HashMap (DFA model) that stores the offensive words. Marked {@code volatile} because {@link
   * #reload()} replaces it wholesale while request threads read it through {@link #censor(String)}.
   */
  private volatile HashMap<?, ?> offensiveWordMap;

  /** The flag in the HashMap indicating whether an offensive word terminates. */
  private static final String IS_END = "isEnd";

  /** The name of the system option that toggles automatic censoring of offensive words. */
  private static final String OPTION_AUTO_CENSOR = "autoCensorOffensiveWords";

  /** The autowired OffensiveWordMapper object, used to load the offensive words from the database. */
  private final OffensiveWordMapper offensiveWordMapper;

  /** The autowired OptionMapper object, used to read the auto-censor system option. */
  private final OptionMapper optionMapper;
}

/**
 * Stores the position of an offensive word (start position and length).
 *
 * @author Zhou YiHao
 */
class Position {
  /**
   * The constructor of Position.
   *
   * @param start - the start position of the offensive word
   * @param length - the length of the offensive word
   */
  public Position(int start, int length) {
    this.start = start;
    this.length = length;
  }

  int start;

  int length;
}
