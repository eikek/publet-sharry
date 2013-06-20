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

import java.nio.file._
import java.util.zip.{ZipEntry, ZipOutputStream}
import com.google.common.io.ByteStreams
import java.io.InputStream

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 21:40
 */
object FileIO {

  /**
   * Creates a new temporary directory and writes all files into it.
   *
   * @param items the files to write to disk
   * @return the directory containing all files
   */
  def store(items: Iterable[Entry]): Path = {
    val tempdir = Files.createTempDirectory("sharry-files")
    items foreach { item =>
      val in = item.inputStream
      in.exec {
        (tempdir / item.name).readFrom(in)
      }
    }
    tempdir
  }

  /**
   * Zips the given directory into a single zip file next to the directory. The
   * directory will be deleted afterwards.
   *
   * If the directory contains exactly one file with extension `zip`, it is just
   * moved.
   *
   * @param directory the directory to zip
   * @return the path to the zip file (sibling to the directory)
   */
  def zipDir(directory: Path): Path = {
    val zipfile = directory.getParent / (directory.getFileName.toString + ".zip")
    val zipout = new ZipOutputStream(zipfile.getOutput(StandardOpenOption.CREATE_NEW))
    findSingle(directory, "*.zip") match {
      case Some(zipf) => zipf.moveTo(zipfile, StandardCopyOption.REPLACE_EXISTING)
      case _ => {
        zipout.exec {
          directory.visitFiles(addZipEntry(zipout, _))
        }
      }
    }
    directory.deleteTree()
    zipfile
  }

  private def findSingle(directory: Path, glob: String) = {
    val iter = directory.list(glob).iterator
    if (iter.hasNext) {
      val f = iter.next()
      if (iter.hasNext) None else Some(f)
    } else {
      None
    }
  }

  def zipToDir(files: Iterable[Entry], zipFile: Path) {
    val zipout = new ZipOutputStream(zipFile.getOutput(StandardOpenOption.CREATE_NEW))
    zipout.exec {
      files.foreach(entry => {
        val in = entry.inputStream
        in.exec {
          addZipEntry(zipout, in, entry.name)
        }
      })
    }
  }

  private[this] def addZipEntry(out: ZipOutputStream, file: Path) = {
    out.putNextEntry(new ZipEntry(file.getFileName.toString))
    val in = file.getInput()
    in.exec {
      ByteStreams.copy(in, out)
    }
    FileVisitResult.CONTINUE
  }
  private[this] def addZipEntry(out: ZipOutputStream, file: InputStream, name: String) {
    out.putNextEntry(new ZipEntry(name))
    ByteStreams.copy(file, out)
  }

  /**
   * Stores all file items to a directory on disk, then zips the directory
   * and encrypts it using the given password. After that the directory and
   * unencrypted zip file are deleted.
   *
   * @param items
   * @param password
   * @return
   */
  def storeAndEncryptFiles(items: Iterable[Entry], password: String, targetFile: Path) {
    val zip = zipDir(store(items))
    SymmetricCrypt.encrypt(zip, targetFile, password, createSha(password))
    zip.deleteIfExists()
  }

  /**
   * Decrypts the given file and returns the path to the temporary file
   * containing the decrypted content.
   *
   * @param encryptedFile
   * @param password
   */
  def decryptFile(encryptedFile: Path, password: String) = {
    val tempFile = Files.createTempFile("sharry-dec", ".zip")
    SymmetricCrypt.decrypt(encryptedFile, tempFile, password, createSha(password))
    tempFile
  }

}
