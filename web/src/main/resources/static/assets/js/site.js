/*!
 * Global JavaScript by @zjhzxhz
 *
 * Copyright 2015 Contributors
 * Released under the GPL v3 license
 * http://opensource.org/licenses/GPL-3.0
 */
/* Attach the Spring Security CSRF token to every state-changing AJAX request.
 * The token and header name are rendered into <meta> tags by layouts/main.html. */
$(function() {
    var csrfToken  = $('meta[name="_csrf"]').attr('content'),
        csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    $(document).ajaxSend(function(event, jqXHR, settings) {
        var method = (settings.type || 'GET').toUpperCase();
        if ( csrfToken && csrfHeader && !settings.crossDomain &&
             method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS' && method !== 'TRACE' ) {
            jqXHR.setRequestHeader(csrfHeader, csrfToken);
        }
    });
});

/* Cloudflare Turnstile widgets that follow the site theme. The pages load api.js with
 * render=explicit&onload=onTurnstileLoad; a rendered widget cannot be restyled, so it is
 * removed and rendered again (which discards a solved challenge) when the theme is toggled. */
function renderTurnstileWidgets() {
    var theme = document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
    $('.cf-turnstile').each(function() {
        var widgetId = $(this).data('widgetId');
        if ( widgetId !== undefined ) {
            turnstile.remove(widgetId);
        }
        $(this).data('widgetId', turnstile.render(this, {
            sitekey: this.getAttribute('data-sitekey'),
            theme: theme
        }));
    });
}
window.onTurnstileLoad = renderTurnstileWidgets;
document.addEventListener('voj:themechange', function() {
    if ( window.turnstile ) { renderTurnstileWidgets(); }
});

/* String Prototype Extension - used by the table-building page scripts.
 * Replaces each %s placeholder, in order, with the next argument in a single pass, so
 * an argument that itself contains "%s" is inserted verbatim rather than re-substituted.
 * The arguments are inserted as-is: escape user text with escapeHtml() first. */
String.prototype.format = function() {
    var args = arguments, i = 0;
    return String(this).replace(/%s/g, function() { return String(args[i++]); });
};

/* Convert a Java-style locale (en_US, zh_CN) to a BCP 47 language tag (en-US, zh-CN). */
function toBcp47Locale(locale) {
    return (locale || 'en_US').replace('_', '-');
}

/* Dates and times are shown in the viewer's time zone, which only the browser knows. The
 * server renders each timestamp as
 *     <span data-time="<epoch ms>" data-format="datetime">fallback, in the server's zone</span>
 * and localizeDateTimes() rewrites the text on load; scripts that build content format the
 * timestamps of the JSON responses (epoch ms) with formatDateTime(), so a page and the rows
 * or live updates added to it later agree. The format names map to Intl options below, plus
 * 'relative' for "x ago". The locale defaults to the page language (<html lang>). */
var DATE_TIME_FORMATS = {
    'datetime':       { dateStyle: 'long',   timeStyle: 'medium' },
    'datetime-short': { dateStyle: 'medium', timeStyle: 'short' },
    'date':           { dateStyle: 'medium' },
    'date-short':     { dateStyle: 'short' },
    'month':          { year: 'numeric', month: 'short' }
};
function formatDateTime(dateTime, format, locale) {
    locale = locale || document.documentElement.lang;
    if ( format === 'relative' ) {
        return getTimeElapsed(dateTime, locale);
    }
    var date = (dateTime == null || dateTime === '') ? new Date(NaN) : new Date(dateTime);
    if ( isNaN(date.getTime()) ) {
        return '';
    }
    return new Intl.DateTimeFormat(toBcp47Locale(locale), DATE_TIME_FORMATS[format]).format(date);
}
function localizeDateTimes(root) {
    $('[data-time][data-format]', root || document).each(function() {
        var text = formatDateTime(Number(this.getAttribute('data-time')), this.getAttribute('data-format'));
        if ( text ) { this.textContent = text; }
    });
}
$(function() { localizeDateTimes(); });

/* Relative "x ago" string (consumed by the discussion threads).
 * Uses the built-in Intl.RelativeTimeFormat, replacing moment().fromNow(). */
