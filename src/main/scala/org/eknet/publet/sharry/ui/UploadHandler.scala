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
import org.eknet.publet.sharry.lib.{Entry, FileName}
import java.text.DateFormat
import org.apache.commons.fileupload.FileItem
import org.eknet.publet.sharry.SharryService.{AddResponse, AddRequest}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 20:14
 */
class UploadHandler extends ScalaScript with Logging {

  def serve() = {
    import org.eknet.publet.sharry.lib.Timeout._
    val uploads = PubletWebContext.uploads.map { FileItemEntry.apply }
    val timeout = longParam("timeout").filter(_ > 0).map(_.days)
    val req = AddRequest(
      files = uploads,
      password = param("password").map(_.toCharArray).getOrElse(Array()),
      owner = Security.username,
      timeout = timeout,
      filename = param("name")
    )
    val resp = sharry.addFiles(req)
    resp.fold(failure, success)
  }

  private def success(resp: AddResponse) = makeJson(addResponse2Map(resp))

  private def failure(exc: Exception) = {
    error("Error adding files!", exc)
    makeJson(Map("success" -> false, "message" -> exc.getMessage))
  }

  case class FileItemEntry(i: FileItem) extends Entry {
    def name = i.getName
    def inputStream = i.getInputStream
  }
}
