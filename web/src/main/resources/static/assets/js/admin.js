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

  // --- Navigation drawer that is also the sidebar ----------------------------
  // Wide (>860px): the element is a plain sticky sidebar (no role). Narrow: it is a
  // popover="manual" drawer exposed as role="dialog". Its scroller holds the sheet
  // plus an empty "closed" snap stop, so opening/closing is scrolling (swipe works
  // natively) and an IntersectionObserver on the sheet is the single source of
  // truth for "fully closed". A popover is not modal, so this code supplies the
  // modality: the main column is inert and aria-modal is set while open, Esc and a
  // tap beside the sheet close it, and focus returns to the burger afterwards.
  function setupDrawer() {
    var drawer = document.querySelector('[data-adm="sidebar"]');
    var burger = document.querySelector('[data-adm="burger"]');
    if (!drawer || !burger || typeof drawer.showPopover !== 'function') return;
    var scroller = drawer.querySelector('[data-adm="drawer-scroller"]');
    var sheet = drawer.querySelector('[data-adm="drawer-sheet"]');
    var closeBtn = drawer.querySelector('[data-adm="drawer-close"]');
    var main = document.querySelector('.adm-main');
    var narrow = window.matchMedia('(max-width: 860px)');
    var opening = false;

    function isOpen() { return drawer.hasAttribute('aria-modal'); }
    function nextFrames() {
      return new Promise(function (r) { requestAnimationFrame(function () { requestAnimationFrame(r); }); });
    }

    function openDrawer() {
      if (!narrow.matches || isOpen()) return;
      opening = true;
      drawer.showPopover();
      if (main) main.inert = true;
      drawer.setAttribute('aria-modal', 'true');
      var ready = Promise.resolve();
      if (!(window.CSS && CSS.supports('scroll-initial-target', 'nearest'))) {
        // Start from the closed stop so the scroll below slides the sheet in.
        scroller.scrollTo({ left: scroller.scrollWidth, behavior: 'instant' });
        ready = nextFrames();
      }
      // `opening` only shields the jump to the closed stop; once the slide-in starts,
      // a swipe or tap that closes the drawer again must be honoured.
      ready.then(function () {
        opening = false;
        scroller.scrollTo({ left: 0, behavior: 'auto' });
      });
    }

    // Slide out; the observer closes the popover once the sheet has left.
    function closeDrawer() {
      if (!isOpen()) return;
      opening = false;
      scroller.scrollTo({ left: scroller.scrollWidth, behavior: 'auto' });
    }

    function onDrawerClosed(restoreFocus) {
      if (!isOpen()) return;
      opening = false;
      drawer.hidePopover();
      if (main) main.inert = false;
      drawer.removeAttribute('aria-modal');
      burger.setAttribute('aria-expanded', 'false');
      if (restoreFocus) burger.focus();
    }

    var visible = 1 / window.innerWidth;
    new IntersectionObserver(function (entries) {
      var entry = entries[entries.length - 1];
      if (!narrow.matches || !isOpen()) return;
      if (entry.intersectionRatio === 1) {
        burger.setAttribute('aria-expanded', 'true');
      } else if (entry.intersectionRatio < visible && !opening) {
        onDrawerClosed(true);
      }
    }, { root: drawer, threshold: [visible, 1] }).observe(sheet);

    // Backdrop fade where scroll-driven animations are unsupported (CSS gates its own).
    if (!(window.CSS && CSS.supports('animation-timeline: scroll()'))) {
      scroller.addEventListener('scroll', function () {
        var ratio = 1 - scroller.scrollLeft / sheet.offsetWidth;
        drawer.style.setProperty('--drawer-backdrop', Math.max(0, Math.min(1, ratio)));
      });
    }

    // Wide: plain sidebar; narrow: dialog. Leaving narrow mode closes an open drawer.
    function syncMode() {
      if (narrow.matches) {
        drawer.setAttribute('role', 'dialog');
        drawer.setAttribute('aria-label', drawer.getAttribute('data-drawer-label') || '');
      } else {
        onDrawerClosed(false);
        drawer.removeAttribute('role');
        drawer.removeAttribute('aria-label');
      }
    }

    burger.addEventListener('click', openDrawer);
    if (closeBtn) closeBtn.addEventListener('click', closeDrawer);
    drawer.addEventListener('click', function (e) {
      if (isOpen() && !sheet.contains(e.target)) closeDrawer();
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && isOpen()) closeDrawer();
    });
    if (narrow.addEventListener) narrow.addEventListener('change', syncMode);
    syncMode();
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
