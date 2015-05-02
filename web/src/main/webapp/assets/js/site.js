/*!
 * Global JavaScript by @zjhzxhz
 * http://www.testzilla.org/
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
