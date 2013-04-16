$(function() {

  var fileupload = $('#fileupload').fileupload({
    singleFileUploads: false,
    limitConcurrentUploads: 2,
    autoUpload: false
  });

  fileupload.bind('fileuploaddone', function(e, data) {
    fileupload.unmask();
    var templ = '<div class="hero-unit">' +
        '<h2>Download ready!</h2><br/>'+
        '<p><a href="{{url}}">{{url}} ({{sizeString}})</a> <br/></p>'+
        '<p>Valid until: {{validUntilDate}}</p>'+
        '<p>Password is: <b style="display:none" class="password">{{password}}</b> &nbsp; <a class="btn showPasswordButton">Show password</a></p>' +
        '<hr/>'+
        '<p><a class="btn btn-primary btn-large" href="#"><i class="icon-envelope icon-white"></i> Share via Email</a> </div></p>' +
        '</div>';

    fileupload.html(Mustache.render(templ, data.result));
    fileupload.find('.showPasswordButton').click(function(e) {
      var pw = fileupload.find('.password').css("display");
      if (pw === "none") {
        fileupload.find('.password').css('display', 'inline');
        $(e.target).text("Hide Password");
      } else {
        fileupload.find('.password').css('display', 'none');
        $(e.target).text("Show Password");
      }
    });
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

