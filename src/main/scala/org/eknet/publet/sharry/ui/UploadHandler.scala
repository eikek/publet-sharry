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
import org.eknet.publet.web.util.{PubletWeb, PubletWebContext}
import org.eknet.publet.web.shiro.Security
import grizzled.slf4j.Logging
import org.eknet.publet.vfs.util.ByteSize
import org.eknet.publet.sharry.lib.Entry
import org.apache.commons.fileupload.FileItem
import org.eknet.publet.sharry.SharryService.{AddResponse, AddRequest}
import org.eknet.publet.auth.store.{DefaultAuthStore, UserProperty}
import org.eknet.publet.{sharry => psharry}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 20:14
 */
class UploadHandler extends ScalaScript with Logging {

  def serve() = {
    val uploads = PubletWebContext.uploads.map { FileItemEntry.apply }
    if (uploads.isEmpty) {
      makeFailure("No files given.")
    } else {
      val size = PubletWebContext.uploads.foldLeft(0L)((s,t) => s+ t.getSize)
      if (size > maxUploadSize) {
        makeFailure("Maximum upload size ("+ ByteSize.bytes.normalizeString(maxUploadSize) +") exceeded!")
      } else {
        param("forAlias") match {
          case Some(alias) => anonymousUpload(alias, uploads)
          case _ => authenticatedUpload(uploads)
        }
      }
    }
  }

  private def success(resp: AddResponse) = makeJson(addResponse2Map(resp))

  private def failure(exc: Exception) = {
    error("Error adding files!", exc)
    makeJson(Map("success" -> false, "message" -> exc.getMessage))
  }

  private def anonymousUpload(aliasId: String, uploads: Iterable[Entry]) = {
    sharry.findAlias(aliasId).map(a => (a, sharry.findUser(a.name))) match {
      case Some((alias, Some(owner))) => {
        import org.eknet.publet.sharry.lib.Timeout._
        val timeout = alias.timeout.getOrElse(14.days)
        val req = AddRequest (
          files = uploads,
          password = alias.defaultPassword,
          owner = owner,
          timeout = Some(timeout),
          filename = param("name"),
          sender = Some(alias.name)
        )
        val resp = sharry.addFiles(req)
        if (alias.notification && resp.isRight) {
          authStore.findUser(owner).flatMap(_.get(UserProperty.email)) match {
            case Some(email) => {
              val subject = "[Sharry] Upload '%s' ready".format(resp.right.get.filename)
              sendMails(email, email, subject, mailText(resp.right.get))
                .left.map(e => error("Cannot send mail!", e))
            }
            case _ => warn("No email for user '"+owner+"'!")
          }
        }
        resp.right.map(_.copy(password = "")).fold(failure, success)
      }
      case _ => makeJson(Map("success" -> false, "message" -> "Alias not found."))
    }
  }

  private def mailText(resp: AddResponse) =
    """Hi %s,
      |
      |The upload '%s' (%s) has completed from your alias page %s.
      |
      |Check it out: %s
      |Password: %s
      |
      |It is valid until %s.
      |
      |Regards.
    """.stripMargin.format(resp.archive.owner,
      resp.filename,
      ByteSize.bytes.normalizeString(resp.archive.size),
      resp.sender,
      PubletWebContext.urlOf(sharryPath / "download" / resp.id),
      new String(resp.password),
      untilDateString(resp.archive))

  private def authenticatedUpload(uploads: Iterable[Entry]) = asSharryUser {
    import org.eknet.publet.sharry.lib.Timeout._
    val timeout = longParam("timeout").filter(_ > 0).map(_.days)
    val req = AddRequest(
      files = uploads,
      password = param("password").getOrElse(""),
      owner = Security.username,
      timeout = timeout,
      filename = param("name")
    )
    val resp = sharry.addFiles(req)
    resp.fold(failure, success)
  }

  case class FileItemEntry(i: FileItem) extends Entry {
    def name = i.getName
    def inputStream = i.getInputStream
  }

  def authStore = PubletWeb.instance[DefaultAuthStore].get

  def maxUploadSize = psharry.maxUploadSize(config)
}
