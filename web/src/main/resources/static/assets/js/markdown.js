/*!
 * Markdown helpers for VOJ.
 *
 *   renderMarkdown(text)     -> sanitized HTML via markdown-it (display + editor preview).
 *   createMarkdownEditor(el) -> a lightweight write/preview editor wrapped around a <textarea>.
 *                               No CodeMirror, no EasyMDE. Returns an object exposing value() /
 *                               value(str). The toolbar inserts Markdown syntax; the Preview tab
 *                               renders with the same markdown-it instance, so the preview matches
 *                               the rendered output exactly.
 *
 * Loaded globally by the main and admin layouts, so both helpers are available on every page.
 */
(function(global) {
    var instance = null;

    /* Lazily build a single configured markdown-it instance.
     * html:false escapes any raw HTML in the source (XSS-safe). */
    function markdown() {
        if ( instance === null ) {
            instance = global.markdownit({ html: false, linkify: true, typographer: false });
        }
        return instance;
    }

    /* Render a Markdown string to sanitized HTML. */
    global.renderMarkdown = function(text) {
        return markdown().render(text || '');
    };

    /* Typeset TeX math inside a DOM element with KaTeX's auto-render extension.
     * No-op on pages that don't load KaTeX (renderMathInElement undefined), so it
     * is safe to call from shared code. Call it AFTER renderMarkdown has injected
     * the HTML, since auto-render scans the live DOM, not a string. */
    global.typesetMath = function(element) {
        if ( !element || typeof global.renderMathInElement !== 'function' ) { return; }
        global.renderMathInElement(element, {
            delimiters: [
                { left: '$$', right: '$$', display: true  },
                { left: '\\[', right: '\\]', display: true  },
                { left: '$',  right: '$',  display: false },
                { left: '\\(', right: '\\)', display: false }
            ],
            throwOnError: false
        });
    };

    /* Toolbar: `wrap` surrounds the selection, `line` prefixes each selected line,
     * `link` inserts a link template. */
    var TOOLBAR = [
        { label: 'B',        title: 'Bold',          wrap: ['**', '**'] },
        { label: 'I',        title: 'Italic',        wrap: ['*', '*'] },
        { label: 'H',        title: 'Heading',       line: '### ' },
        { label: '“',   title: 'Quote',         line: '> ' },
        { label: '≡',   title: 'List',          line: '- ' },
        { label: '1.',       title: 'Numbered list', line: '1. ' },
        { label: '&lt;/&gt;', title: 'Code',         wrap: ['`', '`'] },
        { label: '🔗', title: 'Link',      link: true }
    ];

    function el(tag, cls) {
        var node = document.createElement(tag);
        if ( cls ) { node.className = cls; }
        return node;
    }

    function wrapSelection(ta, before, after) {
        var s = ta.selectionStart, e = ta.selectionEnd, v = ta.value;
        ta.value = v.slice(0, s) + before + v.slice(s, e) + after + v.slice(e);
        ta.focus();
        ta.selectionStart = s + before.length;
        ta.selectionEnd = e + before.length + (e - s);
    }

    function prefixLines(ta, prefix) {
        var s = ta.selectionStart, e = ta.selectionEnd, v = ta.value;
        var lineStart = v.lastIndexOf('\n', s - 1) + 1;
        var block = v.slice(lineStart, e).replace(/^/gm, prefix);
        ta.value = v.slice(0, lineStart) + block + v.slice(e);
        ta.focus();
        ta.selectionStart = lineStart;
        ta.selectionEnd = lineStart + block.length;
    }

    function insertLink(ta) {
        var s = ta.selectionStart, e = ta.selectionEnd, v = ta.value;
        var snippet = '[' + (v.slice(s, e) || 'text') + '](url)';
        ta.value = v.slice(0, s) + snippet + v.slice(e);
        ta.focus();
        ta.selectionStart = s + snippet.length - 4; // select the 'url' placeholder
        ta.selectionEnd = ta.selectionStart + 3;
    }

    /* Attach a Markdown editor to a <textarea> (selector string or DOM node).
     * The textarea stays the source of truth, so the form/submit code reads value()
     * directly off it. Idempotent: re-attaching returns the existing instance. */
    global.createMarkdownEditor = function(element, options) {
        var ta = typeof element === 'string' ? document.querySelector(element) : element;
        if ( !ta ) { return null; }
        if ( ta.vojMarkdownEditor ) { return ta.vojMarkdownEditor; }
        options = options || {};

        var wrap    = el('div', 'vw-md'),
            bar     = el('div', 'vw-md-bar'),
            body    = el('div', 'vw-md-body'),
            preview = el('div', 'vw-md-preview markdown');
        preview.hidden = true;

        TOOLBAR.forEach(function(b) {
            var btn = el('button', 'vw-md-btn');
            btn.type = 'button';
            btn.innerHTML = b.label;
            btn.title = b.title;
            btn.addEventListener('click', function(ev) {
                ev.preventDefault();
                if ( b.wrap ) { wrapSelection(ta, b.wrap[0], b.wrap[1]); }
                else if ( b.line ) { prefixLines(ta, b.line); }
                else if ( b.link ) { insertLink(ta); }
            });
            bar.appendChild(btn);
        });
        bar.appendChild(el('span', 'vw-md-spacer'));

        var writeTab   = el('button', 'vw-md-tab active'),
            previewTab = el('button', 'vw-md-tab');
        writeTab.type = previewTab.type = 'button';
        writeTab.textContent   = options.writeLabel   || 'write';
        previewTab.textContent = options.previewLabel || 'preview';
        writeTab.addEventListener('click', function(ev) {
            ev.preventDefault();
            preview.hidden = true; ta.hidden = false;
            writeTab.classList.add('active'); previewTab.classList.remove('active');
        });
        previewTab.addEventListener('click', function(ev) {
            ev.preventDefault();
            preview.innerHTML = global.renderMarkdown(ta.value);
            global.typesetMath(preview);
            preview.hidden = false; ta.hidden = true;
            previewTab.classList.add('active'); writeTab.classList.remove('active');
        });
        bar.appendChild(writeTab);
        bar.appendChild(previewTab);

        // Insert the editor chrome around the textarea, moving the textarea inside it.
        ta.parentNode.insertBefore(wrap, ta);
        ta.classList.add('vw-md-input');
        body.appendChild(ta);
        body.appendChild(preview);
        wrap.appendChild(bar);
        wrap.appendChild(body);

        var api = {
            value: function(v) {
                if ( v === undefined ) { return ta.value; }
                ta.value = v;
                if ( !preview.hidden ) { preview.innerHTML = global.renderMarkdown(v); global.typesetMath(preview); }
                return api;
            },
            refresh: function() { return api; },
            textarea: ta
        };
        ta.vojMarkdownEditor = api;
        return api;
    };
})(window);
