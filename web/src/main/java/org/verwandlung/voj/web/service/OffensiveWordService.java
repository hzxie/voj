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
package org.verwandlung.voj.web.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.OffensiveWordMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;

/**
 * The offensive word service, responsible for the persistence side of the offensive word
 * dictionary: the configured import sources, the dictionary itself, and the import bookkeeping.
 *
 * <p>The network-facing import orchestration lives in {@link OffensiveWordImportService}; this
 * service only touches the database so that the whole dictionary can be replaced atomically.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class OffensiveWordService {
  /**
   * Gets the configured offensive word import sources, one URL per line.
   *
   * @return the offensive word import sources
   */
  public String getOffensiveWordSources() {
    Option option = optionMapper.getOption(OFFENSIVE_WORD_SOURCES_OPTION_KEY);
    return option == null ? "" : option.getOptionValue();
  }

  /**
   * Updates the configured offensive word import sources.
   *
   * @param sources - the offensive word import sources, one URL per line
   */
  public void updateOffensiveWordSources(String sources) {
    updateOption(OFFENSIVE_WORD_SOURCES_OPTION_KEY, sources == null ? "" : sources);
  }

  /**
   * Gets the total number of offensive words currently in the dictionary.
   *
   * @return the total number of offensive words
   */
  public long getNumberOfOffensiveWords() {
    return offensiveWordMapper.getNumberOfOffensiveWords();
  }

  /**
   * Gets the time at which the offensive word dictionary was last imported.
   *
   * @return the last import time, or an empty string if it has never been imported
   */
  public String getOffensiveWordsImportedAt() {
    Option option = optionMapper.getOption(OFFENSIVE_WORDS_IMPORTED_AT_OPTION_KEY);
    return option == null ? "" : option.getOptionValue();
  }

  /**
   * Replaces the whole offensive word dictionary with the given words in a single transaction. If
   * any insert fails the deletion is rolled back, so a failed import never wipes the dictionary.
   * Rebuilding the in-memory DFA is left to the caller, which must do it after this method returns
   * (i.e. after the transaction commits).
   *
   * @param offensiveWords - the offensive words to store, de-duplicated case-sensitively (the
   *     database collapses any further duplicates that are only collation-equal)
   * @return the number of offensive words actually stored
   */
  public int replaceAllOffensiveWords(Collection<String> offensiveWords) {
    offensiveWordMapper.deleteAllOffensiveWords();

    List<String> words = new ArrayList<>(offensiveWords);
    for (int i = 0; i < words.size(); i += BATCH_INSERT_SIZE) {
      offensiveWordMapper.batchCreateOffensiveWords(
          words.subList(i, Math.min(i + BATCH_INSERT_SIZE, words.size())));
    }
    updateOption(
        OFFENSIVE_WORDS_IMPORTED_AT_OPTION_KEY, LocalDateTime.now().format(IMPORTED_AT_FORMATTER));
    return (int) offensiveWordMapper.getNumberOfOffensiveWords();
  }

  /**
   * Updates the value of an existing system option.
   *
   * @param optionName - the name of the option
   * @param optionValue - the new value of the option
   */
  private void updateOption(String optionName, String optionValue) {
    Option option = optionMapper.getOption(optionName);
    option.setOptionValue(optionValue);
    optionMapper.updateOption(option);
  }

  /** The maximum number of offensive words inserted per SQL statement. */
  private static final int BATCH_INSERT_SIZE = 500;

  /** The system setting key holding the offensive word import sources (one URL per line). */
  public static final String OFFENSIVE_WORD_SOURCES_OPTION_KEY = "offensiveWordSources";

  /** The system setting key holding the time of the last offensive word import. */
  public static final String OFFENSIVE_WORDS_IMPORTED_AT_OPTION_KEY = "offensiveWordsImportedAt";

  /** The formatter used to render the last import time. */
  private static final DateTimeFormatter IMPORTED_AT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** The autowired OffensiveWordMapper object. */
  @Autowired private OffensiveWordMapper offensiveWordMapper;

  /** The autowired OptionMapper object. */
  @Autowired private OptionMapper optionMapper;
}
