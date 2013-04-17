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
import org.eknet.publet.sharry.SharryService.ArchiveInfo
import org.eknet.publet.vfs.util.ByteSize

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 18.04.13 00:43
 */
class ArchiveManage extends ScalaScript {

  def serve() = {
    Security.checkAuthenticated()
    val currentOwner = (ai: ArchiveInfo) => ai.archive.owner == Security.username
    val list = sharry.listArchives.filter(currentOwner).toList
    val size = list.map(_.archive.size).foldLeft(0L)((s,t) => s+t)
    makeJson(Map(
      "archives" -> list.map(archiveInfo2Map),
      "size" -> size,
      "sizeString" -> ByteSize.bytes.normalizeString(size),
      "count" -> list.size
    ))
  }
}
