/*
 * Copyright 2013 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 11.02.13 21:07
 */
(function ($) {

  var initTemplate = '<div>' +
      '<div class="sharryScreen sharryUpload fade"></div> ' +
      '<div class="sharryScreen sharryUploadDoneOk fade"></div> ' +
      '<div class="sharryScreen sharryShareEmail fade"></div> ' +
      '</div>';

  function showScreens($this, names) {
    $this.find('.sharryScreen').removeClass('in').css('display', 'none');
    $.each(names, function(index, el) {
      $this.find('.'+el).addClass('in').css('display', 'block');
    });
  }

  function renderUpload($this, settings) {
    $this.find('.sharryShareEmail').html(Mustache.render(shareEmailTemplate, settings));
    $this.find('.sharryUpload').html(Mustache.render(uploadFormTemplate, settings));
    var fu = $this.find('form.fileuploadForm').fileupload({
      singleFileUploads: false,
      limitConcurrentUploads: 2,
      autoUpload: false,
      downloadTemplateId: null,
      downloadTemplate: function (o) {
        var rows = $();
        $.each(o.files, function (index, file) {

        });
      }
    });
    fu.bind('fileuploadstart', function (e, data) {
       $this.mask({
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
    fu.bind('fileuploaddone', function(e, data) {
      $this.unmask();
      var fileupload = $this.find('.sharryUploadDoneOk');
      if (data.result.success === false) {
        fileupload.html('<p class="alert alert-error"><strong>Error</strong> '+data.result.message+'</p>');
      } else {
        settings.result = data.result;
        fileupload.html(Mustache.render(uploadOkTemplate, data.result));
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
        fileupload.find('.shareEmailButton').click(function(e) {
          $this.find('.sharryShareEmail input[name="subject"]').val("[Sharry] Download Ready");
          var msg = Mustache.render(emailTextTemplate, settings.result);
          $this.find('.sharryShareEmail textarea[name="message"]').val(msg);
          showScreens($this, ["sharryUploadDoneOk", "sharryShareEmail"]);
        });
      }
      showScreens($this, ["sharryUploadDoneOk"]);
    });
  }


  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('sharry');

        if (!data) {
          var settings = $.extend({
            uploadUrl: "actions/upload.json",
            shareMailUrl: "actions/sharemail.json"
          }, options);
          $(this).data('sharry', {
            target: $this,
            settings: settings
          });

          $this.html(Mustache.render(initTemplate, settings));
          renderUpload($this, settings);
          showScreens($this, ["sharryUpload"]);
        }
      });
    }
  };

  $.fn.sharry = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jquery.sharry.js');
    }
  };

  var uploadFormTemplate =
      '<form class="fileuploadForm form-horizontal" action="{{ uploadUrl }}" method="POST" enctype="multipart/form-data">'+
       ' <p>Use the <em>Add files...</em> button to select files from your hard disk, or just drag&drop them on this page!</p>' +
      '  <div class="fileupload-buttonbar">'+
      '    <span class="btn btn-primary fileinput-button">'+
      '      <i class="icon-plus icon-white"></i>'+
      '      <span>Add files...</span>'+
      '      <input type="file" name="files[]" multiple>'+
      '    </span>'+
      '    <input type="hidden" id="containerPathInput" name="container" value=""/>'+
      '    <button type="submit" class="btn btn-inverse start">'+
      '      <i class="icon-upload icon-white"></i>'+
      '      <span>Start upload</span>'+
      '    </button>'+
      '    <button class="btn btn-inverse cancel" type="reset">'+
      '      <i class="icon-ban-circle icon-white"></i>' +
      '      <span>Cancel upload</span>'+
      '    </buton>'+
      '  </div>'+
      '  <div class="fileupload-loading"></div><br/>'+
      '  <div>'+
      '    <div class="control-group">' +
      '      <label class="control-label">Password</label>'+
      '      <div class="controls">' +
      '        <input type="password" name="password" required="required">'+
      '        <span class="help-block">You can specify a password that is used to encrypt the files. If it is left empty, there is a random one generated for you.</span>' +
      '      </div>'+
      '    </div>'+
      '    <div class="control-group">'+
      '      <label class="control-label">Valid for</label>'+
      '      <div class="controls">'+
      '        <select name="timeout">'+
      '          <option value="1">1 day</option>'+
      '          <option value="7" selected>1 week</option>'+
      '          <option value="14">2 weeks</option>'+
      '          <option value="30">1 month</option>'+
      '          <option value="60">2 month</option>'+
      '          <option value="90">3 month</option>'+
      '          <option value="180">6 month</option>'+
      '          <option value="0">Forever</option>'+
      '        </select>'+
      '      </div>'+
      '    </div>'+
      '  </div>'+
      '  <div>'+
      '    <div>'+
      '      <table role="presentation" class="table table-striped">'+
      '        <tbody class="files" data-toggle="modal-gallery" data-target="#modal-gallery"></tbody>'+
      '      </table>'+
      '    </div>'+
      '  </div>'+
      '</form>' +
      '<script id="template-upload" type="text/x-tmpl">'+
      '{% for (var i=0, file; file=o.files[i]; i++) { %}'+
      '  <tr class="template-upload fade">'+
      '    <td class="preview"><span class="fade"></span></td>'+
      '    <td class="name"><span>{%=file.name%}</span></td>'+
      '    <td class="size"><span>{%=o.formatFileSize(file.size)%}</span></td>'+
      '    {% if (file.error) { %}'+
      '      <td class="error" colspan="2"><span class="label label-important">Error</span> {%=file.error%}</td>'+
      '    {% } else if (o.files.valid && !i) { %}'+
      '      <td>'+
      '        <div class="progress progress-success progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0"><div class="bar" style="width:0%;"></div></div>'+
      '      </td>'+
      '      <td style="display:none">{% if (!o.options.autoUpload) { %}'+
      '        <button class="btn btn-primary start">'+
      '          <i class="icon-upload icon-white"></i>'+
      '          <span>Start</span>'+
      '        </button>'+
      '       {% } %}</td>'+
      '    {% } else { %}'+
      '    <td colspan="2"></td>'+
      '    {% } %}'+
      '    <td  style="display:none">{% if (!i) { %}'+
      '      <button class="btn btn-warning cancel">'+
      '        <i class="icon-ban-circle icon-white"></i>'+
      '        <span>Cancel</span>'+
      '      </button>'+
      '    {% } %}</td>'+
      '  </tr>'+
      '{% } %}'+
      '</script>';

  var uploadOkTemplate = '<div class="well">' +
      '<h2>Download ready!</h2><br/>'+
      '<p><a href="{{url}}">{{url}} ({{sizeString}})</a> <br/></p>'+
      '<p>Valid until: {{validUntilDate}}</p>'+
      '<p>Password is: <b style="display:none" class="password">{{password}}</b> &nbsp; <a class="btn showPasswordButton">Show password</a></p>' +
      '<hr/>'+
      '<p><a class="btn btn-primary btn-large shareEmailButton" href="#"><i class="icon-envelope icon-white"></i> Share via Email</a> </div></p>' +
      '</div>';

  var shareEmailTemplate =
      '<form action="{{shareMailUrl}}">' +
      '  <fieldset>'+
      '  <label>Receivers <small style="color:#666;">(list of emails, for example: me@gmail.com, john.doe@hotmail.com)</small></label>'+
      '  <input class="input-block-level" type="text" name="receivers" required="required"/> '+
      '  <label>Subject</label>' +
      '  <input class="input-block-level" type="text" name="subject"/>' +
      '  <label>Message</label>'+
      '  <textarea class="input-block-level" name="message" cols="30" rows="10"/>' +
      '  <br/><a href="#" class="btn btn-primary"><i class="icon-envelope icon-white"></i> Send</a> '+
      '  </fieldset>'+
      '</form>';

  var emailTextTemplate = "Hi,\n\n" +
      "The download ({{sizeString}}) is ready:\n\n" +
      "{{&url}}\n\n" +
      "Password: {{password}}\n" +
      "The link is valid until {{validUntilDate}}.\n\n"+
      "Regards";

})(jQuery);