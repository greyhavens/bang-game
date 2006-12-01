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

var screenshotNext = eval(screenshotCurrent) + 1;
var screenshotPrevious = screenshotCurrent - 1;

if (screenshotPrevious == 0)
	screenshotPrevious = screenshotsTotal;
if (screenshotNext > screenshotsTotal)
	screenshotNext = 1;	

// ------------------------------------------------------------------

function previousPicture(){

   if (document.all){
      document.images.SlideShow.style.filter="blendTrans(duration=2)"
      document.images.SlideShow.style.filter="blendTrans(duration=crossFadeDuration)"
      document.images.SlideShow.filters.blendTrans.Apply()      
   }

	if (screenshotsTotal > 2)
		document.images.SlideShow.src = preLoad[1].src
	else if (screenshotsTotal == 1)
		document.images.SlideShow.src = preLoad[0].src
	else 
		document.images.SlideShow.src = preLoad[1].src   


   if (document.all){
      document.images.SlideShow.filters.blendTrans.Play()
   }

	var blah = 0;
	var temp;
	Pic[blah] = imgDir + screenshotNext +'.png';
	blah ++;
	
	temp = screenshotCurrent;
	screenshotCurrent = screenshotNext;
	screenshotPrevious = temp;
	screenshotNext++;
	if (screenshotNext > screenshotsTotal)
		screenshotNext = 1;	
	Pic[blah] = imgDir + screenshotNext +'.png';
	blah++;
	Pic[blah] = imgDir + screenshotPrevious +'.png';   

	if (checkTotal != screenshotsTotal || check[screenshotNext-1] == 1) {
		preLoad[1] = new Image();
		check[screenshotNext-1] = 0;
		checkTotal++;
	}

	if (screenshotsTotal > 2) {
		preLoad[0].src = Pic[0];
		preLoad[1].src = Pic[1];
		preLoad[2].src = Pic[2];		
	}
	else if (screenshotsTotal == 1)
		preLoad[0].src = Pic[0];
	else 
		preLoad[1].src = Pic[1];		
	   
	displayScreenshotCurrent = screenshotsTotal - screenshotCurrent + 1;
	document.getElementById('displayCurrent').innerHTML = displayScreenshotCurrent;
	document.getElementById('displayCurrent').style.fontWeight = "bold";	
	document.getElementById('displayCurrent').style.color = "#FFFFFF";	
	
	return false;
}

// ------------------------------------------------------------------
function nextPicture(){

   if (document.all){
      document.images.SlideShow.style.filter="blendTrans(duration=2)"
      document.images.SlideShow.style.filter="blendTrans(duration=crossFadeDuration)"
      document.images.SlideShow.filters.blendTrans.Apply()      
   }

	if (screenshotsTotal > 2)
	   document.images.SlideShow.src = preLoad[2].src
	else if (screenshotsTotal == 1)
		document.images.SlideShow.src = preLoad[0].src
	else 
		document.images.SlideShow.src = preLoad[1].src


   if (document.all){
      document.images.SlideShow.filters.blendTrans.Play()
   }

	var temp;

	temp = screenshotCurrent;
	screenshotCurrent = screenshotPrevious;
	screenshotNext = temp;
	screenshotPrevious--;
	if (screenshotPrevious == 0)
		screenshotPrevious = screenshotsTotal;	
		
	var blah = 0;
	Pic[blah] = imgDir + directory + screenshotCurrent +'.png';
	blah ++;
	Pic[blah] = imgDir + directory + screenshotNext +'.png';
	blah++;
	Pic[blah] = imgDir + directory + screenshotPrevious +'.png';   


	if (checkTotal != screenshotsTotal || check[screenshotPrevious-1] == 1) {
		preLoad[2] = new Image();
		check[screenshotPrevious-1] = 0;
		checkTotal++;
	}

	if (screenshotsTotal > 2){
		preLoad[0].src = Pic[0];
		preLoad[1].src = Pic[1];
		preLoad[2].src = Pic[2];
	}
	else if (screenshotsTotal == 1)
		preLoad[0].src = Pic[0];
	else 
		preLoad[1].src = Pic[1];		


	displayScreenshotCurrent = screenshotsTotal - screenshotCurrent + 1;		
	document.getElementById('displayCurrent').innerHTML = displayScreenshotCurrent;
	document.getElementById('displayCurrent').style.fontWeight = "bold";	
	document.getElementById('displayCurrent').style.color = "#ffffff";	
		   
		   
	return false;
}
// ------------------------------------------------------------------

//Layout the Nav
function printGalleryNav() {	
    document.write ('<table width="700px" border="0"><tr><td width="150px">');
    // Back button to go to the previous graphic
    document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotNext+','+screenshotsTotal+'"><img src = "/images/screenshots/back.gif" border="0" name="SlideShow" alt="Click on image to view next entry"></a>');
    //document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotNext+','+screenshotsTotal+'">Previous</a>');
    document.write ('</td><td align="center" >');
    // Tell the user which image they are on, and how many photos in the full collection
    var displayScreenshotCurrent = screenshotsTotal - screenshotCurrent + 1;
    document.write ('<b><span id = "displayCurrent" style = "font-weight: bold; color: #330000;">'+ displayScreenshotCurrent +'</span> of '+ screenshotsTotal + '</b><br/>');
    // Back to thumbnail gallery 
    document.write ('<a href = "'+ upURL +'">Back to Thumbnail Gallery</a></span>');
    document.write ('</td><td align="right" width="150px">');
    // Next button to go to the next graphic
    document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotPrevious+','+screenshotsTotal+'"><img src = "/images/screenshots/next.gif" border="0" name="SlideShow" alt="Click on image to view next entry"></a>');
    //document.write ('<a href="'+sPath+'?'+imgDir+','+screenshotPrevious+','+screenshotsTotal+'">Next</a>');
    document.write ('</td></tr></table>');
}


