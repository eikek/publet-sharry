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

package org.eknet.publet.sharry.lib

import grizzled.slf4j.Logging
import java.nio.file.Path
import scala.util.parsing.combinator.RegexParsers

/**
 *
 * @param time the timestamp this file was created
 * @param until the timestamp until this file is valid. after this point in time, it
 *              can safely be removed.
 * @param owner the owner of this file. the subject who owns this file
 * @param checksum checksum of the content
 * @param size the file size in bytes
 * @param ext the file extension. default is "zip"
 * @param version the version of this file. this can be useful if encryption changes later
 *                to still be able to distinguish other versions when decrypting.
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 00:03
 */
case class FileName(time: Long = System.currentTimeMillis(),
                    until: Long = 0,
                    owner: String,
                    checksum: String,
                    size: Long,
                    ext: String = "zip",
                    version: Long = 1) {

  require(checksum != null, "checksum must be specified")

  val fullName = {
    val buf = new StringBuilder
    buf.append(time).append(".")
    buf.append(until).append(".")
    buf.append(checksum).append(".")
    buf.append(owner).append(".")
    buf.append(size).append(".")
    buf.append(version).append(".")
    buf.append(ext)
    buf.toString()
  }

}

object FileName extends Logging {

  def apply(name: Path): FileName = FileName(name.getFileName.toString)
  def apply(name: String): FileName = Parser.parseFilename(name).fold(x => sys.error(x), identity)

  def tryParse(name: String): Option[FileName] = Parser.parseFilename(name).fold(x => None, Some(_))
  def tryParse(name: Path): Option[FileName] = tryParse(name.getFileName.toString)

  val outdated: FileName => Boolean = name => name.until > 0 && name.until <= System.currentTimeMillis()

  private object Parser extends RegexParsers {

    def filename = timestamp ~"."~ timestamp ~"."~ checksum ~"."~ owner ~"."~ size ~"."~ version ~"."~ ext ^^ {
      case added ~"."~ until ~"."~ uniq ~"."~ login ~"."~ sz ~"."~ vers ~"."~ extension => new FileName(added, until, uniq, login, sz, extension, vers)
      case x@_ => sys.error("Wrong file name: "+ x)
    }

    private val timestamp = "[0-9]+".r ^^ (_.toLong)
    private val size = "[0-9]+".r ^^ (_.toLong)
    private val owner = "[\\w]+".r
    private val checksum = "[a-zA-Z0-9]+".r
    private val ext = "[a-zA-Z0-9]+".r
    private val version ="[0-9]+".r ^^ (_.toLong)

    def parseFilename(name: String) = parseAll(filename, name) match {
      case Success(result, _) => Right(result)
      case failure: NoSuccess => Left(failure.msg)
    }
  }

}
