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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

/**
 * The test class for MailSender.
 *
 * @author Haozhe Xie
 */
public class MailSenderTest {
  /** Builds a MailSender backed by a mocked template engine (its constructor is private). */
  @BeforeEach
  public void setUp() throws Exception {
    templateEngine = mock(ITemplateEngine.class);
    JavaMailSender mailSender = mock(JavaMailSender.class);

    Constructor<MailSender> constructor =
        MailSender.class.getDeclaredConstructor(ITemplateEngine.class, JavaMailSender.class);
    constructor.setAccessible(true);
    vojMailSender = constructor.newInstance(templateEngine, mailSender);
    ReflectionTestUtils.setField(vojMailSender, "baseUrl", "https://voj.example.org");
  }

  /** Test case: tests getMailContent(String, Map). Test data: a template name and a model. Expected: the rendered output is returned and the base URL is injected into the model. */
  @Test
  public void testGetMailContentInjectsBaseUrlAndReturnsRenderedBody() {
    when(templateEngine.process(eq("mail/reset-password"), org.mockito.ArgumentMatchers.any(IContext.class)))
        .thenReturn("<html>rendered</html>");

    Map<String, Object> model = new HashMap<>();
    model.put("username", "zjhzxhz");
    String content = vojMailSender.getMailContent("mail/reset-password", model);

    Assertions.assertEquals("<html>rendered</html>", content);
    Assertions.assertEquals("https://voj.example.org", model.get("baseUrl"));
  }

  /** Test case: tests getMailContent(String, Map). Test data: a template name and a model. Expected: the template engine receives a Thymeleaf context carrying the model variables. */
  @Test
  public void testGetMailContentPassesModelToTemplateEngine() {
    when(templateEngine.process(eq("mail/welcome"), org.mockito.ArgumentMatchers.any(IContext.class)))
        .thenReturn("body");

    Map<String, Object> model = new HashMap<>();
    model.put("username", "zjhzxhz");
    vojMailSender.getMailContent("mail/welcome", model);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    org.mockito.Mockito.verify(templateEngine).process(eq("mail/welcome"), contextCaptor.capture());
    Context context = contextCaptor.getValue();
    Assertions.assertEquals("zjhzxhz", context.getVariable("username"));
    Assertions.assertEquals("https://voj.example.org", context.getVariable("baseUrl"));
  }

  /** The mocked Thymeleaf template engine. */
  private ITemplateEngine templateEngine;

  /** The MailSender object under test. */
  private MailSender vojMailSender;
}
