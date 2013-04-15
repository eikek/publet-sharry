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

import java.io.InputStream
import java.nio.file.{Files, Path}
import org.apache.commons.fileupload.FileItem

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 23:59
 */
trait Entry {

  def name: String
  def inputStream: InputStream

}

object Entry {

  def apply(item: FileItem) = new Entry {
    val name = item.getName
    def inputStream = item.getInputStream
  }

  def apply(file: Path) = new Entry {
    val name = file.getFileName.toString
    def inputStream = Files.newInputStream(file)
  }
}
