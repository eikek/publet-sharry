package org.eknet.publet.sharry

import java.io.InputStream
import org.apache.commons.fileupload.FileItem
import java.nio.file.{Files, Path}

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
