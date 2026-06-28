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

  ready(function () {
    // --- Sidebar drawer (mobile) ---------------------------------------------
    var sidebar = document.querySelector('[data-adm="sidebar"]');
    var scrim = document.querySelector('[data-adm="scrim"]');
    var burger = document.querySelector('[data-adm="burger"]');
    function openSb() { if (sidebar) sidebar.classList.add('open'); if (scrim) scrim.classList.add('open'); }
    function closeSb() { if (sidebar) sidebar.classList.remove('open'); if (scrim) scrim.classList.remove('open'); }
    if (burger) burger.addEventListener('click', openSb);
    if (scrim) scrim.addEventListener('click', closeSb);

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
