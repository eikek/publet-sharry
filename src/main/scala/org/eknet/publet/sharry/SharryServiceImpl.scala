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

import grizzled.slf4j.Logging
import java.nio.file.{FileVisitResult, Path}
import java.io.OutputStream
import com.google.inject.{Singleton, Inject}
import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import com.google.inject.name.Named
import org.eknet.publet.vfs.util.ByteSize

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 00:31
 */
@Singleton
class SharryServiceImpl @Inject() (@Named("sharryFolder") folder: Path, @Named("maxSharryFolderSize") maxSize: Long) extends SharryService with Logging {

  import files._
  folder.ensureDirectories()

  def addFiles(files: Iterable[Entry], owner: String, password: String, timeout: Timeout) = {
    if (getFolderSize >= maxSize) {
      Left(new IllegalStateException("Maximum folder size "+ ByteSize.bytes.normalizeString(maxSize) +" exceeded."))
    } else {
      val name = FileName(until = System.currentTimeMillis()+ timeout.millis, owner = owner)
      val file = folder / name.fullName
      FileIO.storeAndEncryptFiles(files, password, file)
      Right(name)
    }
  }

  def lookupFile(name: FileName) = folder /? name.fullName

  def decryptFile(name: FileName, password: String, out: OutputStream) {
    lookupFile(name) match {
      case Some(file) => SymmetricCrypt.decrypt(file, out, password.toCharArray)
      case None => ioError("Cannot find file: "+ name)
    }
  }

  def decryptFile(name: FileName, password: String, target: Path) {
    lookupFile(name) match {
      case Some(file) => SymmetricCrypt.decrypt(file, target, password.toCharArray)
      case None => ioError("Cannot find file: "+ name)
    }
  }

  def removeFiles(filter: FileName => Boolean) = {
    val counter = new AtomicInteger(0)
    folder.visitFiles(file => {
      if (filter(FileName(file))) {
        file.deleteIfExists()
        counter.incrementAndGet()
      }
      FileVisitResult.CONTINUE
    })
    counter.get()
  }

  def getFolderSize = {
    val size = new AtomicLong(0)
    folder.visitFiles(f => { size.addAndGet(f.fileSize); FileVisitResult.CONTINUE })
    size.get()
  }
}
