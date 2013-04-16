$(function() {

  function randomString() {
    var chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    var str = "";
    for (var i=0; i<30; i++) {
      str = str + chars.charAt((Math.ceil(Math.random() * chars.length)));
    }
    return str;
  }

});

