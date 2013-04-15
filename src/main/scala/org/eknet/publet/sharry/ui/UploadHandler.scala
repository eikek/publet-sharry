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

package org.eknet.publet.sharry.ui

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.web.util.{PubletWebContext, RenderUtils}
import org.eknet.publet.web.shiro.Security
import grizzled.slf4j.Logging
import org.eknet.publet.vfs.util.ByteSize
import org.eknet.publet.sharry.lib.FileName

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 20:14
 */
class UploadHandler extends ScalaScript with Logging {

  def serve() = {
    val uploads = PubletWebContext.uploads.map { i => new FileItemEntry(i) }
    val password = PubletWebContext.param("password").filter(!_.isEmpty).map(_.toCharArray).getOrElse(randomPassword(18))
    val name = sharry.addFiles(uploads, Security.username, password, None)

    name.fold(failure, success(password)_)
  }

  private def success(password: Array[Char])(name: FileName) = makeJson(Map(
    "success" -> true,
    "password" -> new String(password),
    "size" -> name.size,
    "sizeString" -> ByteSize.bytes.normalizeString(name.size),
    "created" -> name.date.getTime,
    "checksum" -> name.checksum,
    "name" -> name.fullName,
    "url" -> ("http://localhost:8081/sharry/"+name.fullName)
  ))

  private def failure(exc: Exception) = {
    error("Error adding files!", exc)
    makeJson(Map("success" -> false, "message" -> exc.getMessage))
  }
}
