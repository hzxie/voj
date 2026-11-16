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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.interceptor.AdministrationSection;
import org.verwandlung.voj.web.model.BulletinBoardMessage;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.BulletinBoardService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.Pagination;

/**
 * The bulletin board administration.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("bulletins")
public class BulletinAdministrationController {
  /**
   * Loads the bulletin board message list page.
   *
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the bulletin board messages
   */
  @RequestMapping(value = "/all-bulletins", method = RequestMethod.GET)
  public ModelAndView allBulletinsView(
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_BULLETINS_PER_PAGE = optionService.getIntOption("bulletinsPerPage", 50);
    long totalBulletins = bulletinBoardService.getNumberOfBulletinBoardMessages();
    long offset = Pagination.getOffset(pageNumber, NUMBER_OF_BULLETINS_PER_PAGE);
    List<BulletinBoardMessage> messages =
        bulletinBoardService.getBulletinBoardMessages(offset, NUMBER_OF_BULLETINS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-bulletins");
    view.addObject("messages", messages);
    view.addObject("currentPage", pageNumber);
    view.addObject(
        "totalPages", (long) Math.ceil(totalBulletins * 1.0 / NUMBER_OF_BULLETINS_PER_PAGE));
    return view;
  }

  /**
   * Loads the create bulletin board message page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object for creating a bulletin board message
   */
  @RequestMapping(value = "/new-bulletin", method = RequestMethod.GET)
  public ModelAndView newBulletinView(HttpServletRequest request, HttpServletResponse response) {
    return new ModelAndView("pages/administration/edit-bulletin");
  }

  /**
   * Loads the edit bulletin board message page.
   *
   * @param messageId - the unique identifier of the bulletin board message
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the bulletin board message to edit
   */
  @RequestMapping(value = "/edit-bulletin/{messageId}", method = RequestMethod.GET)
  public ModelAndView editBulletinView(
      @PathVariable(value = "messageId") long messageId,
      HttpServletRequest request,
      HttpServletResponse response) {
    BulletinBoardMessage message = bulletinBoardService.getBulletinBoardMessage(messageId);
    if (message == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/administration/edit-bulletin");
    view.addObject("message", message);
    view.addObject("sectionLabel", message.getMessageTitle());
    return view;
  }

  /**
   * Creates a bulletin board message.
   *
   * @param messageTitle - the title of the message
   * @param messageBody - the content of the message
   * @param request - the HttpServletRequest object
   * @return a Map containing the creation result
   */
  @RequestMapping(value = "/createBulletin.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> createBulletinAction(
      @RequestParam(value = "messageTitle") String messageTitle,
      @RequestParam(value = "messageBody") String messageBody,
      @RequestParam(value = "isPinned", defaultValue = "false") boolean isPinned,
      @RequestParam(value = "status", defaultValue = "PUBLISHED") String status,
      HttpServletRequest request) {
    User currentUser = HttpSessionParser.getCurrentUser();
    long authorId = currentUser != null ? currentUser.getUid() : 0;
    return bulletinBoardService.createBulletinBoardMessage(
        messageTitle, messageBody, authorId, isPinned, status);
  }

  /**
   * Edits a bulletin board message.
   *
   * @param messageId - the unique identifier of the message
   * @param messageTitle - the new title of the message
   * @param messageBody - the new content of the message
   * @param request - the HttpServletRequest object
   * @return a Map containing the edit result
   */
  @RequestMapping(value = "/editBulletin.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editBulletinAction(
      @RequestParam(value = "messageId") long messageId,
      @RequestParam(value = "messageTitle") String messageTitle,
      @RequestParam(value = "messageBody") String messageBody,
      @RequestParam(value = "isPinned", defaultValue = "false") boolean isPinned,
      @RequestParam(value = "status", defaultValue = "PUBLISHED") String status,
      HttpServletRequest request) {
    return bulletinBoardService.editBulletinBoardMessage(
        messageId, messageTitle, messageBody, isPinned, status);
  }

  /**
   * Deletes the selected bulletin board messages.
   *
   * @param bulletins - the set of message IDs, separated by commas
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteBulletins.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteBulletinsAction(
      @RequestParam(value = "bulletins") String bulletins, HttpServletRequest request) {
    List<Long> bulletinList = JsonUtils.toList(bulletins, Long.class);
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put(
        "isSuccessful",
        BulkActions.run(
            "delete bulletins " + bulletinList,
            () -> bulletinBoardService.deleteBulletinBoardMessages(bulletinList)));
    return result;
  }

  /** The autowired BulletinBoardService object. Used for the bulletin board admin. */
  @Autowired private BulletinBoardService bulletinBoardService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;
}
