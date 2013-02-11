package org.eknet.publet.sharry.files

import org.apache.commons.fileupload.FileItem
import java.nio.file._
import java.util.zip.{ZipEntry, ZipOutputStream}
import com.google.common.io.{InputSupplier, OutputSupplier, ByteStreams}
import java.io.{InputStream, OutputStream}
import org.eknet.publet.sharry.Entry

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
   * @param directory the directory to zip
   * @return the path to the zip file (sibling to the directory)
   */
  def zipDir(directory: Path): Path = {
    val zipfile = directory.getParent / (directory.getFileName.toString + ".zip")
    val zipout = new ZipOutputStream(zipfile.getOutput(StandardOpenOption.CREATE_NEW))
    zipout.exec {
      directory.visitFiles(addZipEntry(zipout, _))
    }
    directory.deleteTree()
    zipfile
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
    SymmetricCrypt.encrypt(zip, targetFile, password.toCharArray)
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
    SymmetricCrypt.decrypt(encryptedFile, tempFile, password.toCharArray)
    tempFile
  }

}
