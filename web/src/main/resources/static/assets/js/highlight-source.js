/* Highlights submitted source code with highlight.js, deterministically by the
 * language rather than by auto-detection. Each <code> block carries a data-mime
 * attribute holding the language slug (a CodeMirror MIME); it is mapped to the
 * matching highlight.js grammar and pinned via a language-* class before
 * highlighting. Blocks without a known data-mime fall back to auto-detection.
 *
 * Pascal maps to the delphi grammar, which is not part of the bundled common
 * languages, so pages must load hljs/delphi.min.js before calling this. */
(function (global) {
    var HLJS_BY_MIME = {
        'text/x-csrc': 'c', 'text/x-c++src': 'cpp', 'text/x-java': 'java',
        'text/x-pascal': 'delphi', 'text/x-python': 'python', 'text/x-go': 'go',
        'text/x-kotlin': 'kotlin', 'text/javascript': 'javascript', 'text/x-rustsrc': 'rust'
    };
    global.highlightSourceBlocks = function (selector) {
        $(selector).each(function (i, block) {
            var name = HLJS_BY_MIME[block.getAttribute('data-mime')];
            if ( name ) { block.classList.add('language-' + name); }
            hljs.highlightElement(block);
        });
    };
})(window);
