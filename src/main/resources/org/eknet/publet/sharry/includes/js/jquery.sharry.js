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

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('sharry');

        if (!data) {
          var settings = $.extend({
            defaultSetting: "somevalue"
          }, options);
          $(this).data('sharry', {
            target: $this,
            settings: settings
          });
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
})(jQuery);