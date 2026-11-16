/* Follows the judging of a pending submission on its pages (the submission page and the
 * administration's edit-submission page). The live log of the judging comes from the stream of
 * the submission, which the browser reconnects when it ends (e.g. it timed out) while the
 * submission is pending. Once the verdict is in, the verdict, time, memory, execute time, judge log
 * and judger are read from the database: when the stream sends the verdict, or by the poll every
 * 10 seconds, which also shows it when no stream could be opened. The stream and the poll then
 * stop.
 *
 * options:
 *   submissionId  - the unique identifier of the submission
 *   streamUrl     - the URL of its stream (getRealTimeJudgeResult.action)
 *   submissionUrl - the URL that reads it (getSubmission.action)
 *   connectedText - the line the judge log gets once the stream is open
 *   showVerdict   - function(name, slug) that shows a verdict; slug is null while the judging
 *                   runs (the stream sends the name of the verdict only)
 *   elements      - the selectors of judgeLog, usedTime, usedMemory, executeTime and judgerName;
 *                   an element the page does not have is left out */
function followJudging(options) {
    var elements = options.elements,
        poll     = setInterval(readSubmission, 10000),
        source   = new EventSource(options.streamUrl),
        isConnected = false;

    function element(name) { return $(elements[name] || []); }

    source.onopen = function() {
        if ( isConnected ) { return; }
        isConnected = true;
        element('judgeLog').append($('<p>').text(options.connectedText));
    };
    source.onmessage = function(e) {
        var message = JSON.parse(e['data']);
        options.showVerdict(message['judgeResult'], null);
        element('judgeLog').append(renderMarkdown(message['message']));
        // 'Running' is intermediate; any other verdict is the last message, so the verdict is read
        // at once instead of at the next poll.
        if ( message['judgeResult'] != 'Running' ) {
            source.close();
            readSubmission();
        }
    };

    function readSubmission() {
        $.ajax({
            type: 'GET',
            url: options.submissionUrl,
            data: { 'submissionId': options.submissionId },
            dataType: 'JSON',
            success: function(result) {
                var submission = result['submission'];
                if ( !result['isSuccessful'] || submission['judgeResult']['judgeResultSlug'] == 'PD' ) {
                    return;
                }
                clearInterval(poll);
                source.close();
                options.showVerdict(submission['judgeResult']['judgeResultName'],
                                    submission['judgeResult']['judgeResultSlug']);
                element('usedTime').text(submission['usedTime'] + ' ms');
                element('usedMemory').text(submission['usedMemory'] + ' KB');
                element('executeTime').text(formatDateTime(submission['executeTime'], 'datetime'));
                element('judgeLog').html(renderMarkdown(submission['judgeLog']));
                element('judgerName').text(submission['judger'] ? submission['judger']['username'] : '—');
            }
        });
    }
}
