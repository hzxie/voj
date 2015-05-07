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
        email             = $('p.email', '#drawer-nav #profile').html(),
        hashCode          = md5(email),
        gravatarSeriveUrl = 'https://secure.gravatar.com/';
    
    $.ajax({
        type: 'GET',
        url: gravatarSeriveUrl + hashCode + '.json',
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