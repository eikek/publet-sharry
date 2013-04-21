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

package org.eknet.publet

import org.eknet.publet.web.util.Key
import org.eknet.publet.vfs.util.ByteSize
import grizzled.slf4j.Logging
import scala.Some
import org.eknet.publet.web.Config
import org.eknet.publet.quartz.QuartzDsl

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 18.04.13 10:50
 */
package object sharry extends Logging with QuartzDsl {

  val idKey = Key("sharry-file-id")

  val defaultFolderSize = ByteSize.mib.toBytes(500)
  val defaultMaxUploadSize = ByteSize.mib.toBytes(100)

  val jobdef = newJob[FileDeleteJob]
    .withIdentity("file-delete" in "sharry")
    .withDescription("Removes outdated shared files")
    .build()

  def maxUploadSize(config: Config) = config("sharry.maxUploadSize") match {
    case Some(x) => parseSize(x).getOrElse(defaultMaxUploadSize)
    case _ => defaultMaxUploadSize
  }

  def maxSize(config: Config): Long = config("sharry.maxFolderSize") match {
    case Some(x) => parseSize(x).getOrElse(defaultFolderSize)
    case None => defaultFolderSize
  }


  def parseSize(size: String) = {
    val sizeRegex = """((\d+)(\.\d+)?)(.*)""".r
    size match {
      case sizeRegex(num, i, p, unit) => {
        if (unit.isEmpty) Some(i.toLong)
        else Some(ByteSize.fromString(unit).toBytes(num.toDouble))
      }
      case _ => {
        warn("Cannot parse folder size string: "+ size+ "! Using default.")
        None
      }
    }
  }
}
