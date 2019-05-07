/*!
 * Global JavaScript by @zjhzxhz
 *
 * Copyright 2015 Contributors
 * Released under the GPL v3 license
 * http://opensource.org/licenses/GPL-3.0
 */
/* String Protorype Extension */
String.prototype.format = function() {
    var newStr = this, i = 0;
    while (/%s/.test(newStr)) {
        newStr = newStr.replace("%s", arguments[i++])
    }
    return newStr;
}

/* Format DateTime for different locales */
function getFormatedDateString(dateTime, locale) {
    var dateObject = new Date(dateTime),
        dateString = dateObject.toString();

    if ( locale == 'en_US' ) {
        dateString = dateObject.toString('MMM d, yyyy h:mm:ss tt');
    } else if ( locale == 'zh_CN' ) {
        dateString = dateObject.toString('yyyy年M月d日 HH:mm:ss');
    }
    return dateString;
}

/* JavaScript for DrawerMenu */
function openDrawerMenu() {
    $('#drawer-nav').animate({
        right: 0
    }, 100);
}

function closeDrawerMenu() {
    $('#drawer-nav').animate({
        right: -320
    }, 100);
}

/* Display the avatar of the user in DrawerMenu */
$(function() {
    var imageObject       = $('img', '#drawer-nav #profile'),
        email             = $('p.email', '#drawer-nav #profile').html();
    
    if ( typeof(email) == 'undefined' ) {
        return;
    }
    $.ajax({
        type: 'GET',
        url: 'https://secure.gravatar.com/' + md5(email) + '.json',
        dataType: 'jsonp',
        success: function(result){
            if ( result != null ) {
                var imageUrl    = result['entry'][0]['thumbnailUrl'],
                    requrestUrl = imageUrl + '?s=200';
                $(imageObject).attr('src', requrestUrl);
            }
        }
    });
});

/* Display Current Language Name */
$(function() {
    var languageMapper      = {
        'en_US': 'English',
        'zh_CN': '简体中文'
    };
    var currentLanguageCode = $('#current-language').html(),
        currentLanguageName = languageMapper[currentLanguageCode];

    $('#current-language').html(currentLanguageName);
});