    <script type="text/javascript">
        String.prototype.format = function() {
            var newStr = this, i = 0;
            while (/%s/.test(newStr)) {
                newStr = newStr.replace("%s", arguments[i++])
            }
            return newStr;
        }
    </script>
    <script type="text/javascript">
        $('#sidebar-toggle').click(function() {
            var isSidebarShown = $('#sidebar').is(':visible');

            if ( isSidebarShown ) {
                $('#sidebar').css('display', 'none');
                $('#header').css('padding-left', 0);
                $('#header').css('padding-right', 0);
                $('#content').css('margin-left', 0);
                $('#content').css('margin-right', 0);
            } else {
                $('#sidebar').css('display', 'block');
                $('#header').css('padding-left', 250);
                $('#header').css('padding-right', -250);
                $('#content').css('margin-left', 250);
                $('#content').css('margin-right', -250);
            }
        });
    </script>
    <script type="text/javascript">
        $('li.nav-item-has-children > a').click(function() {
            var navItem       = $(this).parent(),
                isSubNavShown = $('ul.sub-nav', navItem).is(':visible');

            if ( isSubNavShown ) {
                $('i.fa-caret-down', navItem).addClass('fa-caret-right');
                $('i.fa-caret-down', navItem).removeClass('fa-caret-down');
                $('ul.sub-nav', navItem).slideUp(120);
            } else {
                $('i.fa-caret-right', navItem).addClass('fa-caret-down');
                $('i.fa-caret-right', navItem).removeClass('fa-caret-right');
                $('ul.sub-nav', navItem).slideDown(120);
            }
        });
    </script>
    <script type="text/javascript">
        $(function() {
            var currentLocation     = window.location.pathname,
                mostSimilarItem     = null,
                mostSimilarLength   = 0;

            $('.nav-item > a', '#sidebar-nav').each(function() {
                var navHref = $(this).attr('href');

                if ( currentLocation.indexOf(navHref) != -1 ) {
                    if ( navHref.length > mostSimilarLength ) {
                        mostSimilarItem   = $(this);
                        mostSimilarLength = navHref.length;
                    }
                }
            });
            $(mostSimilarItem).parent().addClass('active');

            $('li.primary-nav-item', '#sidebar-nav').each(function() {
                if ( $('li.secondary-nav-item.active', $(this)).length != 0 ) {
                    $(this).addClass('active');

                    $('i.fa-caret-right', $(this)).addClass('fa-caret-down');
                    $('i.fa-caret-right', $(this)).removeClass('fa-caret-right');
                    $('ul.sub-nav', $(this)).slideDown(120);
                }
            });
        });
    </script>