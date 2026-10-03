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
package org.verwandlung.voj.web.controller;

import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.HtmlUtils;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.ProblemService;

/**
 * Serves the files search engines look for: robots.txt and sitemap.xml. Both use the public base
 * URL (url.base) so the links stay correct behind a reverse proxy. Crawlers only read robots.txt at
 * the root of the host, so it is only effective when the application is served at the root.
 *
 * @author Haozhe Xie
 */
@Controller
public class SeoController {
  /**
   * Serves robots.txt: keeps crawlers out of the administration pages and points them to the
   * sitemap. The other private pages (sign-in, submissions, ...) carry a noindex meta tag instead,
   * which crawlers can only see when they are allowed to fetch the page.
   *
   * @return the content of robots.txt
   */
  @RequestMapping(value = "/robots.txt", method = RequestMethod.GET)
  public ResponseEntity<String> robotsTxt() {
    String baseUrl = getBaseUrl();
    String basePath = URI.create(baseUrl).getPath();
    String robotsTxt =
        "User-agent: *\n"
            + "Disallow: "
            + basePath
            + "/administration\n"
            + "\n"
            + "Sitemap: "
            + baseUrl
            + "/sitemap.xml\n";
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(robotsTxt);
  }

  /**
   * Serves sitemap.xml: the public landing pages, the public problems, the published contests and
   * the visible discussion threads.
   *
   * @return the content of sitemap.xml
   */
  @RequestMapping(value = "/sitemap.xml", method = RequestMethod.GET)
  public ResponseEntity<String> sitemapXml() {
    String baseUrl = getBaseUrl();
    StringBuilder sitemap = new StringBuilder();
    sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    for (String path : STATIC_PAGES) {
      appendUrl(sitemap, baseUrl + path);
    }
    for (long problemId : problemService.getIdsOfPublicProblems(MAX_URLS_PER_TYPE)) {
      appendUrl(sitemap, baseUrl + "/p/" + problemId);
    }
    for (Contest contest : contestService.getContests(null, true, 0, MAX_URLS_PER_TYPE)) {
      appendUrl(sitemap, baseUrl + "/contest/" + contest.getContestId());
    }
    List<DiscussionThread> threads =
        discussionService.getDiscussionThreadsOfTopic(null, 0, MAX_URLS_PER_TYPE);
    for (DiscussionThread thread : threads) {
      appendUrl(sitemap, baseUrl + "/discussion/" + thread.getDiscussionThreadId());
    }
    sitemap.append("</urlset>\n");
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(sitemap.toString());
  }

  /**
   * Appends a URL entry to the sitemap.
   *
   * @param sitemap - the sitemap being built
   * @param url - the absolute URL of the page
   */
  private void appendUrl(StringBuilder sitemap, String url) {
    sitemap.append("  <url><loc>").append(HtmlUtils.htmlEscape(url)).append("</loc></url>\n");
  }

  /**
   * Gets the public base URL of the application without the trailing slash.
   *
   * @return the public base URL (e.g. https://oj.example.edu)
   */
  private String getBaseUrl() {
    String baseUrl = applicationProperties.getProperty("url.base");
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /** The public landing pages, relative to the base URL. */
  private static final String[] STATIC_PAGES = {
    "/", "/p", "/contest", "/discussion", "/judgers", "/help", "/about", "/terms", "/privacy"
  };

  /**
   * The maximum number of URLs listed for each type of content, which keeps the sitemap well under
   * the 50,000-URL limit of the sitemap protocol.
   */
  private static final int MAX_URLS_PER_TYPE = 10000;

  /** The autowired ProblemService object. */
  @Autowired private ProblemService problemService;

  /** The autowired ContestService object. */
  @Autowired private ContestService contestService;

  /** The autowired DiscussionService object. */
  @Autowired private DiscussionService discussionService;

  /** The application properties, exposing url.base. */
  @Autowired
  @Qualifier("propertyConfigurer")
  private Properties applicationProperties;
}
