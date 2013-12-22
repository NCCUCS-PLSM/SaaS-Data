			jQuery(function() {
				var $elem = jQuery('#content');
				
				jQuery('#nav_up').fadeIn('slow');

				
				jQuery(window).bind('scrollstart', function(){
					jQuery('#nav_up').stop().animate({'opacity':'0.2'});
				});
				jQuery(window).bind('scrollstop', function(){
					jQuery('#nav_up').stop().animate({'opacity':'1'});
				});
				

				jQuery('#nav_up').click(
					function (e) {
						jQuery('html, body').animate({scrollTop: '0px'}, 800);
					}
				);
            });