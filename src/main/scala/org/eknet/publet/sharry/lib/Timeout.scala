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

package org.eknet.publet.sharry.lib

import java.util.concurrent.TimeUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 22:51
 */
case class Timeout(value: Long, unit: TimeUnit) {

  lazy val millis = TimeUnit.MILLISECONDS.convert(value, unit)

}

object Timeout {

  class TimeoutLong(value: Long) {
    def seconds = Timeout(value, TimeUnit.SECONDS)
    def minutes = Timeout(value, TimeUnit.MINUTES)
    def hours = Timeout(value, TimeUnit.HOURS)
    def days = Timeout(value, TimeUnit.DAYS)
    def week = Timeout(7 * value, TimeUnit.DAYS)
  }

  implicit def longForTimeout(value: Long) = new TimeoutLong(value)
}