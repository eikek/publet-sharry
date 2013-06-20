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
import java.io.OutputStream
import java.nio.file.DirectoryStream.Filter
import java.nio.file.{Files, FileVisitResult, Path}
import java.security.{DigestOutputStream, MessageDigest}
import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import javax.xml.bind.DatatypeConverter
import org.eknet.publet.vfs.util.ByteSize
import scala.io.Codec

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 00:31
 */
class SharryImpl(folder: Path, var folderSizeLimit: Long) extends Sharry with Logging {

  folder.ensureDirectories()

  private val fileFilter =  (entry: Path) => Files.isRegularFile(entry) && FileName.tryParse(entry).isDefined

  private implicit def toFilter(ff: Path => Boolean) = new Filter[Path] {
    def accept(entry: Path) = ff(entry)
  }

  def addFiles(files: Iterable[Entry], owner: String, password: String, timeout: Option[Timeout]) = {
    require(owner != null && !owner.isEmpty, "Owner is required")
    require(password != null && password.length > 0, "password is required")
    require(!files.isEmpty, "No files to store.")
    if (folderSize >= folderSizeLimit) {
      Left(new IllegalStateException("Maximum folder size "+ ByteSize.bytes.normalizeString(folderSizeLimit) +" exceeded."))
    } else {
      val zip = FileIO.zipDir(FileIO.store(files))
      val checksum = createMd5(zip.getInput())
      val name = FileName(
        until = timeout.map(_.millis + System.currentTimeMillis()).getOrElse(0L),
        owner = owner,
        checksum = checksum,
        size = zip.fileSize
      )
      val file = folder / name.fullName
      SymmetricCrypt.encrypt(zip, file, password, createSha(password))
      zip.deleteIfExists()
      Right(name)
    }
  }

  def lookupFile(name: FileName) = folder /? name.fullName

  def decryptFile(name: FileName, password: String, out: OutputStream) {
    lookupFile(name) match {
      case Some(file) => {
        val md5 = MessageDigest.getInstance("MD5")
        val mdout = new DigestOutputStream(out, md5)
        SymmetricCrypt.decrypt(file, mdout, password, createSha(password))
        mdout.flush()
        val digest = DatatypeConverter.printHexBinary(md5.digest()).toLowerCase
        if (digest != name.checksum) {
          ioError("Decrypting failed.")
        }
      }
      case None => ioError("Cannot find file: "+ name)
    }
  }

  def decryptFile(name: FileName, password: String, target: Path) {
    lookupFile(name) match {
      case Some(file) => {
        SymmetricCrypt.decrypt(file, target, password, createSha(password))
        val digest = createMd5(target.getInput())
        if (digest != name.checksum) {
          ioError("Decrypting failed.")
        }
      }
      case None => ioError("Cannot find file: "+ name)
    }
  }

  def removeFiles(filter: FileName => Boolean) = {
    val counter = new AtomicInteger(0)
    listFiles.withFilter(filter).foreach { name =>
      (folder / name.fullName).deleteIfExists()
      counter.incrementAndGet()
    }
    counter.get()
  }

  def folderSize = {
    val size = new AtomicLong(0)
    folder.visitFiles(f => { size.addAndGet(f.fileSize); FileVisitResult.CONTINUE })
    size.get()
  }

  def listFiles = new Iterable[FileName] {
    def iterator: Iterator[FileName] = new FileIterator
  }

  private class FileIterator extends Iterator[FileName] {
    val stream = Files.newDirectoryStream(folder, fileFilter).iterator()
    def hasNext = stream.hasNext
    def next() = FileName(stream.next())
  }
}
