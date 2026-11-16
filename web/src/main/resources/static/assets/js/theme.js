/*!
 * Verwandlung Online Judge - UI chrome behaviour (vanilla JS, no jQuery).
 *
 *  - Theme: dark/light toggle. With nothing stored the page follows the system
 *    colour scheme (and keeps following it live); toggling stores an override only
 *    when it differs from the system scheme, so toggling back returns to "system".
 *    The initial theme is applied by the inline script in fragments/chrome.html
 *    (theme-head) before first paint.
 *  - Footer language menu: a native popover (light dismiss, Esc, focus return and
 *    the expanded state come from the browser) placed by CSS anchor positioning;
 *    its links reload the page in the chosen language.
 *  - vojConfirm(message) / vojAlert(message): promise-based replacements for the
 *    blocking confirm() / alert(), rendered in the shared <dialog> (fragments/chrome).
 */
(function () {
  'use strict';

  var STORAGE_KEY = 'voj-theme';
  var root = document.documentElement;
  var systemLight = window.matchMedia ? window.matchMedia('(prefers-color-scheme: light)') : null;

  function forEach(list, fn) { Array.prototype.forEach.call(list, fn); }

  function ready(fn) {
    if (document.readyState !== 'loading') { fn(); }
    else { document.addEventListener('DOMContentLoaded', fn); }
  }

  // ---- Theme ---------------------------------------------------------------
  function systemTheme() { return systemLight && systemLight.matches ? 'light' : 'dark'; }

  function storedTheme() {
    try {
      var t = localStorage.getItem(STORAGE_KEY);
      return t === 'light' || t === 'dark' ? t : null;
    } catch (e) { return null; }
  }

  function currentTheme() { return root.getAttribute('data-theme') === 'light' ? 'light' : 'dark'; }

  function syncToggles(theme) {
    forEach(document.querySelectorAll('[data-vw="theme-toggle"]'), function (el) {
      el.setAttribute('aria-pressed', theme === 'dark' ? 'true' : 'false');
    });
  }

  function render(theme) {
    var changed = theme !== currentTheme();
    root.setAttribute('data-theme', theme);
    var meta = document.querySelector('meta[name="color-scheme"]');
    if (meta) { meta.content = theme; }
    syncToggles(theme);
    // Let widgets that cannot follow the CSS variables (e.g. Turnstile) re-theme themselves.
    if (changed) {
      document.dispatchEvent(new CustomEvent('voj:themechange', { detail: { theme: theme } }));
    }
  }

  function toggleTheme() {
    var target = currentTheme() === 'dark' ? 'light' : 'dark';
    try {
      // Matching the system scheme means "undo my override": forget it instead of
      // pinning it, so the page keeps following future system changes.
      if (target === systemTheme()) { localStorage.removeItem(STORAGE_KEY); }
      else { localStorage.setItem(STORAGE_KEY, target); }
    } catch (e) { /* storage blocked: still switch for this page view */ }
    render(target);
  }

  if (systemLight) {
    var onSystemChange = function () { if (!storedTheme()) { render(systemTheme()); } };
    systemLight.addEventListener('change', onSystemChange);
  }
  // Another tab changed the override.
  window.addEventListener('storage', function (e) {
    if (e.key === STORAGE_KEY || e.key === null) { render(storedTheme() || systemTheme()); }
  });

  // ---- Shared confirm / alert dialog ---------------------------------------
  var dialogQueue = Promise.resolve();

  function showDialog(message, isConfirm) {
    var run = function () {
      return new Promise(function (resolve) {
        var dialog = document.getElementById('vw-dialog');
        if (!dialog || typeof dialog.showModal !== 'function') {
          // No shared dialog on this page (or no <dialog> support): use the native one.
          if (isConfirm) { resolve(window.confirm(message)); }
          else { window.alert(message); resolve(true); }
          return;
        }
        var cancelBtn = dialog.querySelector('button[value="cancel"]');
        var okBtn = dialog.querySelector('button[value="ok"]');
        dialog.querySelector('.vw-dialog-message').textContent = message == null ? '' : String(message);
        cancelBtn.hidden = !isConfirm;
        // Initial focus: the safe action for a confirmation, OK for a notice.
        cancelBtn.autofocus = isConfirm;
        okBtn.autofocus = !isConfirm;
        dialog.returnValue = '';
        dialog.addEventListener('close', function onClose() {
          dialog.removeEventListener('close', onClose);
          var confirmed = dialog.returnValue === 'ok';
          dialog.returnValue = '';
          resolve(isConfirm ? confirmed : true);
        });
        dialog.showModal();
      });
    };
    dialogQueue = dialogQueue.then(run, run);
    return dialogQueue;
  }

  /** Ask a yes/no question. Resolves to true only when the user picks OK. */
  window.vojConfirm = function (message) { return showDialog(message, true); };
  /** Show a notice. Resolves once it is dismissed. */
  window.vojAlert = function (message) { return showDialog(message, false); };

  // Light dismiss for <dialog closedby="any"> where the attribute is unsupported (Safari).
  // Test the pointer position rather than event.target: the dialog is also the target for
  // its own padding, and the press and release must both land outside so a text-selection
  // drag that ends on the backdrop does not dismiss it.
  function lightDismissFallback() {
    if ('closedBy' in HTMLDialogElement.prototype) { return; }
    var outside = function (e, dialog) {
      var r = dialog.getBoundingClientRect();
      return e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
    };
    forEach(document.querySelectorAll('dialog[closedby="any"]'), function (dialog) {
      var pressedOutside = false;
      dialog.addEventListener('pointerdown', function (e) { pressedOutside = outside(e, dialog); });
      dialog.addEventListener('pointerup', function (e) {
        if (pressedOutside && outside(e, dialog)) { dialog.close(); }
      });
    });
  }

  // Declarative hooks that replace inline on* handlers in the templates:
  //  - <select data-autosubmit> submits its form when the selection changes;
  //  - <button data-vw="history-back"> goes back one page.
  document.addEventListener('change', function (e) {
    var el = e.target;
    if (el.hasAttribute && el.hasAttribute('data-autosubmit') && el.form) { el.form.submit(); }
  });
  document.addEventListener('click', function (e) {
    var el = e.target.closest ? e.target.closest('[data-vw="history-back"]') : null;
    if (el) { e.preventDefault(); window.history.back(); }
  });

  ready(function () {
    if (typeof HTMLDialogElement !== 'undefined') { lightDismissFallback(); }

    // Theme toggle(s).
    syncToggles(currentTheme());
    forEach(document.querySelectorAll('[data-vw="theme-toggle"]'), function (el) {
      el.addEventListener('click', toggleTheme);
    });

    // Language switch: reload the current page with ?language=<code> (read by the
    // server-side LocaleChangeInterceptor), keeping the rest of the query string.
    // The links' own href (?language=<code>) is the no-script fallback.
    forEach(document.querySelectorAll('.vw-lang [data-language]'), function (a) {
      a.addEventListener('click', function (e) {
        e.preventDefault();
        var u = new URL(window.location.href);
        u.searchParams.set('language', a.getAttribute('data-language'));
        window.location.href = u.toString();
      });
    });
  });
})();
