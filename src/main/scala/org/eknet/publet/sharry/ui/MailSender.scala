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
import org.eknet.publet.web.shiro.Security
import org.eknet.publet.auth.store.UserProperty

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.04.13 21:20
 */
class MailSender extends ScalaScript {

  def serve() = {
    Security.checkAuthenticated()
    val receivers = param("receivers")
    val subject = param("subject")
    val message = param("message")
    (receivers, subject, message) match {
      case (Some(rec), Some(subj), Some(msg)) => {
        sendMails(getFromMail.get, rec, subj, msg)
          .fold(failure, success)
      }
      case _ => makeJson(Map("success" -> false, "message" -> "Too less arguments"))
    }
  }

  def failure(e: Exception) = {
    error("Error sending mails", e)
    makeJson(Map("success" -> false, "message" -> e.getMessage))
  }

  def success(msg: String) = {
    makeJson(Map("success" -> true, "message" -> msg))
  }

  def getFromMail = Security.user.flatMap(_.get(UserProperty.email)).orElse(config("sharry.from"))
}
