/*!
 * Activity heat-map for VOJ (GitHub-style contribution calendar).
 *
 * Zero-dependency renderer that draws a 53-week x 7-day grid into a container.
 * Replaces the legacy Highcharts submission line-chart.
 *
 *   VojHeatmap.render('#submissions-calendar', { '2025/06/27': 3, ... }, {
 *       totalElement: '#heatmap-total', totalLabel: 'submissions in the last year'
 *   });
 *
 * `data` maps a 'yyyy/MM/dd' date (the format produced by the *.action endpoints)
 * to that day's submission count. Cells are graded into four intensity levels;
 * the colours live in app.css as CSS custom properties, so the dark/light theme
 * toggle is handled entirely by CSS.
 */
(function(global) {
    var WEEKS  = 53,
        DAYS   = 7,
        MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    /* Map a day's submission count to a 0..3 intensity level. */
    function levelOf(count) {
        if ( !count )     return 0;
        if ( count <= 2 ) return 1;
        if ( count <= 5 ) return 2;
        return 3;
    }

    /* Zero-padded 'yyyy/MM/dd' key, matching the server's date format. */
    function dateKey(date) {
        var m = date.getMonth() + 1, d = date.getDate();
        return date.getFullYear() + '/' + (m < 10 ? '0' + m : m) + '/' + (d < 10 ? '0' + d : d);
    }

    function el(tag, className) {
        var node = document.createElement(tag);
        if ( className ) { node.className = className; }
        return node;
    }

    function buildLegend(options) {
        var legend = el('div', 'vw-heatmap-legend');
        legend.appendChild(document.createTextNode((options.lessLabel || 'less') + ' '));
        for ( var l = 0; l < 4; ++ l ) {
            legend.appendChild(el('span', 'vw-hm-cell vw-hm-l' + l));
        }
        legend.appendChild(document.createTextNode(' ' + (options.moreLabel || 'more')));
        return legend;
    }

    global.VojHeatmap = {
        render: function(target, data, options) {
            var container = typeof target === 'string' ? document.querySelector(target) : target;
            if ( !container ) { return; }
            data = data || {};
            options = options || {};

            // Sunday that starts the left-most column: this week's Sunday minus 52 weeks.
            var today = new Date();
            today.setHours(0, 0, 0, 0);
            var start = new Date(today);
            start.setDate(today.getDate() - today.getDay() - (WEEKS - 1) * DAYS);

            var monthsRow = el('div', 'vw-heatmap-months'),
                grid      = el('div', 'vw-heatmap-grid'),
                total     = 0,
                prevMonth = -1,
                w, i;

            // Month labels: name a column the first week its month appears.
            for ( w = 0; w < WEEKS; ++ w ) {
                var firstOfWeek = new Date(start);
                firstOfWeek.setDate(start.getDate() + w * DAYS);
                var month = firstOfWeek.getMonth(),
                    label = el('div');
                if ( month !== prevMonth && firstOfWeek.getDate() <= DAYS ) {
                    label.textContent = MONTHS[month];
                    prevMonth = month;
                }
                monthsRow.appendChild(label);
            }

            // Cells. grid-auto-flow:column fills top-to-bottom then rightwards, so
            // iterating week-major (w*7 + d) matches the visual order.
            for ( i = 0; i < WEEKS * DAYS; ++ i ) {
                var cellDate = new Date(start);
                cellDate.setDate(start.getDate() + i);
                var cell = el('div', 'vw-hm-cell');
                if ( cellDate > today ) {
                    cell.className += ' vw-hm-empty'; // future day: invisible placeholder
                } else {
                    var key = dateKey(cellDate), count = data[key] || 0;
                    total += count;
                    cell.className += ' vw-hm-l' + levelOf(count);
                    cell.title = key + ' · ' + count;
                }
                grid.appendChild(cell);
            }

            var inner = el('div', 'vw-heatmap-inner');
            inner.appendChild(monthsRow);
            inner.appendChild(grid);
            var scroll = el('div', 'vw-heatmap-scroll');
            scroll.appendChild(inner);

            container.innerHTML = '';
            container.appendChild(scroll);
            container.appendChild(buildLegend(options));

            if ( options.totalElement ) {
                var totalNode = document.querySelector(options.totalElement);
                if ( totalNode ) {
                    totalNode.textContent = total + (options.totalLabel ? ' ' + options.totalLabel : '');
                }
            }
        }
    };
})(window);
