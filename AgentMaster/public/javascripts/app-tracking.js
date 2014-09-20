 var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-33180546-1']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

function trackFunc() {
_rover.setAppId(1355);
// Page Impression
var pageImpEvent=2050847;
var impEvt=_rover.createPageImpEvent(pageImpEvent);
impEvt.setLVTrk(true);
ebayLVTr.setRover(_rover);
ebayLVTr.setPageImpEvent(pageImpEvent);
ebayLVTrClk._ebayLVTrackerClk_init_tracker();
_rover.track();
}

// on body load execute the analytics function
$(window).load(function () {
	trackFunc();
});