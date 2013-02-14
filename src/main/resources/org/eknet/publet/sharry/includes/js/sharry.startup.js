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

  function randomString() {
    var chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    var str = "";
    for (var i=0; i<30; i++) {
      str = str + chars.charAt((Math.ceil(Math.random() * chars.length)));
    }
    return str;
  }
  fileupload.find('input[name="uploadKey"]').val(randomString());
});

