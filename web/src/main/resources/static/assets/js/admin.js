/*!
 * Verwandlung Online Judge - administration chrome behaviour (vanilla JS).
 * Navigation drawer/sidebar + switches / segmented controls (class + ARIA state)
 * + checkbox data-table selection / bulk bar.
 * The theme toggle reuses [data-vw="theme-toggle"], handled by theme.js.
 */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') { fn(); }
    else { document.addEventListener('DOMContentLoaded', fn); }
  }

  // --- Switches and segmented controls ---------------------------------------
  // .adm-switch (<button role="switch">) shows its state with an `.on` class and
  // .adm-seg options (buttons) mark the chosen one with `.active`. The class is the
  // look and aria-checked / aria-pressed the semantics, so they are always set
  // together through admSetSwitch / admSetSegment (page scripts use them too).
  function forEach(list, fn) { Array.prototype.forEach.call(list, fn); }
  function textOf(el) { return el ? el.textContent.replace(/\s+/g, ' ').trim() : ''; }

  function setSwitch(sw, on) {
    sw.classList.toggle('on', on);
    sw.setAttribute('aria-checked', on ? 'true' : 'false');
  }

  function setSegment(seg, chosen) {
    forEach(seg.querySelectorAll(':scope > button'), function (o) {
      o.classList.toggle('active', o === chosen);
      o.setAttribute('aria-pressed', o === chosen ? 'true' : 'false');
    });
  }

  // Name a switch after the text of its row, unless the markup names it already.
  function labelSwitch(sw) {
    if (sw.hasAttribute('aria-label') || sw.hasAttribute('aria-labelledby')) return;
    var row = sw.closest('.adm-setrow, .adm-togrow, .vw-between, .language');
    var lbl = row && row.querySelector('.lbl, .language-name, span:not(.knob)');
    var text = lbl ? (lbl.tagName === 'INPUT' ? lbl.value : textOf(lbl)) : '';
    if (text) sw.setAttribute('aria-label', text);
  }

  // Initial ARIA state for the server-rendered controls (and for controls a page adds
  // later: it calls admEnhance on the new markup).
  function enhance(scope) {
    forEach(scope.querySelectorAll('.adm-switch'), function (sw) {
      labelSwitch(sw);
      setSwitch(sw, sw.classList.contains('on'));
    });
    forEach(scope.querySelectorAll('.adm-seg > button'), function (o) {
      o.setAttribute('aria-pressed', o.classList.contains('active') ? 'true' : 'false');
    });
  }

  // --- Focus after removing a list item ---------------------------------------
  // Removing an item (or re-rendering its list) destroys the focused delete button.
  // Keep keyboard focus in the list: `controls` are the list's delete buttons after the
  // change (a NodeList or jQuery set) and `index` the position of the one that was
  // clicked, so the button now at that position is the next item's, or the last one's
  // when the removed item was last; with none left, focus `fallback` (the add control).
  function focusAt(controls, index, fallback) {
    var target = controls[Math.min(index, controls.length - 1)] ||
                 (fallback && fallback.jquery ? fallback[0] : fallback);
    if (target) target.focus();
  }

  window.admSetSwitch = setSwitch;
  window.admSetSegment = setSegment;
  window.admEnhance = enhance;
  window.admFocusAt = focusAt;

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
    // The wide-mode query of css/admin.css; narrow is everything else.
    var wide = window.matchMedia('(min-width:861px)');

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
    wide.addEventListener('change', function () {
      if (wide.matches && drawer.open) drawer.close();
    });
  }

  ready(function () {
    enhance(document);

    setupDrawer();

    // --- Toggle switches (settings / editors) --------------------------------
    // Any [data-adm="switch"] flips its `.on` class on click; pages read the
    // `.on` state (or the optional hidden input named by data-input) on save.
    Array.prototype.forEach.call(document.querySelectorAll('[data-adm="switch"]'), function (sw) {
      sw.addEventListener('click', function () {
        var on = !sw.classList.contains('on');
        setSwitch(sw, on);
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
          setSegment(seg, opt);
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
