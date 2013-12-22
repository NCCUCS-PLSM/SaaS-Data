(function(jQuery){
	
	//plugin's default options
	var settings = {
		combine: true,					//combine multiple menus into a single select
		groupPageText: 'Main',			//optgroup's aren't selectable, make an option for it
		nested: true,					//create optgroups by default
		prependTo: 'body',				//insert at top of page by default
		switchWidth: 480,				//width at which to switch to select, and back again
		topOptionText: 'Select a page'	//default "unselected" state
	},
	
	//used to store original matched menus
	jQuerymenus,
	
	//used as a unique index for each menu if no ID exists
	menuCount = 0,
	
	//used to store unique list items for combining lists
	uniqueLinks = [];


	//go to page
	function goTo(url){
		document.location.href = url;
	}
	
	//does menu exist?
	function menuExists(){
		return (jQuery('.mnav').length) ? true : false;
	}

	//validate selector's matched list(s)
	function isList(jQuerythis){
		var pass = true;
		jQuerythis.each(function(){
			if(!jQuery(this).is('ul') && !jQuery(this).is('ol')){
				pass=false;
			}
		});
		return pass;
	}//isList()


	//function to decide if mobile or not
	function isMobile(){
		return (jQuery(window).width() < settings.switchWidth);
	}
	
	
	//function to get text value of element, but not it's children
	function getText(jQueryitem){
		return jQuery.trim(jQueryitem.clone().children('ul, ol').remove().end().text());
	}
	
	//function to check if URL is unique
	function isUrlUnique(url){
		return (jQuery.inArray(url, uniqueLinks) === -1) ? true : false;
	}
	
	
	//function to do duplicate checking for combined list
	function checkForDuplicates(jQuerymenu){
		
		jQuerymenu.find(' > li').each(function(){
		
			var jQueryli = jQuery(this),
				link = jQueryli.find('a').attr('href'),
				parentLink = function(){
					if(jQueryli.parent().parent().is('li')){
						return jQueryli.parent().parent().find('a').attr('href');
					} else {
						return null;
					}
				};
						
			//check nested <li>s before checking current one
			if(jQueryli.find(' ul, ol').length){
				checkForDuplicates(jQueryli.find('> ul, > ol'));
			}
		
			//remove empty UL's if any are left by LI removals
			if(!jQueryli.find(' > ul li, > ol li').length){
				jQueryli.find('ul, ol').remove();
			}
		
			//if parent <li> has a link, and it's not unique, append current <li> to the "unique parent" detected earlier
			if(!isUrlUnique(parentLink(), uniqueLinks) && isUrlUnique(link, uniqueLinks)){
				jQueryli.appendTo(
					jQuerymenu.closest('ul#mmnav').find('li:has(a[href='+parentLink()+']):first ul')
				);
			}
			
			//otherwise, check if the current <li> is unique, if it is, add it to the unique list
			else if(isUrlUnique(link)){
				uniqueLinks.push(link);
			}
			
			//if it isn't, remove it. Simples.
			else{
				jQueryli.remove();
			}
		
		});
	}
	
	
	//function to combine lists into one
	function combineLists(){
		
		//create a new list
		var jQuerymenu = jQuery('<ul id="mmnav" />');
		
		//loop through each menu and extract the list's child items
		//then append them to the new list
		jQuerymenus.each(function(){
			jQuery(this).children().clone().appendTo(jQuerymenu);
		});
		
		//de-duplicate any repeated items
		checkForDuplicates(jQuerymenu);
				
		//return new combined list
		return jQuerymenu;
		
	}//combineLists()
	
	
	
	//function to create options in the select menu
	function createOption(jQueryitem, jQuerycontainer, text){
		
		//if no text param is passed, use list item's text, otherwise use settings.groupPageText
		if(!text){
			jQuery('<option value="'+jQueryitem.find('a:first').attr('href')+'">'+jQuery.trim(getText(jQueryitem))+'</option>').appendTo(jQuerycontainer);
		} else {
			jQuery('<option value="'+jQueryitem.find('a:first').attr('href')+'">'+text+'</option>').appendTo(jQuerycontainer);
		}
	
	}//createOption()
	
	
	
	//function to create option groups
	function createOptionGroup(jQuerygroup, jQuerycontainer){
		
		//create <optgroup> for sub-nav items
		var jQueryoptgroup = jQuery('<optgroup label="'+jQuery.trim(getText(jQuerygroup))+'" />');
		
		//append top option to it (current list item's text)
		createOption(jQuerygroup,jQueryoptgroup, settings.groupPageText);
	
		//loop through each sub-nav list
		jQuerygroup.children('ul, ol').each(function(){
		
			//loop through each list item and create an <option> for it
			jQuery(this).children('li').each(function(){
				createOption(jQuery(this), jQueryoptgroup);
			});
		});
		
		//append to select element
		jQueryoptgroup.appendTo(jQuerycontainer);
		
	}//createOptionGroup()

	
	
	//function to create <select> menu
	function createSelect(jQuerymenu){
	
		//create <select> to insert into the page
		var jQueryselect = jQuery('<select id="mm'+menuCount+'" class="mnav" />');
		menuCount++;
		
		//create default option if the text is set (set to null for no option)
		if(settings.topOptionText){
			createOption(jQuery('<li>'+settings.topOptionText+'</li>'), jQueryselect);
		}
		
		//loop through first list items
		jQuerymenu.children('li').each(function(){
		
			var jQueryli = jQuery(this);

			//if nested select is wanted, and has sub-nav, add optgroup element with child options
			if(jQueryli.children('ul, ol').length && settings.nested){
				createOptionGroup(jQueryli, jQueryselect);
			}
			
			//otherwise it's a single level select menu, so build option
			else {
				createOption(jQueryli, jQueryselect);			
			}
						
		});
		
		//add change event and prepend menu to set element
		jQueryselect
			.change(function(){goTo(jQuery(this).val());})
			.prependTo(settings.prependTo);
	
	}//createSelect()

	
	//function to run plugin functionality
	function runPlugin(){
	
		//menu doesn't exist
		if(isMobile() && !menuExists()){
			
			//if user wants to combine menus, create a single <select>
			if(settings.combine){
				var jQuerymenu = combineLists();
				createSelect(jQuerymenu);
			}
			
			//otherwise, create a select for each matched list
			else{
				jQuerymenus.each(function(){
					createSelect(jQuery(this));
				});
			}
		}
		
		//menu exists, and browser is mobile width
		if(isMobile() && menuExists()){
			jQuery('.mnav').show();
			jQuerymenus.hide();
		}
			
		//otherwise, hide the mobile menu
		if(!isMobile() && menuExists()){
			jQuery('.mnav').hide();
			jQuerymenus.show();
		}
		
	}//runPlugin()

	
	
	//plugin definition
	jQuery.fn.mobileMenu = function(options){

		//override the default settings if user provides some
		if(options){jQuery.extend(settings, options);}
		
		//check if user has run the plugin against list element(s)
		if(isList(jQuery(this))){
			jQuerymenus = jQuery(this);
			runPlugin();
			jQuery(window).resize(function(){runPlugin();});
		} else {
			alert('mobileMenu only works with <ul>/<ol>');
		}
				
	};//mobileMenu()
	
})(jQuery);
