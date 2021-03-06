
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

package org.eknet.publet.sharry

import org.eknet.publet.web.util.{PubletWebContext, PubletWeb, RenderUtils}
import org.eknet.publet.ext.{MailSupport, MailSessionFactory}
import org.eknet.publet.web.Config
import com.google.common.base.Splitter
import org.eknet.publet.sharry.lib.FileName
import org.eknet.publet.vfs.util.ByteSize
import java.text.DateFormat
import org.eknet.publet.sharry.SharryService.{Alias, AddResponse, ArchiveInfo}
import org.eknet.publet.vfs.{Content, Path}
import org.eknet.publet.web.shiro.Security

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 15.04.13 19:00
 */
package object ui extends MailSupport {

  def wrapException[A](body: => A) = {
    try {
      Right(body)
    } catch {
      case e:Exception => Left(e)
    }
  }

  def param(name: String) = PubletWebContext.param(name).filter(!_.isEmpty)
  def longParam(name: String) = param(name).map(_.toLong)
  def boolParam(name: String) = param(name) match {
    case Some(x) if (x.equalsIgnoreCase("on")) => true
    case Some(x) if (x.equalsIgnoreCase("yes")) => true
    case Some(x) if (x.equalsIgnoreCase("true")) => true
    case _ => false
  }

  def makeJson(data: Any) = RenderUtils.makeJson(data)

  def makeSuccess(message: String) = makeJson(Map("success" -> true, "message" -> message))
  def makeFailure(message: String) = makeJson(Map("success" -> false, "message" -> message))

  def sharry = PubletWeb.instance[SharryService].get

  def sendMails(from: String, tos: String, subject: String, text: String): Either[Exception, String] = {
    def isValid(str: String) = str != null && !str.isEmpty
    import collection.JavaConversions._
    wrapException {
      if (mailSession.getSmtpHost.isEmpty || !isValid(from) || !isValid(tos) || !isValid(subject)) {
        throw new IllegalArgumentException("To less arguments for sending mail.")
      } else {
        Splitter.on(',').trimResults().omitEmptyStrings().split(tos).foreach { receiver =>
          newMail(from)
            .to(receiver)
            .subject(subject)
            .text(text)
            .send()
        }
        "Mails sent"
      }
    }
  }

  def uploadUrl(aliasName: String) =  PubletWebContext.urlOf(sharryPath / "upload" / aliasName)
  def downloadUrl(fileId: String) =  PubletWebContext.urlOf(sharryPath / "download" / fileId)

  def fileName2Map(name: FileName) = Map(
    "size" -> name.size,
    "sizeString" -> ByteSize.bytes.normalizeString(name.size),
    "created" -> name.time,
    "validUntil" -> name.until,
    "validUntilDate" -> untilDateString(name),
    "checksum" -> name.checksum,
    "owner" -> name.owner,
    "name" -> name.fullName
  )

  def untilDateString(name: FileName) = if (name.until<=0) "Forever"
    else DateFormat.getDateInstance(DateFormat.LONG, PubletWebContext.getLocale).format(new java.util.Date(name.until))

  def archiveInfo2Map(ai: ArchiveInfo) = fileName2Map(ai.archive) ++ Map(
    "id" -> ai.id,
    "givenName" -> ai.name,
    "sender" -> ai.sender,
    "senderIsOwner" -> (ai.sender == ai.archive.owner),
    "clickCount" -> sharry.clickCount(ai.archive),
    "url" -> downloadUrl(ai.id)
  )

  def addResponse2Map(resp: AddResponse) = fileName2Map(resp.archive) ++ Map(
    "id" -> resp.id,
    "givenName" -> resp.filename,
    "password" -> new String(resp.password),
    "url" -> downloadUrl(resp.id)
  )

  def aliasToMap(a: Alias) = Map(
    "name" -> a.name,
    "defaultPassword" -> (if (a.defaultPassword.isEmpty) false else new String(a.defaultPassword)),
    "timeout" -> a.timeout.map(_.days.toInt).getOrElse(-1),
    "enabled" -> a.enabled,
    "notification" -> a.notification,
    "url" -> uploadUrl(a.name)
  )

  def mailSession = PubletWeb.instance[MailSessionFactory].get
  def config = PubletWeb.instance[Config].get

  def sharryPath = PubletWeb.instance[Path].named("sharryPath")

  def asSharryUser(body: => Option[Content]) = {
    if (Security.hasPerm(Permissions.sharryAllowed)) {
      body
    } else {
      makeFailure("Unauthorized.")
    }
  }
}
