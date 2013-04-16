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
import org.eknet.publet.web.util.PubletWeb
import org.eknet.publet.ext.{MailSupport, MailSessionFactory}
import org.eknet.publet.web.Config

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.04.13 21:20
 */
class MailSender extends ScalaScript with MailSupport {

  def serve() = {
    val receivers = param("receivers")
    val subject = param("subject")
    val message = param("message")
    (receivers, subject, message) match {
      case (Some(rec), Some(subj), Some(msg)) => {
        if (mailSession.getSmtpHost.isEmpty || config("sharry.from").isEmpty) {
          makeJson(Map("success" -> false, "message" -> "Mailsettings not configured"))
        } else {
          try {
            rec.split("\\s*,\\s*").foreach { receiver =>
              newMail(config("sharry.from").get)
                .to(receiver)
                .subject(subj)
                .text(msg)
                .send()
            }
            makeJson(Map("success"->true, "message" -> "Mail(s) sent"))
          } catch {
            case e: Exception => {
              error("Error sending mail!", e)
              makeJson(Map("success" -> false, "message" -> e.getMessage))
            }
          }
        }
      }
      case _ => makeJson(Map("success" -> false, "message" -> "Too less arguments"))
    }
  }

  def mailSession = PubletWeb.instance[MailSessionFactory].get
  def config = PubletWeb.instance[Config].get

}
