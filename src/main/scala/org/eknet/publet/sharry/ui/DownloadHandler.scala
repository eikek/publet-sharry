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

import org.eknet.publet.web.{ReqUtils, CustomContent, ErrorResponse}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.eknet.publet.vfs.{ResourceName, ContentResource, ContentType}
import com.google.common.net.HttpHeaders
import grizzled.slf4j.Logging

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.04.13 20:01
 */
class DownloadHandler(val name: ResourceName) extends ContentResource with CustomContent with Logging {

  def exists = true

  override def contentType = ContentType.zip

  def send(req: HttpServletRequest, resp: HttpServletResponse) {
    val util = new ReqUtils(req)
    val password = util.param("password").filter(!_.isEmpty)
    val filename = util.param("filename").filter(!_.isEmpty)
    val archiveinfo = filename.flatMap(f => sharry.findArchive(f))
    (password, filename, archiveinfo) match {
      case (Some(pw), Some(file), Some(ai)) => {
        val fn = ai.archive
        resp.setContentType(ContentType.zip.mimeString)
        resp.setContentLength(fn.size.toInt)
        resp.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+convertName(ai.name)+"."+fn.ext)

        val os = resp.getOutputStream
        wrapException { sharry.decryptFile(fn, pw, os) }
          .fold(error("Error getting file!", _), identity)
      }
      case _ => ErrorResponse.notFound.send(req, resp)
    }
  }

  private def convertName(str: String) = str.replaceAll("\\s+", "_").replaceAll("[^\\w\\._]+", "-")
}
