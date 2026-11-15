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
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.interceptor.CommonModelPopulator;
import org.verwandlung.voj.web.util.LogSanitizer;

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
   * Handles the NoResourceFoundException, thrown when an unmapped URL falls through to the static
   * resource handler and matches no resource. Rendering the 404 view keeps a stale link (e.g. to a
   * removed page) a clean Not Found rather than the catch-all 500.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoResourceFoundException.class)
  public ModelAndView noResourceFoundView(
      HttpServletRequest request, HttpServletResponse response) {
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
   * Handles a request parameter or path variable that cannot be converted to the handler's argument
   * type (e.g. {@code GET /p/abc} for a numeric problem id). A malformed path variable names a
   * resource that cannot exist, so it is a 404; a malformed request parameter is a 400. Either way
   * it is the client's mistake, not a server error, so it is neither logged as an error nor
   * rendered as a 500.
   *
   * @param ex - the thrown exception object
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ExceptionHandler(TypeMismatchException.class)
  public ModelAndView typeMismatchView(
      TypeMismatchException ex, HttpServletRequest request, HttpServletResponse response) {
    // No converter at all is a programming error rather than bad input.
    if (ex instanceof ConversionNotSupportedException) {
      return internalServerErrorView(ex, request, response);
    }
    boolean isPathVariable =
        ex instanceof MethodArgumentTypeMismatchException
            && ((MethodArgumentTypeMismatchException) ex)
                .getParameter()
                .hasParameterAnnotation(PathVariable.class);
    return clientErrorView(
        isPathVariable ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST, ex, request, response);
  }

  /**
   * Handles a request body that cannot be read (e.g. malformed JSON) as a 400.
   *
   * @param ex - the thrown exception object
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ModelAndView messageNotReadableView(
      HttpMessageNotReadableException ex,
      HttpServletRequest request,
      HttpServletResponse response) {
    return clientErrorView(HttpStatus.BAD_REQUEST, ex, request, response);
  }

  /**
   * Renders the error page for a client error (4xx) with the given status. Client errors are caused
   * by the request, so they are only logged at debug level rather than as server errors.
   *
   * @param status - the HTTP status of the response
   * @param ex - the thrown exception object
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  private ModelAndView clientErrorView(
      HttpStatusCode status,
      Exception ex,
      HttpServletRequest request,
      HttpServletResponse response) {
    LOGGER.debug(
        "Client error {} for {}: {}",
        status.value(),
        LogSanitizer.forLog(request.getRequestURI()),
        LogSanitizer.forLog(ex.getMessage()));
    if (response.isCommitted()) {
      return null;
    }
    response.setStatus(status.value());
    ModelAndView view = new ModelAndView("pages/errors/404");
    view.setStatus(status);
    commonModelPopulator.populate(view, request, response);
    return view;
  }

  /**
   * Handles general exceptions. Spring MVC exceptions that describe a client error (e.g. a missing
   * request header, an unsupported media type or a failed argument validation) carry their 4xx
   * status and are rendered as client errors; everything else is an unexpected server error (500).
   *
   * @param ex - the thrown exception object
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @ExceptionHandler(Exception.class)
  public ModelAndView internalServerErrorView(
      Exception ex, HttpServletRequest request, HttpServletResponse response) {
    if (ex instanceof ErrorResponse
        && ((ErrorResponse) ex).getStatusCode().is4xxClientError()) {
      return clientErrorView(((ErrorResponse) ex).getStatusCode(), ex, request, response);
    }
    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    // A committed response (e.g. an in-flight SSE/async stream whose send() failed because
    // the client disconnected) can no longer be turned into an HTML error page: Thymeleaf
    // would call response.getWriter() on a response whose OutputStream is already open and
    // throw "getOutputStream() has already been called", masking the real cause. Nothing can
    // be rendered, so log the original exception and let the container close the connection.
    if (response.isCommitted()) {
      LOGGER.warn("Suppressing error view for an already-committed response", ex);
      return null;
    }

    String requestId = newRequestId();
    // Log the stack trace tagged with the same id shown to the user, so a reported
    // request-id can be located in the logs.
    LOGGER.error("[request-id: {}] Unhandled exception", requestId, ex);

    ModelAndView view = new ModelAndView("pages/errors/500");
    view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    commonModelPopulator.populate(view, request, response);
    view.addObject("requestId", requestId);
    return view;
  }

  /**
   * Generates a short, human-quotable correlation id (three groups of four hex digits) used to tie
   * the error page shown to the user to the corresponding entry in the server log.
   *
   * @return a request id of the form XXXX:XXXX:XXXX
   */
  private String newRequestId() {
    String hex =
        java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    return hex.substring(0, 4) + ":" + hex.substring(4, 8) + ":" + hex.substring(8, 12);
  }

  /** The autowired CommonModelPopulator object. */
  @Autowired private CommonModelPopulator commonModelPopulator;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ExceptionHandlingController.class);
}
