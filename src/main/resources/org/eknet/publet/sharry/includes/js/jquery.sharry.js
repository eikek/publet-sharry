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
      '<div class="sharryShareEmail"></div> ' +
      '</div>';

  function showScreens($this, names) {
    $this.find('.sharryScreen').removeClass('in').css('display', 'none');
    $.each(names, function(index, el) {
      $this.find('.'+el).addClass('in').css('display', 'block');
    });
  }

  function renderUpload($this, settings) {
    $this.html(Mustache.render(initTemplate, settings));
    $this.find('.sharryShareEmail').html(Mustache.render(shareEmailTemplate, settings));
    $this.find('.sharryShareEmail form').ajaxForm({
      beforeSubmit: function(arr, form, options) {
        form.mask();
      },
      success: function(resp, status, xhr, form) {
        form.unmask();
        var css = resp.success === false ? "alert-error" : "alert-success";
        form.find('.mailFeedback').feedbackMessage({
          message: resp.message,
          cssClass: 'alert ' + css
        });
      }
    });

    $this.find('.sharryUpload').html(Mustache.render(uploadFormTemplate, settings));
    var fu = $this.find('form.fileuploadForm').fileupload({
      singleFileUploads: false,
      limitConcurrentUploads: 1,
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
        if (settings.forAlias) {
          fileupload.html(Mustache.render(uploadOkAnonymousTemplate, data.result));
        } else {
          fileupload.html(Mustache.render(uploadOkTemplate, data.result));
        }

        fileupload.find('.shareEmailButton').click(function(e) {
          $this.find('.mailModal').modal('toggle');
        });
        fileupload.find('.newUploadButton').click(function(e) {
          fu.fileupload("destroy");
          renderUpload($this, settings);
        });

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

        $this.find('.sharryShareEmail input[name="subject"]').val("[Sharry] Download Ready");
        var msg = Mustache.render(emailTextTemplate, data.result);
        $this.find('.sharryShareEmail textarea[name="message"]').val(msg);

      }
      showScreens($this, ["sharryUploadDoneOk"]);
    });

    showScreens($this, ["sharryUpload"]);
  }


  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('sharry');

        if (!data) {
          var settings = $.extend({
            uploadUrl: "open/upload.json",
            shareMailUrl: "actions/sharemail.json",
            forAlias: null
          }, options);
          $(this).data('sharry', {
            target: $this,
            settings: settings
          });

          renderUpload($this, settings);
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
      '  <div class="fileupload-buttonbar">'+
      '    <span class="btn btn-primary fileinput-button">'+
      '      <i class="icon-plus icon-white"></i>'+
      '      <span>Add files...</span>'+
      '      <input type="file" name="files[]" multiple>'+
      '    </span>'+
      '    <input type="hidden" id="containerPathInput" name="container" value=""/>'+
      '   {{#forAlias}}<input type="hidden" name="forAlias" value="{{forAlias}}"/> {{/forAlias}}' +
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
      '  {{^forAlias}}' +
      '    <div class="control-group">' +
      '      <label class="control-label">Password</label>'+
      '      <div class="controls">' +
      '        <input type="password" name="password" placeholder="optional password">'+
      '        <span class="help-inline">or a random one is used</span>' +
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
      '    </div>' +
      '  {{/forAlias}}' +
      '    <div class="control-group">' +
      '      <label class="control-label">Name</label>' +
      '      <div class="controls">' +
      '        <input type="text" placeholder="optional file name" name="name">' +
      '        <span class="help-inline">or a random one is created</span> ' +
      '      </div>' +
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

  var uploadOkTemplate =
      '<div class="well">' +
      '  <h2>"{{givenName}}" ready for download!</h2><br/>'+
      '  <p><a href="{{url}}">{{url}} ({{sizeString}})</a> <br/></p>'+
      '  <p>Valid until: {{validUntilDate}}</p>'+
      '  <p>Password is: <b style="display:none" class="password">{{password}}</b> &nbsp; <a class="btn showPasswordButton">Show password</a></p>' +
      '  <hr/>'+
      '  <p>' +
      '   <a class="btn btn-primary shareEmailButton" href="#"><i class="icon-envelope icon-white"></i> Share via Email</a> ' +
      '   <a class="btn btn-success newUploadButton" href="#"><i class="icon-repeat icon-white"></i> New Upload</a> ' +
      '  </p>' +
      '</div>';
  var uploadOkAnonymousTemplate =
      '<div class="well">' +
      '  <h2>"{{givenName}}" successfully uploaded!</h2><br/>'+
      '  <p>The user has been notified. Thank you.</p>'+
      '  <hr/>'+
      '  <p>' +
      '   <a class="btn btn-success newUploadButton" href="#"><i class="icon-repeat icon-white"></i> New Upload</a> ' +
      '  </p>' +
      '</div>';

  var shareEmailTemplate =
      '<div class="modal hide fade mailModal">' +
      '  <form action="{{shareMailUrl}}">' +
      '  <div class="modal-header">' +
      '    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>'+
      '    <h3>Send Email</h3>'+
      '  </div> '+
      '  <div class="modal-body">' +
      '    <fieldset>'+
      '      <label>Receivers</label>'+
      '      <input class="input-block-level" type="text" name="receivers" required="required"/> ' +
      '      <span class="help-block">list of emails, for example: me@gmail.com, john.doe@hotmail.com</span> '+
      '      <label>Subject</label>' +
      '      <input class="input-block-level" type="text" name="subject"/>' +
      '      <label>Message</label>'+
      '      <textarea class="input-block-level" name="message" cols="30" rows="9"/>' +
      '    </fieldset>'+
      '  </div> '+
      '  <div class="modal-footer">' +
      '    <span class="mailFeedback"></span>'+
      '      <a class="btn" data-dismiss="modal">Close</a> ' +
      '    <button href="#" class="btn btn-primary"><i class="icon-envelope icon-white"></i> Send</button> '+
      '  </div> '+
      '  </form>'+
      '</div> ';

  var emailTextTemplate = "Hi,\n\n" +
      "The download '{{givenName}}' ({{sizeString}}) is ready:\n\n" +
      "{{&url}}\n\n" +
      "Password: {{password}}\n" +
      "The link is valid until {{validUntilDate}}.\n\n"+
      "Regards";

})(jQuery);