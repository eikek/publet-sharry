$(function() {

  var fileupload = $('#fileupload').fileupload({
    singleFileUploads: true,
    limitConcurrentUploads: 2,
    autoUpload: false
  });


  fileupload.bind('fileuploadstart', function (e, data) {
    fileupload.mask({
      spinner: {
        length: 25,
        radius: 15,
        lines: 15,
        width: 3
      },
      label: "Please wait ...",
      overlayOpacity: 0.35
    });
  });

  fileupload.bind('fileuploaddone', function(e, data) {
    fileupload.unmask();
  });

});

