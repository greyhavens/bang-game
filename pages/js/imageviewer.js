// ------------------------------------------------------------------
// Figure out which subpage we are on
var query = window.location.search.substring(1);
var vars = query.split("&");
for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split(",");
}
var queryCurrentPage = pair[0];
var queryTotalPages = pair[1];
	
if (!queryCurrentPage) {
    queryCurrentPage = 1;
    queryTotalPages = 1;
}

// ------------------------------------------------------------------
// Layout the Nav	
function printThumbNav() {	
    var sPath = window.location.pathname;
    var sPage = sPath.substring(sPath.lastIndexOf('/') + 1);
        
    if (queryCurrentPage == 1) {
        var prevPage = 1;
    } else {
        var prevPage = parseInt(queryCurrentPage) - 1;
    }
    
    if (queryCurrentPage == pages) {
        var nextPage = pages;
    } else {
        var nextPage = parseInt(queryCurrentPage) + 1;
    }
    document.write ('<table width="700px" border="0"><tr><td>');
    // If you are already on the first page, you don't need a back button
    if (currentPage == "1") {
        document.write('<img src="/images/screenshots/backx.gif">');
    } else {
        document.write('<a href="?' + prevPage + ',' + pages + '"><img src="/images/screenshots/back.gif"></a>');    
    }
    document.write ('</td><td align="center">');
    if (pages != 1) {
        for (x = 1; x<=pages; x++) {
            document.write("<a href =" + sPage +"?"+ x +","+ pages +">");
    
            if (x == currentPage) {
                document.write("<font color = #330000><b>");
            }
            document.write("Page " + x);
            if (x == currentPage) {
                document.write("</b></font>");
            }
            document.write("</a>");
            if (x != pages) {
                document.write(" | ");
            }
        }
    }
    document.write ('</td><td align="right">');
    // If you are already on the last page, you don't need a next button
    if (queryCurrentPage == pages) {
        document.write('<img src="/images/screenshots/nextx.gif">');
    } else {
        document.write('<a href="?' + nextPage + ',' + pages + '"><img src="/images/screenshots/next.gif"></a>');
    }
    document.write ('</td></tr></table>');
}

// ------------------------------------------------------------------
// Layout the Gallery
function buildGallery() {
//Figure out the first img for the page
    var theStart = screenshotsTotal - ((currentPage - 1) * screenshotsPerPage);	
	
	//Check if you have enough pics to fill the full grid
	var theEnd = 0;
	if (theStart - screenshotsPerPage > 0) {
	    theEnd = theStart - screenshotsPerPage;
	}
	
	//Build the table of thumbs
	var whichColumn = 1;
	while (theStart > theEnd) {
		if (whichColumn == 1) {
			document.write("<tr>");
		}
		document.write("<td>");
		screenshot(theStart);
		document.write("</td>");
		if (whichColumn == columns) {
			document.write("</tr>");
			whichColumn = 1;
		} else 
			whichColumn++;
		theStart--;
	}
}

// ------------------------------------------------------------------
// Layout the specific screenshot

function screenshot(number) {
	document.write('<a href="'+myImgPage+'?'+ImgLibrary+','+number+','+screenshotsTotal+'"><img src="' + ImgLibrary + 'thumbs/' + number +'.jpg" border="0" class="thumb"></a>');
}



