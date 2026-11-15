/*!
 * Verwandlung Online Judge - administration chrome behaviour (vanilla JS).
 * Sidebar drawer (mobile) + checkbox data-table selection / bulk bar.
 * The theme toggle reuses [data-vw="theme-toggle"], handled by theme.js.
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') { fn(); }
    else { document.addEventListener('DOMContentLoaded', fn); }
  }

  // --- ARIA state for the custom admin controls ------------------------------
  // .adm-switch (<button role="switch">) and .adm-check (row / select-all
  // checkboxes) keep their state in an `.on` class that page scripts also flip;
  // .adm-seg options mark the chosen one with `.active`. Mirror those classes
  // into aria-checked / aria-pressed, including on controls added later.
  function textOf(el) { return el ? el.textContent.replace(/\s+/g, ' ').trim() : ''; }

  function labelSwitch(sw) {
    if (sw.hasAttribute('aria-label') || sw.hasAttribute('aria-labelledby')) return;
    var row = sw.closest('.adm-setrow, .adm-togrow, .vw-between, .language');
    var lbl = row && row.querySelector('.lbl, .language-name, span:not(.knob)');
    var text = lbl ? (lbl.value !== undefined && lbl.tagName === 'INPUT' ? lbl.value : textOf(lbl)) : '';
    if (text) sw.setAttribute('aria-label', text);
  }

  function enhanceCheck(c) {
    if (c.getAttribute('role') !== 'checkbox') {
      c.setAttribute('role', 'checkbox');
      c.setAttribute('tabindex', '0');
    }
    if (!c.hasAttribute('aria-label')) {
      var row = c.closest('[data-adm="row"]');
      var label = row ? textOf(row.querySelector('.adm-cell.head') || row.querySelector('.adm-cell'))
                      : document.body.getAttribute('data-select-all-label');
      if (label) c.setAttribute('aria-label', label);
    }
  }

  function syncState(el) {
    if (el.classList.contains('adm-switch') || el.classList.contains('adm-check')) {
      el.setAttribute('aria-checked', el.classList.contains('on') ? 'true' : 'false');
    } else if (el.parentNode && el.parentNode.classList && el.parentNode.classList.contains('adm-seg')) {
      el.setAttribute('aria-pressed', el.classList.contains('active') ? 'true' : 'false');
    }
  }

  function enhance(scope) {
    Array.prototype.forEach.call(scope.querySelectorAll('.adm-switch'), function (sw) { labelSwitch(sw); syncState(sw); });
    Array.prototype.forEach.call(scope.querySelectorAll('.adm-check'), function (c) { enhanceCheck(c); syncState(c); });
    Array.prototype.forEach.call(scope.querySelectorAll('.adm-seg > [data-value]'), syncState);
  }

  ready(function () {
    enhance(document);
    new MutationObserver(function (records) {
      records.forEach(function (r) {
        if (r.type === 'attributes') { syncState(r.target); return; }
        Array.prototype.forEach.call(r.addedNodes, function (n) { if (n.nodeType === 1) enhance(n.parentNode || n); });
      });
    }).observe(document.body, { subtree: true, childList: true, attributes: true, attributeFilter: ['class'] });

    // Space / Enter on a focused checkbox span (buttons already do this natively).
    // preventDefault keeps Space from scrolling and Enter from following the row link.
    document.addEventListener('keydown', function (e) {
      var t = e.target;
      if ((e.key === ' ' || e.key === 'Enter') && t.classList && t.classList.contains('adm-check')) {
        e.preventDefault();
        t.click();
      }
    });

    // --- Sidebar drawer (mobile) ---------------------------------------------
    // Below 861px the sidebar is an off-canvas drawer: keep it out of the tab
    // order while closed (inert), reflect the state on the burger, move focus in
    // on open and back to the burger on close (scrim click or Esc).
    var sidebar = document.querySelector('[data-adm="sidebar"]');
    var scrim = document.querySelector('[data-adm="scrim"]');
    var burger = document.querySelector('[data-adm="burger"]');
    var narrow = window.matchMedia('(max-width: 860px)');
    function isOpen() { return !!sidebar && sidebar.classList.contains('open'); }
    function syncInert() { if (sidebar) sidebar.inert = narrow.matches && !isOpen(); }
    function openSb() {
      if (!sidebar) return;
      sidebar.classList.add('open');
      if (scrim) scrim.classList.add('open');
      if (burger) burger.setAttribute('aria-expanded', 'true');
      syncInert();
      var first = sidebar.querySelector('a, button');
      if (first) first.focus();
    }
    function closeSb() {
      if (!isOpen()) return;
      sidebar.classList.remove('open');
      if (scrim) scrim.classList.remove('open');
      if (burger) { burger.setAttribute('aria-expanded', 'false'); if (narrow.matches) burger.focus(); }
      syncInert();
    }
    if (burger) burger.addEventListener('click', function () { if (isOpen()) closeSb(); else openSb(); });
    if (scrim) scrim.addEventListener('click', closeSb);
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeSb(); });
    if (narrow.addEventListener) narrow.addEventListener('change', syncInert);
    syncInert();

    // --- Toggle switches (settings / editors) --------------------------------
    // Any [data-adm="switch"] flips its `.on` class on click; pages read the
    // `.on` state (or the optional hidden input named by data-input) on save.
    Array.prototype.forEach.call(document.querySelectorAll('[data-adm="switch"]'), function (sw) {
      sw.addEventListener('click', function () {
        var on = sw.classList.toggle('on');
        var inputId = sw.getAttribute('data-input');
        if (inputId) {
          var input = document.getElementById(inputId);
          if (input) input.value = on ? '1' : '0';
        }
      });
    });

    // --- Segmented controls (single-select) ----------------------------------
    // [data-adm="seg"] groups: clicking an option activates it and writes its
    // data-value into the hidden input named by the group's data-input.
    Array.prototype.forEach.call(document.querySelectorAll('[data-adm="seg"]'), function (seg) {
      var inputId = seg.getAttribute('data-input');
      Array.prototype.forEach.call(seg.querySelectorAll('[data-value]'), function (opt) {
        opt.addEventListener('click', function (e) {
          e.preventDefault();
          Array.prototype.forEach.call(seg.querySelectorAll('[data-value]'), function (o) { o.classList.remove('active'); });
          opt.classList.add('active');
          if (inputId) {
            var input = document.getElementById(inputId);
            if (input) input.value = opt.getAttribute('data-value');
          }
        });
      });
    });

    // --- Data-driven fill bars -----------------------------------------------
    // Elements carrying data-pct expose their value to CSS via the --adm-pct
    // custom property, keeping percentage widths/heights out of the markup.
    Array.prototype.forEach.call(document.querySelectorAll('[data-pct]'), function (el) {
      el.style.setProperty('--adm-pct', el.getAttribute('data-pct') + '%');
    });

    // --- Checkbox data table -------------------------------------------------
    var table = document.querySelector('[data-adm="table"]');
    if (!table) return;

    var checkAll = table.querySelector('[data-adm="check-all"]');
    var bulk = document.querySelector('[data-adm="bulk"]');
    var bulkCount = bulk ? bulk.querySelector('[data-adm="bulk-count"]') : null;
    var bulkClear = bulk ? bulk.querySelector('[data-adm="bulk-clear"]') : null;

    function rowChecks() { return Array.prototype.slice.call(table.querySelectorAll('[data-adm="row-check"]')); }
    function selected() { return rowChecks().filter(function (c) { return c.classList.contains('on'); }); }

    function paint(el, on) {
      el.classList.toggle('on', on);
      el.innerHTML = on ? '✓' : '';
    }

    function refresh() {
      var sel = selected();
      var n = sel.length;
      if (checkAll) {
        var all = rowChecks().length > 0 && n === rowChecks().length;
        paint(checkAll, all);
      }
      if (bulk) {
        bulk.classList.toggle('is-open', n > 0);
        if (bulkCount) bulkCount.textContent = n;
      }
    }

    // Toggling a row checkbox must not follow the row's link / open the editor.
    rowChecks().forEach(function (c) {
      c.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
        paint(c, !c.classList.contains('on'));
        c.closest('[data-adm="row"]').classList.toggle('sel', c.classList.contains('on'));
        refresh();
      });
    });

    if (checkAll) {
      checkAll.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
        var turnOn = selected().length !== rowChecks().length;
        rowChecks().forEach(function (c) {
          paint(c, turnOn);
          var row = c.closest('[data-adm="row"]');
          if (row) row.classList.toggle('sel', turnOn);
        });
        refresh();
      });
    }

    if (bulkClear) {
      bulkClear.addEventListener('click', function () {
        rowChecks().forEach(function (c) {
          paint(c, false);
          var row = c.closest('[data-adm="row"]');
          if (row) row.classList.remove('sel');
        });
        refresh();
      });
    }

    // Collect the selected row ids (data-id) - used by page delete/rejudge AJAX.
    window.admSelectedIds = function () {
      return selected().map(function (c) { return c.getAttribute('data-id'); });
    };
    window.admRefreshBulk = refresh;

    refresh();
  });
})();
