var lastTouchedElement;
jQuery('html').live('click', function(event) {
  lastTouchedElement = event.target;
});

function doNotTrackClicks() {
  return navigator.userAgent.match(/iPhone|iPad/i);
}
jQuery('navbar.dropdown > ul > li').live('click', function(event) {
  if (!(doNotTrackClicks() || lastTouchedElement == event.target)) {
    event.preventDefault();
  }
  lastTouchedElement = event.target;
});

jQuery('navbar.dropdown li').live('click', function(event) {
  event.stopPropagation();
});
