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

import java.io.OutputStream
import java.nio.file.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 22:42
 */
trait Sharry {

  /**
   * Adds the given files by zipping them into one archive and encrypting this
   * archive. The file name is returned that can be used to lookup the file.
   *
   * @param files the files to add
   * @param password the password used to encrypt the archive
   * @param timeout how long the file is valid. after this timeout, the file can
   *                will be subject to deletion
   * @return
   */
  def addFiles(files: Iterable[Entry], owner: String, password: Array[Char], timeout: Option[Timeout]): Either[Exception, FileName]

  /**
   * Looks up the encrypted archive.
   *
   * @param name
   * @return
   */
  def lookupFile(name: FileName): Option[Path]

  /**
   * Looks up the encrypted archive and decrypts it into the given
   * output stream.
   *
   * @param name
   * @param password
   * @param out
   */
  def decryptFile(name: FileName, password: String, out: OutputStream)

  /**
   * Convenience method that decrypts the archive and stores it
   * into a file at the given path. The `target` file must not
   * exist and it will contain the unencrypted archive. Client
   * code must take care of access/deletion.
   *
   * @param name
   * @param password
   * @param target
   */
  def decryptFile(name: FileName, password: String, target: Path)

  /**
   * Removes all files that match the given filter.
   *
   * @param filter
   * @return the number of files deleted
   */
  def removeFiles(filter: FileName => Boolean): Int


  /**
   * Lists all files currently available.
   *
   * @return
   */
  def listFiles: Iterable[FileName]

  /**
   * Returns the size the folder currently occupies.
   *
   * @return
   */
  def folderSize: Long

  def folderSizeLimit: Long

}
