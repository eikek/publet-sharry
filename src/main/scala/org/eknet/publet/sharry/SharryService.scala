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

import org.eknet.publet.sharry.lib.{FileName, Timeout, Entry}
import org.eknet.publet.sharry.SharryService.{Alias, ArchiveInfo, AddResponse, AddRequest}
import java.io.OutputStream

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 17.04.13 22:00
 */
trait SharryService {

  def addFiles(req: AddRequest): Either[Exception, AddResponse]

  def decryptFile(name: FileName, password: String, out: OutputStream)

  def removeFiles(filter: ArchiveInfo => Boolean): Int

  def findArchive(name: String): Option[ArchiveInfo]

  def listArchives: Iterable[ArchiveInfo]

  def updateAlias(login: String, alias: Alias)

  def removeAlias(alias: String)

  def findUser(alias: String): Option[String]

  def findAlias(alias: String): Option[Alias]

  def listAliases(login: String): Iterable[Alias]
}

object SharryService {

  case class AddRequest(files: Iterable[Entry],
                        owner: String,
                        password: Array[Char] = Array(),
                        timeout: Option[Timeout] = None,
                        filename: Option[String] = None,
                        sender: Option[String] = None)


  case class AddResponse(id: String, archive: FileName, filename: String, password: Array[Char], sender: String)

  case class ArchiveInfo(archive: FileName, name: String, id: String, sender: String)

  case class Alias(name: String,
                   enabled: Boolean = true,
                   defaultPassword: Array[Char] = Array(),
                   timeout: Option[Timeout] = None,
                   notification: Boolean = true)
}