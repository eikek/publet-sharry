package org.eknet.publet.sharry

import grizzled.slf4j.Logging
import java.nio.file.{FileVisitResult, Path}
import java.io.OutputStream

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 00:31
 */
class SharryServiceImpl(folder: Path) extends SharryService with Logging {

  import files._
  folder.ensureDirectories()

  def addFiles(files: Iterable[Entry], owner: String, password: String, timeout: Timeout) = {
    val name = FileName(until = System.currentTimeMillis()+ timeout.millis, owner = owner)
    val file = folder / name.fullName
    FileIO.storeAndEncryptFiles(files, password, file)
    name
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

  def removeFiles(filter: FileName => Boolean) {
    folder.visitFiles(file => {
      if (filter(FileName(file))) {
        file.deleteIfExists()
      }
      FileVisitResult.CONTINUE
    })
  }
}
