function show(image) {
  window.open("/screenshots/2006/frontier/shot.html?" + image, "image",
    "resizable=yes,status=no,scrollbars=yes");
  return false;
}

function showImage(imagename) {
    document.thisimage.src = imagename;
}
