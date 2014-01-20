// log page error to server
$(window).load(function () {
    window.onerror=function(msg, url, linenumber ){
    	var postData,browserVersion='';
    	$.each($.browser, function(i, val) {
    	      browserVersion += i + " : " + val + " ";
    	    });
    	postData={};
    	postData.browser=browserVersion;
    	postData.url=url;
    	postData.error=msg;
    	postData.lineNumber=linenumber;
    	
    	$.post("/webpage/logError",postData);
    };
});