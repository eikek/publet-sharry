
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
import java.security.SecureRandom

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 15.04.13 19:00
 */
package object ui extends MailSupport {

  def param(name: String) = PubletWebContext.param(name).filter(!_.isEmpty)
  def longParam(name: String) = param(name).map(_.toLong)

  def makeJson(data: Any) = RenderUtils.makeJson(data)

  def sharry = PubletWeb.instance[SharryService].get

  private val chars = '-' :: '_' :: ('a' to 'z').toList ::: ('A' to 'Z').toList ::: ('0' to '9').toList

  def randomPassword(len: Int) = {
    val random = new SecureRandom()
    val pw = for (i <- 1 to len) yield chars(random.nextInt(chars.size))
    pw.toArray
  }

  def sendMails(from: String, tos: String, subject: String, text: String): Either[Exception, String] = {
    def isValid(str: String) = str != null && !str.isEmpty
    import collection.JavaConversions._
    if (mailSession.getSmtpHost.isEmpty || !isValid(from) || !isValid(tos) || !isValid(subject)) {
      Left(new IllegalArgumentException("To less arguments for sending mail."))
    } else {
      try {
        Splitter.on(',').trimResults().omitEmptyStrings().split(tos).foreach { receiver =>
          newMail(from)
            .to(receiver)
            .subject(subject)
            .text(text)
            .send()
        }
        Right("Mails sent")
      } catch {
        case e: Exception => Left(e)
      }
    }
  }

  def mailSession = PubletWeb.instance[MailSessionFactory].get
  def config = PubletWeb.instance[Config].get

}
