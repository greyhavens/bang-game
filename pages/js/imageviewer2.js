// ------------------------------------------------------------------
// Grab all the variables from the URL and figure out what they are

var sPath = window.location.pathname;

var query = window.location.search.substring(1);
var vars = query.split("&");
for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split(",");
}
var imgDir = pair[0];
var screenshotCurrent = pair[1];
var screenshotsTotal = pair[2];

var screenshotNext;
var screenshotPrevious;


calcNewImages();


function calcNewImages()
{
    screenshotNext = eval(screenshotCurrent) + 1;
    screenshotPrevious = eval(screenshotCurrent) - 1;

    if (screenshotPrevious == 0) {
	   screenshotPrevious = screenshotsTotal;
	}
    
    if (screenshotNext > screenshotsTotal) {
	   screenshotNext = 1;    
	}
}

// ------------------------------------------------------------------

//Figure out what are the Next and Previous Images
function nextImage()
{
    screenshotCurrent = screenshotPrevious;
    
    calcNewImages();

    updatePage();
    
    return false;
}

function prevImage()
{
    screenshotCurrent = screenshotNext;
    
    calcNewImages();
    
    updatePage();
    
    return false;
}

//Figure out screenshot numbers
function updatePage()
{
    document.screenshot.src = imgDir + screenshotCurrent + '.jpg';

	document.getElementById('displayCurrentTop').innerHTML = screenshotsTotal - screenshotCurrent + 1;
	document.getElementById('displayCurrentTop').style.fontWeight = "bold";	
	document.getElementById('displayCurrentTop').style.color = "ffffff";

	document.getElementById('displayCurrentBottom').innerHTML = screenshotsTotal - screenshotCurrent + 1;
	document.getElementById('displayCurrentBottom').style.fontWeight = "bold";	
	document.getElementById('displayCurrentBottom').style.color = "ffffff";    
}

//Layout the Nav
function printGalleryNavTop() {	
    document.write ('<table width="700px" border="0"><tr><td width="150px">');
    // Back button to go to the previous graphic
    //document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotNext+','+screenshotsTotal+'"><img src = "/images/screenshots/back.gif" border="0" name="SlideShow" alt="Click on image to view next entry"></a>');
    document.write ('<a href="" onClick="return prevImage();"><img src = "/images/screenshots/back.gif" border="0" name="SlideShow" alt="Previous Image"></a>');


    document.write ('</td><td align="center" >');
    // Tell the user which image they are on, and how many photos in the full collection
    var displayScreenshotCurrent = screenshotsTotal - screenshotCurrent + 1;
    document.write ('<b><span id = "displayCurrentTop" style = "font-weight: bold; color: #330000;">'+ displayScreenshotCurrent +'</span> of '+ screenshotsTotal + '</b><br/>');
    
    // Back to thumbnail gallery 
    document.write ('<a href = "'+ upURL +'">Back to Thumbnail Gallery</a></span>');
    document.write ('</td><td align="right" width="150px">');
    // Next button to go to the next graphic
    //document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotPrevious+','+screenshotsTotal+'"><img src = "/images/screenshots/next.gif" border="0" name="SlideShow" alt="Click on image to view next entry"></a>');
    document.write ('<a href="" onClick="return nextImage();"><img src = "/images/screenshots/next.gif" border="0" name="SlideShow" alt="Next Image"></a>');

    document.write ('</td></tr></table>');
}

function printGalleryNavBottom() {	
    document.write ('<table width="700px" border="0"><tr><td width="150px">');
    // Back button to go to the previous graphic
    //document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotNext+','+screenshotsTotal+'"><img src = "/images/screenshots/back.gif" border="0" name="SlideShow" alt="Click on image to view next entry"></a>');
    document.write ('<a href="" onClick="return prevImage();"><img src = "/images/screenshots/back.gif" border="0" name="SlideShow" alt="Previous Image"></a>');

    document.write ('</td><td align="center" >');
    // Tell the user which image they are on, and how many photos in the full collection
    var displayScreenshotCurrent = screenshotsTotal - screenshotCurrent + 1;
    document.write ('<b><span id = "displayCurrentBottom" style = "font-weight: bold; color: #330000;">'+ displayScreenshotCurrent +'</span> of '+ screenshotsTotal + '</b><br/>');
    
    // Back to thumbnail gallery 
    document.write ('<a href = "'+ upURL +'">Back to Thumbnail Gallery</a></span>');
    document.write ('</td><td align="right" width="150px">');
    // Next button to go to the next graphic
    //document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotPrevious+','+screenshotsTotal+'"><img src = "/images/screenshots/next.gif" border="0" name="SlideShow" alt="Click on image to view next entry"></a>');
    document.write ('<a href="" onClick="return nextImage();"><img src = "/images/screenshots/next.gif" border="0" name="SlideShow" alt="Next Image"></a>');

    document.write ('</td></tr></table>');
}

