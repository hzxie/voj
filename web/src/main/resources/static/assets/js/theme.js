/*!
 * Verwandlung Online Judge - UI chrome behaviour (vanilla JS, no jQuery).
 *
 *  - Theme: dark/light toggle. With nothing stored the page follows the system
 *    colour scheme (and keeps following it live); toggling stores an override only
 *    when it differs from the system scheme, so toggling back returns to "system".
 *    The initial theme is applied by the inline script in fragments/chrome.html
 *    (theme-head) before first paint.
 *  - Site navigation: one list at every width. It stays a popover behind the menu
 *    button until brand + links + account actions fit on one row, then becomes the
 *    inline row (header.is-wide). There is no breakpoint: the row is measured, so a
 *    longer translation or larger text moves the switch point by itself.
 *  - Footer language menu: a native popover (light dismiss, Esc, focus return and
 *    the expanded state come from the browser), positioned here where CSS anchor
 *    positioning is missing. Both menus fall back to class toggling where popover
 *    itself is unsupported.
 *  - vojConfirm(message) / vojAlert(message): promise-based replacements for the
 *    blocking confirm() / alert(), rendered in the shared <dialog> (fragments/chrome).
 */
(function () {
  'use strict';

  var STORAGE_KEY = 'voj-theme';
  var root = document.documentElement;
  var systemLight = window.matchMedia ? window.matchMedia('(prefers-color-scheme: light)') : null;
  var supportsPopover = 'popover' in HTMLElement.prototype;

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
    if (systemLight.addEventListener) { systemLight.addEventListener('change', onSystemChange); }
    else if (systemLight.addListener) { systemLight.addListener(onSystemChange); }
  }
  // Another tab changed the override.
  window.addEventListener('storage', function (e) {
    if (e.key === STORAGE_KEY || e.key === null) { render(storedTheme() || systemTheme()); }
  });

  // ---- Popover fallbacks ---------------------------------------------------
  // Where popover is unsupported, the trigger toggles an `.open` class instead
  // (styled in app.css) and keeps aria-expanded in sync; Esc and outside clicks close it.
  function classToggle(trigger, panel, openTarget) {
    function set(open) {
      openTarget.classList.toggle('open', open);
      trigger.setAttribute('aria-expanded', open ? 'true' : 'false');
    }
    set(false);
    trigger.addEventListener('click', function (e) {
      e.stopPropagation();
      set(!openTarget.classList.contains('open'));
    });
    document.addEventListener('click', function (e) {
      if (!panel.contains(e.target)) { set(false); }
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && openTarget.classList.contains('open')) {
        set(false);
        trigger.focus();
      }
    });
  }

  // Place the language popover above its button, right-aligned, where CSS anchor
  // positioning is unavailable (with it, app.css does this declaratively).
  var supportsAnchoring = window.CSS && CSS.supports && CSS.supports('position-area', 'block-start');
  function positionAbove(trigger, panel) {
    var r = trigger.getBoundingClientRect(), gap = 8;
    var w = panel.offsetWidth, h = panel.offsetHeight;
    var left = Math.min(Math.max(gap, r.right - w), window.innerWidth - w - gap);
    var top = r.top - h - gap;
    if (top < gap) { top = r.bottom + gap; }
    panel.style.left = Math.max(gap, left) + 'px';
    panel.style.top = top + 'px';
  }

  // ---- Site navigation layout ----------------------------------------------
  // Measure the inline row with .is-wide applied: if everything fits, keep it
  // inline and drop the popover attribute; otherwise collapse back to the popover.
  // While collapsed, only remeasure once the header has grown (switching to inline
  // just to measure on every shrink would thrash). Removing `popover` closes an
  // open menu, so reopen it if the layout stays collapsed.
  function setupSiteNav(header, burger, menu) {
    var lastWidth = -1;

    function setPopover(on) {
      // setAttribute/removeAttribute rather than the IDL property (polyfill-safe).
      if (on) { menu.setAttribute('popover', 'auto'); } else { menu.removeAttribute('popover'); }
    }

    function isOpen() {
      if (menu.classList.contains('open')) { return true; }
      try { return menu.matches(':popover-open'); } catch (e) { return false; }
    }

    function layout() {
      var width = header.clientWidth;
      var wide = header.classList.contains('is-wide');
      if (!wide && width <= lastWidth) { lastWidth = width; return; }
      lastWidth = width;

      var wasOpen = isOpen();
      header.classList.add('is-measuring');
      setPopover(false);
      menu.classList.remove('open');
      header.classList.add('is-wide');
      var fits = header.scrollWidth <= header.clientWidth;
      if (!fits) {
        header.classList.remove('is-wide');
        setPopover(supportsPopover);
        if (wasOpen) {
          if (supportsPopover) { menu.showPopover(); } else { menu.classList.add('open'); }
        }
      }
      // Native popovertarget exposes the expanded state itself; the fallback does not.
      if (!supportsPopover) { burger.setAttribute('aria-expanded', !fits && wasOpen ? 'true' : 'false'); }
      window.requestAnimationFrame(function () { header.classList.remove('is-measuring'); });
    }

    if (!supportsPopover) { classToggle(burger, menu, menu); }
    layout();
    if (window.ResizeObserver) {
      var ro = new ResizeObserver(function () { layout(); });
      ro.observe(header);
      ro.observe(menu);
    } else {
      window.addEventListener('resize', layout);
    }
  }

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

    // Site navigation (public header).
    var siteHeader = document.querySelector('[data-vw="site-header"]');
    if (siteHeader) {
      var burger = siteHeader.querySelector('[data-vw="burger"]');
      var menu = siteHeader.querySelector('[data-vw="menu"]');
      if (burger && menu) { setupSiteNav(siteHeader, burger, menu); }
    }

    // Footer language menu.
    forEach(document.querySelectorAll('[data-vw="dropdown"]'), function (dd) {
      var btn = dd.querySelector('[data-vw="dropdown-btn"]');
      var panel = dd.querySelector('[popover]');
      if (!btn || !panel) { return; }
      if (!supportsPopover) {
        classToggle(btn, dd, dd);
      } else if (!supportsAnchoring) {
        panel.addEventListener('toggle', function (e) {
          if (e.newState === 'open') { positionAbove(btn, panel); }
        });
      }
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
