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

/* String Prototype Extension - used by the table-building page scripts. */
String.prototype.format = function() {
    var newStr = this, i = 0;
    while (/%s/.test(newStr)) {
        newStr = newStr.replace("%s", arguments[i++])
    }
    return newStr;
}

/* Convert a Java-style locale (en_US, zh_CN) to a BCP 47 language tag (en-US, zh-CN). */
function toBcp47Locale(locale) {
    return (locale || 'en_US').replace('_', '-');
}

/* Absolute, localized date-time string (consumed by submissions/contests).
 * Uses the built-in Intl API, replacing the legacy Date.js / Moment.js bundles. */
function getFormatedDateString(dateTime, locale) {
    return new Intl.DateTimeFormat(toBcp47Locale(locale), {
        dateStyle: 'medium', timeStyle: 'medium'
    }).format(new Date(dateTime));
}

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
