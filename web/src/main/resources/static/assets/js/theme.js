/*!
 * Verwandlung Online Judge - UI chrome behaviour (vanilla JS, no jQuery).
 * Handles the dark/light theme toggle (persisted in localStorage), the mobile
 * navigation drawer and the footer language dropdown. The initial theme is
 * applied by a tiny inline script in <head> (see layouts/main.html) to avoid a
 * flash of the wrong theme before this file loads.
 */
(function () {
  'use strict';

  var STORAGE_KEY = 'voj-theme';

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    try { localStorage.setItem(STORAGE_KEY, theme); } catch (e) {}
  }

  function currentTheme() {
    return document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
  }

  document.addEventListener('DOMContentLoaded', function () {
    // Theme toggle(s).
    var toggles = document.querySelectorAll('[data-vw="theme-toggle"]');
    Array.prototype.forEach.call(toggles, function (el) {
      el.addEventListener('click', function () {
        applyTheme(currentTheme() === 'dark' ? 'light' : 'dark');
      });
    });

    // Mobile navigation drawer.
    var burger = document.querySelector('[data-vw="burger"]');
    var menu = document.querySelector('[data-vw="menu"]');
    if (burger && menu) {
      burger.addEventListener('click', function () { menu.classList.toggle('open'); });
    }

    // Footer language dropdown (and any other [data-vw="dropdown"] container).
    var dropdowns = document.querySelectorAll('[data-vw="dropdown"]');
    Array.prototype.forEach.call(dropdowns, function (dd) {
      var btn = dd.querySelector('[data-vw="dropdown-btn"]');
      if (!btn) return;
      btn.addEventListener('click', function (e) {
        e.stopPropagation();
        dd.classList.toggle('open');
      });
    });
    document.addEventListener('click', function () {
      Array.prototype.forEach.call(dropdowns, function (dd) { dd.classList.remove('open'); });
    });

    // Bootstrap-style dropdowns (admin header user menu).
    var ddToggles = document.querySelectorAll('.dropdown-toggle');
    Array.prototype.forEach.call(ddToggles, function (toggle) {
      toggle.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
        var dd = toggle.closest('.dropdown');
        if (dd) dd.classList.toggle('open');
      });
    });
    document.addEventListener('click', function () {
      Array.prototype.forEach.call(document.querySelectorAll('.dropdown.open'), function (dd) { dd.classList.remove('open'); });
    });

    // Tabs (replaces the old Bootstrap data-toggle="tab" behaviour).
    var tabLinks = document.querySelectorAll('[data-toggle="tab"]');
    Array.prototype.forEach.call(tabLinks, function (link) {
      link.addEventListener('click', function (e) {
        e.preventDefault();
        var targetSel = link.getAttribute('href');
        if (!targetSel || targetSel.charAt(0) !== '#') return;
        var pane = document.querySelector(targetSel);
        if (!pane) return;
        var content = pane.parentNode;
        Array.prototype.forEach.call(content.children, function (c) { c.classList.remove('active'); });
        pane.classList.add('active');
        var navItem = link.closest('li');
        if (navItem && navItem.parentNode) {
          Array.prototype.forEach.call(navItem.parentNode.children, function (li) { li.classList.remove('active'); });
          navItem.classList.add('active');
        }
      });
    });

    // Language switch: reload the current page with ?language=<code>, which the
    // server-side LocaleChangeInterceptor reads to update the session locale.
    var langOptions = document.querySelectorAll('.vw-lang [data-language]');
    Array.prototype.forEach.call(langOptions, function (a) {
      a.addEventListener('click', function (e) {
        e.preventDefault();
        var code = a.getAttribute('data-language');
        try {
          var u = new URL(window.location.href);
          u.searchParams.set('language', code);
          window.location.href = u.toString();
        } catch (err) {
          window.location.href = window.location.pathname + '?language=' + encodeURIComponent(code);
        }
      });
    });
  });
})();
