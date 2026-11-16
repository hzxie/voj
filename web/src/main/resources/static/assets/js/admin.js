/*!
 * Verwandlung Online Judge - administration chrome behaviour (vanilla JS).
 * Navigation drawer/sidebar + ARIA state for the custom controls + checkbox
 * data-table selection / bulk bar.
 * The theme toggle reuses [data-vw="theme-toggle"], handled by theme.js.
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') { fn(); }
    else { document.addEventListener('DOMContentLoaded', fn); }
  }

  // --- ARIA state for the custom admin controls ------------------------------
  // .adm-switch (<button role="switch">) keeps its state in an `.on` class that page
  // scripts also flip; .adm-seg options mark the chosen one with `.active`. Mirror those classes
  // into aria-checked / aria-pressed, including on controls added later.
  function textOf(el) { return el ? el.textContent.replace(/\s+/g, ' ').trim() : ''; }

  function labelSwitch(sw) {
    if (sw.hasAttribute('aria-label') || sw.hasAttribute('aria-labelledby')) return;
    var row = sw.closest('.adm-setrow, .adm-togrow, .vw-between, .language');
    var lbl = row && row.querySelector('.lbl, .language-name, span:not(.knob)');
    var text = lbl ? (lbl.value !== undefined && lbl.tagName === 'INPUT' ? lbl.value : textOf(lbl)) : '';
    if (text) sw.setAttribute('aria-label', text);
  }

  function syncState(el) {
    if (el.classList.contains('adm-switch')) {
      el.setAttribute('aria-checked', el.classList.contains('on') ? 'true' : 'false');
    } else if (el.parentNode && el.parentNode.classList && el.parentNode.classList.contains('adm-seg')) {
      el.setAttribute('aria-pressed', el.classList.contains('active') ? 'true' : 'false');
    }
  }

  function enhance(scope) {
    Array.prototype.forEach.call(scope.querySelectorAll('.adm-switch'), function (sw) { labelSwitch(sw); syncState(sw); });
    Array.prototype.forEach.call(scope.querySelectorAll('.adm-seg > [data-value]'), syncState);
  }

  // --- Navigation drawer that is also the sidebar ----------------------------
  // The sidebar is a <dialog>. Wide screens never open it (css/admin.css shows it in
  // flow); on narrow screens the burger opens it with showModal(), so the browser
  // supplies the modality, Esc, the inert page and the focus return. Closed, it is
  // role="none" so the in-flow sidebar is not announced as a dialog.
  function setupDrawer() {
    var drawer = document.querySelector('[data-adm="sidebar"]');
    var burger = document.querySelector('[data-adm="burger"]');
    if (!drawer || !burger) return;
    var closeBtn = drawer.querySelector('[data-adm="drawer-close"]');
    var narrow = window.matchMedia('(max-width: 860px)');

    burger.addEventListener('click', function () {
      if (drawer.open) return;
      drawer.removeAttribute('role');
      drawer.setAttribute('aria-label', drawer.getAttribute('data-dialog-label'));
      drawer.showModal();
      burger.setAttribute('aria-expanded', 'true');
    });
    drawer.addEventListener('close', function () {
      drawer.setAttribute('role', 'none');
      drawer.removeAttribute('aria-label');
      burger.setAttribute('aria-expanded', 'false');
    });
    if (closeBtn) closeBtn.addEventListener('click', function () { drawer.close(); });
    // The wide layout has no drawer: close one that is open when the window widens.
    narrow.addEventListener('change', function () {
      if (!narrow.matches && drawer.open) drawer.close();
    });
  }

  ready(function () {
    enhance(document);
    new MutationObserver(function (records) {
      records.forEach(function (r) {
        if (r.type === 'attributes') { syncState(r.target); return; }
        Array.prototype.forEach.call(r.addedNodes, function (n) { if (n.nodeType === 1) enhance(n.parentNode || n); });
      });
    }).observe(document.body, { subtree: true, childList: true, attributes: true, attributeFilter: ['class'] });

    setupDrawer();

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
    // Rows hold native checkboxes (data-adm="row-check", data-id) next to the row's link;
    // the select-all box in the head is checked / indeterminate to match, and the bulk
    // bar shows while anything is selected. Selected rows are highlighted by CSS (:has).
    var table = document.querySelector('[data-adm="table"]');
    if (!table) return;

    var checkAll = table.querySelector('[data-adm="check-all"]');
    var bulk = document.querySelector('[data-adm="bulk"]');
    var bulkCount = bulk ? bulk.querySelector('[data-adm="bulk-count"]') : null;
    var bulkClear = bulk ? bulk.querySelector('[data-adm="bulk-clear"]') : null;

    function rowChecks() { return Array.prototype.slice.call(table.querySelectorAll('[data-adm="row-check"]')); }
    function selected() { return rowChecks().filter(function (c) { return c.checked; }); }

    function refresh() {
      var n = selected().length, total = rowChecks().length;
      if (checkAll) {
        checkAll.checked = total > 0 && n === total;
        checkAll.indeterminate = n > 0 && n < total;
      }
      if (bulk) {
        bulk.classList.toggle('is-open', n > 0);
        if (bulkCount) bulkCount.textContent = n;
      }
    }

    function setAll(on) {
      rowChecks().forEach(function (c) { c.checked = on; });
      refresh();
    }

    table.addEventListener('change', function (e) {
      if (e.target === checkAll) setAll(checkAll.checked);
      else if (e.target.getAttribute('data-adm') === 'row-check') refresh();
    });

    if (bulkClear) {
      bulkClear.addEventListener('click', function () {
        setAll(false);
        // The bulk bar (and the focused button with it) is hidden now.
        if (checkAll) checkAll.focus();
      });
    }

    // Collect the selected row ids (data-id) - used by page delete/rejudge AJAX.
    window.admSelectedIds = function () {
      return selected().map(function (c) { return c.getAttribute('data-id'); });
    };

    // Browsers restore checkbox state on back/forward navigation.
    refresh();
  });
})();