function getTimeElapsed(dateTime, locale) {
    // Guard against missing / unparseable timestamps: new Date('') is an Invalid Date and
    // Intl.RelativeTimeFormat.format(NaN, ...) throws a RangeError, which would otherwise
    // abort the caller (e.g. the discussion thread's initial reply load). Degrade to ''.
    var time = (dateTime == null || dateTime === '') ? NaN : new Date(dateTime).getTime();
    if ( isNaN(time) ) {
        return '';
    }
    var rtf     = new Intl.RelativeTimeFormat(toBcp47Locale(locale), { numeric: 'auto' }),
        seconds = Math.round((time - Date.now()) / 1000),
        units   = [
            ['year',   31536000],
            ['month',   2592000],
            ['week',     604800],
            ['day',       86400],
            ['hour',       3600],
            ['minute',       60],
            ['second',        1]
        ];
    for ( var i = 0; i < units.length; ++ i ) {
        if ( Math.abs(seconds) >= units[i][1] || units[i][0] === 'second' ) {
            return rtf.format(Math.round(seconds / units[i][1]), units[i][0]);
        }
    }
}

/* "Load more" pagination shared by the list pages. The container holds
 *     <p class="availble"><button type="button" class="vw-loadmore-btn">More...</button></p>
 *     <span class="vw-spinner hide" role="status" aria-label="Loading"></span>
 * and options.load(done) fetches the next page, renders it and calls
 * done(newElements, hasMore). While loading, the spinner replaces the button; once the
 * list is exhausted the paragraph shows options.doneText instead. Hiding the button
 * drops keyboard focus, so when it had focus, focus moves to the first newly loaded item
 * (or to the "no more" note). Returns { load: fn } to load a page without a click. */
function createLoadMore(container, options) {
    var $box     = $(container),
        $p       = $('p', $box),
        $spinner = $('.vw-spinner', $box),
        loading  = false;

    function setLoading(isLoading) {
        loading = isLoading;
        $p.toggleClass('hide', isLoading);
        $spinner.toggleClass('hide', !isLoading);
    }
    function focusFirst(elements) {
        var first = $(elements).first();
        if ( first.length === 0 ) { return false; }
        if ( !first.is('a[href], button, input, select, textarea, [tabindex]') ) {
            first.attr('tabindex', '-1');
        }
        first[0].focus();
        return true;
    }
    function load() {
        if ( loading || !$p.hasClass('availble') ) { return; }
        var hadFocus = $box[0].contains(document.activeElement);
        setLoading(true);
        options.load(function(newElements, hasMore) {
            if ( !hasMore ) {
                $p.removeClass('availble').attr('tabindex', '-1').text(options.doneText);
                $box.addClass('is-done');
            }
            setLoading(false);
            if ( hadFocus && !focusFirst(newElements || []) ) {
                ( hasMore ? $('button', $p) : $p )[0].focus();
            }
        });
    }
    $box.on('click', '.vw-loadmore-btn', load);
    return { load: load };
}

/* Live GitHub star count, shared across pages and cached in the browser for an
 * hour. Every .js-github-stars element is filled in; a fresh cache is used
 * first, so revisits (e.g. home -> about) never re-hit the API. The element
 * keeps its spinner until the value is available. */
$(function() {
    var $targets = $('.js-github-stars');
    if ( $targets.length === 0 ) { return; }

    var repo     = 'hzxie/voj',
        cacheKey = 'voj.githubStars',
        ttl      = 60 * 60 * 1000;

    var render = function(stars) { $targets.text(Number(stars).toLocaleString()); };

    var cached = null;
    try { cached = JSON.parse(localStorage.getItem(cacheKey)); } catch (e) { /* ignore */ }

    if ( cached && $.isNumeric(cached.stars) && (Date.now() - cached.time) < ttl ) {
        render(cached.stars);
        return;
    }

    $.getJSON('https://api.github.com/repos/' + repo).done(function(data) {
        render(data.stargazers_count);
        try {
            localStorage.setItem(cacheKey, JSON.stringify({ stars: data.stargazers_count, time: Date.now() }));
        } catch (e) { /* ignore */ }
    }).fail(function() {
        render(cached && $.isNumeric(cached.stars) ? cached.stars : $targets.first().data('fallback'));
    });
});
