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
package org.verwandlung.voj.web.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * The Spring Security configuration. Replaces the home-grown {@code CsrfProtector}, the {@code
 * InterceptorAspect} authorisation aspect and the {@code SessionListener} online-user counter.
 *
 * <ul>
 *   <li><b>Authentication</b> is performed programmatically by {@code AccountsController} so that the
 *       existing AJAX login (which returns a JSON result) is preserved.
 *   <li><b>Authorisation</b> guards {@code /administration/**} (administrators only) and the
 *       account self-service actions (change password / update profile — any authenticated user).
 *   <li><b>CSRF</b> protection is enabled globally; the token is submitted via the {@code csrfToken}
 *       request parameter (matching the existing forms) or the {@code X-CSRF-TOKEN} header.
 *   <li><b>Online users</b> are tracked through a {@link SessionRegistry}.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Configuration
public class SecurityConfig {
  /** The login page unauthenticated users are redirected to. */
  private static final String LOGIN_URL = "/accounts/login";

  /** The page a forbidden (authenticated but unauthorised) user is redirected to. */
  private static final String ACCESS_DENIED_URL = "/";

  /**
   * The security filter chain. Guarded by {@link ConditionalOnWebApplication} so the bean is only
   * built inside a servlet web application — the {@code HttpSecurity} it depends on is not present in
   * the plain application contexts used by the service-layer integration tests.
   */
  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
    HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
    csrfTokenRepository.setParameterName("csrfToken");

    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/administration", "/administration/**")
                    .hasRole("ADMINISTRATORS")
                    .requestMatchers(
                        "/accounts/changePassword.action", "/accounts/updateProfile.action")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_URL))
                    .accessDeniedHandler(accessDeniedHandler()))
        .sessionManagement(
            session -> session.maximumSessions(-1).sessionRegistry(sessionRegistry))
        // Authentication and logout are driven by AccountsController, so the built-in
        // form-login, HTTP basic and logout endpoints are disabled.
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    return http.build();
  }

  /**
   * The password encoder. New passwords are stored as {@code {bcrypt}...}; legacy un-prefixed MD5
   * hashes are matched via MD5 and transparently re-encoded to bcrypt on the next successful login.
   *
   * @return the delegating password encoder
   */
  @Bean
  @SuppressWarnings("deprecation")
  public PasswordEncoder passwordEncoder() {
    MessageDigestPasswordEncoder md5Encoder = new MessageDigestPasswordEncoder("MD5");
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("MD5", md5Encoder);

    DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
    // Existing rows store a bare 32-character MD5 digest without an {id} prefix.
    passwordEncoder.setDefaultPasswordEncoderForMatches(md5Encoder);
    return passwordEncoder;
  }

  /** The registry of authenticated sessions, used to count online users. */
  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  /** Publishes servlet session lifecycle events so the {@link SessionRegistry} stays up to date. */
  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  /** Redirects an authenticated but unauthorised user to the home page. */
  private AccessDeniedHandlerImpl accessDeniedHandler() {
    AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();
    accessDeniedHandler.setErrorPage(ACCESS_DENIED_URL);
    return accessDeniedHandler;
  }
}
