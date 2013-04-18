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
 * @since 18.04.13 12:15
 */
(function ($) {

  var template =
      '<div>' +
      '<ul class="breadcrumb">' +
      '  <li class="active">{{count}} aliases <span class="divider">/</span></li>' +
      '  <li class="active">{{activeCount}} active <span class="divider">/</span></li>' +
      '  <li class="active">{{disabledCount}} disabled</li>' +
      '  <li class="active pull-right">' +
      '    <a class="btn btn-mini refreshListButton"><i class="icon-refresh"></i></a> | ' +
      '    <a class="btn btn-mini addAliasButton"><i class="icon-plus"></i></a>'+
      '  </li>' +
      '</ul> ' +
      '<div class="aliasFeedback"></div> '+
      '<table class="table table-condensed table-hover">' +
      '<thead><tr>' +
      '  <th></th>' +
      '  <th>Active</th>' +
      '  <th>Url</th>' +
      '  <th>Password</th>' +
      '  <th>Timeout</th>' +
      '</tr></thead>' +
      '{{#aliases}}' +
      '<tr>' +
      '  <td>' +
      '    <div class="btn-group">' +
      '      <a class="btn btn-mini dropdown-toggle" data-toggle="dropdown" href="#">Action <span class="caret"></span></a> ' +
      '      <ul class="dropdown-menu">' +
      '        <li><a href="#" class="aliasEditButton" data-alias="{{name}}">Edit</i></a></li> ' +
      '        <li><a href="#" class="aliasDeleteButton" data-alias="{{name}}">Delete</a></li> ' +
      '      </ul>' +
      '    </div>' +
      '  </td>' +
      '  <td><input type="checkbox" disabled="disabled" {{#enabled}}checked="yes"{{/enabled}}></td>' +
      '  <td><a href="{{&url}}">{{url}}</a></td>' +
      '  <td>{{#defaultPassword}}yes{{/defaultPassword}} {{^defaultPassword}}no{{/defaultPassword}}</td>' +
      '  <td>{{timeout}} days</td>' +
      '</tr>' +
      '{{/aliases}}' +
      '</table>' +
      '<div class="modal hide fade">' +
      '  <div class="modal-header">' +
      '    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' +
      '    <h3>Edit Alias</h3> ' +
      '  </div>' +
      '  <form action="{{actionUrl}}" method="post" class="addAliasForm">' +
      '    <div class="formFeedback"></div>' +
      '    <input type="hidden" name="do" value="updateAlias"/> ' +
      '    <input type="hidden" name="aliasName" value=""/> ' +
      '    <div class="modal-body">' +
      '      <div class="readOnlyAlias">' +
      '        <label>Alias Name</label>' +
      '        <span class="uneditable-input">randomly generated</span> ' +
      '      </div> '+
      '      <label>Default Password</label>' +
      '      <input type="password" placeholder="optional default password" name="defaultPassword"/> ' +
      '      <label>Timeout</label>' +
      '      <select name="timeout">'+
      '        <option value="1">1 day</option>'+
      '        <option value="7" selected>1 week</option>'+
      '        <option value="14">2 weeks</option>'+
      '        <option value="30">1 month</option>'+
      '        <option value="60">2 month</option>'+
      '        <option value="90">3 month</option>'+
      '        <option value="180">6 month</option>'+
      '      </select>' +
      '      <label class="checkbox">' +
      '        <input type="checkbox" name="enabled"> Enabled' +
      '      </label> '+
      '      <label class="checkbox">' +
      '        <input type="checkbox" name="notification"> Notify on uploads' +
      '      </label> '+
      '    </div>' +
      '    <div class="modal-footer">' +
      '      <a href="#" class="btn">Cancel</a> ' +
      '      <button type="submit" class="btn btn-primary">Save</button> ' +
      '    </div> '+
      '  </form> ' +
      '</div> ' +
      '</div>';

  function render($this, settings) {
    $this.mask();
    $.get(settings.actionUrl, { "do": "list"}, function(data) {
      data.actionUrl = settings.actionUrl;
      $this.html(Mustache.render(template, data));
      $this.find('.addAliasForm').ajaxForm({
        beforeSubmit: function(arr, form, options) {
          form.mask();
        },
        success: function(resp, status, xhr, form) {
          form.unmask();
          if (resp.success === false) {
            form.find('.formFeedback').feedbackMessage({
              message: resp.message,
              cssClass: 'alert alert-error'
            });
          } else {
            $this.find('.modal').modal("toggle");
            render($this, settings);
          }
        }
      });
      $this.find('.addAliasButton').click(function(e) {
        $this.find('.addAliasForm').clearForm();
        $this.find('.readOnlyAlias .uneditable-input').text('generated randomly');
        $this.find('.modal').modal('toggle');
      });
      $this.find('.refreshListButton').click(function(e) {
        render($this, settings);
      });
      $this.find('.aliasDeleteButton').click(function(e) {
        var target = $(e.target);
        var alias = target.attr('data-alias');
        $this.mask();
        $.post(settings.actionUrl, {"do": "removeAlias", "aliasName": alias}, function(data) {
          $this.unmask();
          if (data.success === false) {
            $this.find('.aliasFeedback').feedbackMessage({
              message: data.message,
              cssClass: 'alert alert-error'
            });
          } else {
            render($this, settings);
          }
        })
      });
      $this.find('.aliasEditButton').click(function(e) {
        var target = $(e.target);
        var alias = target.attr('data-alias');
        $.get(settings.actionUrl, {"do":"getAlias", "aliasName": alias}, function(data) {
          if (data.success === true) {
            var form = $this.find('.addAliasForm').clearForm();
            form.find('input[name="aliasName"]').val(alias);
            form.find('.readOnlyAlias .uneditable-input').text(alias);
            if (data.defaultPassword) {
              form.find('input[name="defaultPassword"]').val(data.defaultPassword);
            }
            if (data.enabled) {
              form.find('input[name="enabled"]').attr('checked', 'yes');
            } else {
              form.find('input[name="enabled"]').removeAttr('checked');
            }
            if (data.notification) {
              form.find('input[name="notification"]').attr('checked', 'yes');
            } else {
              form.find('input[name="notification"]').removeAttr('checked');
            }
            form.find('option[value="'+data.timeout+'"]').attr('selected', 'yes');
            $this.find('.modal').modal('toggle');
          }
        });
      });

      $this.unmask();
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('sharry-alias');

        if (!data) {
          var settings = $.extend({
            actionUrl: "actions/managealias.json"
          }, options);
          $(this).data('sharry-alias', {
            target: $this,
            settings: settings
          });
          render($this, settings);
        }
      });
    }
  };

  $.fn.sharryAlias = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jquery.sharry-alias.js');
    }
  };
})(jQuery);