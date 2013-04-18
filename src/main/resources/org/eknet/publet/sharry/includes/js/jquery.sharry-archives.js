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
 * @since 18.04.13 00:51
 */
(function ($) {

  var tableTemplate =
      '<div>' +
      '<ul class="breadcrumb">' +
      '  <li class="active">{{count}} items <span class="divider">/</span></li>' +
      '  <li class="active">{{sizeString}}</li>' +
      '  <li class="active pull-right refreshListButton"><a class="btn btn-mini"><i class="icon-refresh"></i></a></li>' +
      '</ul> '+
      '<table class="table table-condensed table-hover">' +
      '<thead>' +
      '  <tr>' +
      '    <th></th>' +
      '    <th>Name</th>' +
      '    <th>Valid Until</th>' +
      '    <th>Size</th>' +
      '  </tr>' +
      '</thead><tbody>' +
      '{{#archives}}' +
      '<tr>' +
      '  <td>' +
      '    <div class="btn-group">' +
      '      <a class="btn btn-mini dropdown-toggle" data-toggle="dropdown" href="#">Action <span class="caret"></span></a> ' +
      '      <ul class="dropdown-menu">' +
      '        <li><a href="{{url}}">Download</i></a></li> ' +
      '        <li><a href="#">Delete</a></li> ' +
      '      </ul>' +
      '    </div>' +
      '  </td>' +
      '  <td>{{givenName}}</td>' +
      '  <td>{{validUntilDate}}</td>' +
      '  <td>{{sizeString}}</td>' +
      '</tr>' +
      '{{/archives}}' +
      '</tbody></table>' +
      '</div>';

  function render($this, settings) {
    $this.mask();
    $.get(settings.listUrl, { "do": "list" }, function(data) {
      $this.unmask();
      $this.html(Mustache.render(tableTemplate, data));
      $this.find('.refreshListButton').click(function(e) {
        render($this, settings);
      });
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('sharry-archives');

        if (!data) {
          var settings = $.extend({
            listUrl: "actions/listarchives.json"
          }, options);
          $(this).data('sharry-archives', {
            target: $this,
            settings: settings
          });
          render($this, settings);
        }
      });
    }
  };
  $.fn.sharryArchives = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jquery.sharry-archives.js');
    }
  };
})(jQuery);