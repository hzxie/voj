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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.interceptor.CommonModelPopulator;

/**
 * The controller for handling exceptions.
 *
 * @author Haozhe Xie
 */
@ControllerAdvice
public class ExceptionHandlingController {
  /**
   * Handles the MissingServletRequestParameterException.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ModelAndView badRequestView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/errors/404");
    commonModelPopulator.populate(view, request, response);
    return view;
  }

  /**
   * Handles the ResourceNotFoundException.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public ModelAndView notFoundView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/errors/404");
    commonModelPopulator.populate(view, request, response);
    return view;
  }

  /**
   * Handles the HttpRequestMethodNotSupportedException.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ModelAndView methodNotAllowedView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/errors/404");
    commonModelPopulator.populate(view, request, response);
    return view;
  }

  /**
   * Handles general exceptions.
   *
   * @param ex - the thrown exception object
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ModelAndView internalServerErrorView(
      Exception ex, HttpServletRequest request, HttpServletResponse response) {
    LOGGER.catching(ex);

    ModelAndView view = new ModelAndView("pages/errors/500");
    commonModelPopulator.populate(view, request, response);
    return view;
  }

  /** The autowired CommonModelPopulator object. */
  @Autowired private CommonModelPopulator commonModelPopulator;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ExceptionHandlingController.class);
}
