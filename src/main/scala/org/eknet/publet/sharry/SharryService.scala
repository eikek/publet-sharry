package org.eknet.publet.sharry

import java.io.OutputStream
import java.nio.file.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 22:42
 */
trait SharryService {

  /**
   * Adds the given files by zipping them into one archive and encrypting this
   * archive. The file name is returned that can be used to lookup the file.
   *
   * @param files
   * @param password
   * @param timeout
   * @return
   */
  def addFiles(files: Iterable[Entry], owner: String, password: String, timeout: Timeout): FileName

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
   */
  def removeFiles(filter: FileName => Boolean)
}
