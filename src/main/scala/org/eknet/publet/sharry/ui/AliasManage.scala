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
import org.eknet.publet.web.util.PubletWebContext
import org.eknet.publet.sharry.SharryServiceImpl
import org.eknet.publet.sharry.lib.Timeout
import org.eknet.publet.sharry.SharryService.Alias
import java.util.concurrent.TimeUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 18.04.13 12:23
 */
class AliasManage extends ScalaScript {

  def serve() = {
    param("do") match {
      case Some("list") => listAliases
      case Some("updateAlias") => updateAlias()
      case Some("removeAlias") => removeAlias()
      case Some("getAlias") => getAlias
      case e@_ => makeJson(Map("success" -> false, "message" -> ("Command not found: "+e)))
    }
  }

  def getAlias = {
    Security.checkAuthenticated()
    val name = param("aliasName")
    name match {
      case Some(n) => {
        sharry.findAlias(n).map(aliasToMap).map(m => m + ("success" -> true)).flatMap(makeJson)
      }
      case _ => makeJson(Map("success" -> false, "message" -> "No alias name given."))
    }
  }

  def removeAlias() = {
    Security.checkAuthenticated()
    val name = param("aliasName")
    name match {
      case Some(n) => {
        sharry.removeAlias(n)
        makeJson(Map("success" -> true, "message" -> "Alias removed."))
      }
      case _ => makeJson(Map("success" -> false, "message" -> "No alias name given."))
    }
  }

  def updateAlias() = {
    val alias = aliasFromParams()
    if (Security.isAuthenticated) {
      sharry.updateAlias(Security.username, alias)
    }
    makeJson(Map("success" -> true, "message" -> "Alias added"))
  }

  private def aliasFromParams() = {
    import Timeout._
    val aliasName = param("aliasName").getOrElse(SharryServiceImpl.randomId(8))
    val timeout = longParam("timeout").getOrElse(14L).days
    val defaultPassw = param("defaultPassword").map(_.toCharArray).getOrElse(Array[Char]())
    val enabled = boolParam("enabled")
    val notification = boolParam("notification")
    Alias(aliasName, enabled, defaultPassw, Some(timeout), notification)
  }
  def listAliases = {
    val login = Security.username
    val list = sharry.listAliases(login)
    val activeCount = list.foldLeft(0)((s,t) => if (t.enabled) s+1 else s)
    val inactiveCount = list.size - activeCount
    makeJson(Map(
      "aliases" -> list.map(aliasToMap).toList,
      "success" -> true,
      "count" -> list.size,
      "activeCount" -> activeCount,
      "disabledCount" -> inactiveCount
    ))
  }
}
