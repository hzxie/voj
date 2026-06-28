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

/**
 * The offensive word filtering class.
 *
 * <p>The offensive words are loaded once from the database into an in-memory DFA; the actual
 * filtering never touches the database. The dictionary can be refreshed at runtime - for example
 * after an administrator re-imports the external word lists - through {@link #reload()}.
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
  private OffensiveWordFilter(OffensiveWordMapper offensiveWordMapper) {
    this.offensiveWordMapper = offensiveWordMapper;
  }

  /**
   * Reloads the offensive word dictionary from the database and rebuilds the DFA. The freshly built
   * model is swapped in atomically, so concurrent {@link #filter(String)} calls keep using the
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
   * Provides the offensive word filtering function. An overload of offensiveWordFilter(String, int,
   * String). Sets the default values of the match rule (maxMatchType) and the replacement character
   * ("*").
   *
   * @param text - the string to filter
   * @return the filtered string
   */
  public String filter(String text) {
    return filter(text, OffensiveWordFilter.MAX_MATCH_TYPE, "*");
  }

  /**
   * Provides the offensive word filtering function.
   *
   * @param text - the string to filter
   * @param matchType - the match rule, 1 for minimum match, 2 for maximum match
   * @return the filtered string
   */
  private String filter(String text, int matchType, String replaceChar) {
    // Get the positions of all offensive words in the text
    List<Position> offensiveWordsPosition = getOffensiveWordsPosition(text, matchType);
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
   * @param matchType - the match rule, 1 for minimum match, 2 for maximum match
   * @return the positions of the offensive words
   */
  private List<Position> getOffensiveWordsPosition(String text, int matchType) {
    List<Position> offensiveWordsPosition = new ArrayList<>();
    // Traverse the string to be filtered, and check whether the prefix of the substring of text
    // starting at i is an offensive word
    for (int i = 0; i < text.length(); ++i) {
      int length = checkOffensiveWord(text, i, matchType);
      if (length > 0) {
        Position position = new Position(i, length);
        offensiveWordsPosition.add(position);
        // Skip the already-matched offensive word; subtract one because the for loop increments
        i = i + length - 1;
      }
    }
    return offensiveWordsPosition;
  }

  /**
   * Checks whether the prefix of the string starting at beginIndex is an offensive word.
   *
   * @param text - the text to filter
   * @param beginIndex - the start position of this check
   * @param matchType the match mode, 1 for minimum match, 2 for maximum match
   * @return the length of the offensive word if one exists, or 0 if none exists
   */
  @SuppressWarnings("rawtypes")
  private int checkOffensiveWord(String text, int beginIndex, int matchType) {
    /*
     * matchedLength is the length of the longest offensive word matched so far.
     * matchingLength is the length of the offensive word currently being matched.
     * When a terminal symbol is matched, matchingLength is assigned to matchedLength.
     * For a maximum match, matching continues to check whether the currently matched offensive word
     * is part of a longer offensive word; if the match fails at this point, it falls back to
     * matchedLength and ends the match.
     */
    int matchedLength = 0;
    int matchingLength = 0;

    char nowWord = 0;
    Map nowMap = getOffensiveWordMap();
    for (int i = beginIndex; i < text.length(); ++i) {
      nowWord = text.charAt(i);
      // The follow set of the current character
      nowMap = (Map) nowMap.get(nowWord);
      if (nowMap != null) {
        ++matchingLength;
        if ("1".equals(nowMap.get(IS_END))) { // Matched a terminal symbol; update the current max matched length
          matchedLength = matchingLength;
          if (OffensiveWordFilter.MIN_MATCH_TYPE == matchType) {
            // For the minimum match rule, end the match as soon as a terminal symbol is matched
            break;
          }
        }
      } else { // If the follow set of the current character is empty, the match ends
        break;
      }
    }
    return matchedLength;
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
        char keyChar = key.charAt(i);
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
   * #reload()} replaces it wholesale while request threads read it through {@link #filter(String)}.
   */
  private volatile HashMap<?, ?> offensiveWordMap;

  /** The flag in the HashMap indicating whether an offensive word terminates. */
  private static final String IS_END = "isEnd";

  /**
   * minMatchType: the minimum match rule (1). Once the terminal symbol of an offensive word is
   * found, it no longer continues to look for whether it is contained in a longer offensive word.
   * maxMatchType: the maximum match rule (2). Once the terminal symbol of an offensive word is
   * found, it still continues to look for whether it is contained in a longer offensive word.
   */
  public static final int MIN_MATCH_TYPE = 1;

  public static final int MAX_MATCH_TYPE = 2;

  /** The autowired OffensiveWordMapper object, used to load the offensive words from the database. */
  private final OffensiveWordMapper offensiveWordMapper;
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
