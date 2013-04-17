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

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.DirectoryStream.Filter
import java.nio.charset.Charset
import scala.Some
import java.nio.file.{FileVisitResult, SimpleFileVisitor, OpenOption, StandardCopyOption, AtomicMoveNotSupportedException, CopyOption, Files, Path}
import java.io.{BufferedInputStream, IOException, InputStreamReader, BufferedReader, OutputStreamWriter, BufferedWriter, OutputStream, InputStream}
import java.security.{DigestInputStream, MessageDigest}
import javax.xml.bind.DatatypeConverter

/**
 * This package contains the base of the sharry functionality, not depending
 * on other publet modules.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 11.02.13 22:07
 */
package object lib {

  implicit def decoratePath(p: Path) = new DecoratedPath(p)
  implicit def undecoratePath(dp: DecoratedPath) = dp.path
  implicit def pathToFile(p: Path) = p.toFile

  class DecoratedPath(val path: Path) {
    def notExists = Files.notExists(path)
    def exists = Files.exists(path)
    def createDirectory = Files.createDirectory(path)
    def createDirectories = Files.createDirectories(path)
    def createFile = Files.createFile(path)
    def delete() { Files.delete(path) }
    def deleteIfExists() = Files.deleteIfExists(path)
    def isDirectory = Files.isDirectory(path)
    def isFile = Files.isRegularFile(path)

    def / (p: Path) = path.resolve(p)
    def / (p: String) = path.resolve(p)

    def /? (p:Path) = /(p) match {
      case np if (Files.exists(np)) => Some(np)
      case _ => None
    }
    def /? (p: String) = /(p) match {
      case np if (Files.exists(np)) => Some(np)
      case _ => None
    }

    def isEmpty = if (isFile) {
      path.toUri.toURL.openStream().available() > 0
    } else if (isDirectory) {
      val ds = Files.newDirectoryStream(path)
      ds.iterator().hasNext
    } else {
      ioError("Cannot determine emptiness for "+ path)
    }

    def readFrom(in: InputStream, opts: CopyOption*) = Files.copy(in, path, opts: _*)
    def writeTo(out: OutputStream) = Files.copy(path, out)
    def copyTo(other: Path, options: CopyOption*) = Files.copy(path, other, options: _*)
    def moveTo(other: Path, options: CopyOption*) = Files.move(path, other, options: _*)

    /**
     * Same as `moveTo(Path, CopyOption*)` but it handles [[java.nio.file.AtomicMoveNotSupportedException]]
     * and retries without this options. It also handles [[java.lang.UnsupportedOperationException]] and
     * retries without any copy options.
     *
     * @param other
     * @param options
     * @return
     */
    def moveToLenient(other: Path, options: CopyOption*) = {
      try {
        moveTo(other, options: _*)
      } catch {
        case e: AtomicMoveNotSupportedException => moveTo(other, options.filter(_ != StandardCopyOption.ATOMIC_MOVE): _*)
        case e: UnsupportedOperationException => moveTo(other)
      }
    }

    def getOutput(options: OpenOption*) = Files.newOutputStream(path, options: _*)
    def getWriter(options: OpenOption*) = new BufferedWriter(new OutputStreamWriter(getOutput(options: _*)))
    def getInput(options: OpenOption*) = Files.newInputStream(path, options: _*)
    def getReader(options: OpenOption*) = new BufferedReader(new InputStreamReader(getInput(options: _*)))

    def list(glob: String): Iterable[Path] = {
      import collection.JavaConversions._
      Files.newDirectoryStream(path, glob)
    }
    def list(): Iterable[Path] = {
      import collection.JavaConversions._
      Files.newDirectoryStream(path)
    }
    def list(filter: Path => Boolean):Iterable[Path] = {
      import collection.JavaConversions._
      Files.newDirectoryStream(path, new Filter[Path] {
        def accept(entry: Path) = filter(entry)
      })
    }

    def deleteTree() = {
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException) = {
          if (exc == null) {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          } else {
            throw exc
          }
        }
      })
    }

    def ensureFile() = {
      notExists match {
        case true => createFile
        case _ => isFile match {
          case true => path
          case _ => ioError("Path '"+path+"' exists but is not a file!")
        }
      }
    }

    def ensureDirectory() = {
      notExists match {
        case true => createDirectory
        case _ => isDirectory match {
          case true => path
          case _ => ioError("Path '"+ path +"' exists but is not a directory.")
        }
      }
    }

    def ensureDirectories() = {
      notExists match {
        case true => createDirectories
        case _ => ensureDirectory()
      }
    }

    def getLines = {
      import collection.JavaConversions._
      Files.readAllLines(path, Charset.defaultCharset()).toList
    }

    def findFile(filter: Path => Boolean) = {
      var result: Option[Path] = None
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          filter(file) match {
            case true => {
              result = Some(file)
              FileVisitResult.TERMINATE
            }
            case _ => FileVisitResult.CONTINUE
          }
        }
      })
      result
    }

    def visitFiles(f: Path => FileVisitResult) {
      Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          f(file)
        }
      })
    }

    def lastModifiedTime = Files.getLastModifiedTime(path)
    def fileSize = Files.size(path)
  }

  def ioError(msg: String, cause: Throwable = null) = Option(cause) match {
    case Some(c) => throw new IOException(msg, c)
    case None => throw new IOException(msg)
  }



  class RicherClosable(val ac: AutoCloseable) {

    def exec[A](body: => A): A = {
      var ex: Option[Exception] = None
      try {
        body
      } catch {
        case e: Exception => {
          ex = Some(e)
          throw e
        }
      } finally {
        try {
          ac.close()
        } catch {
          case e: IOException => {
            ex.map(_.addSuppressed(e))
          }
        }
      }
    }
  }

  implicit def enrichtClosable(ac: AutoCloseable) = new RicherClosable(ac)
  implicit def unrichClosable(rc: RicherClosable) = rc.ac

  def createMd5(in: InputStream) = {
    val md5 = MessageDigest.getInstance("MD5")
    val mdin = new BufferedInputStream(new DigestInputStream(in, md5))
    while (mdin.read() != -1) {}
    mdin.close()
    DatatypeConverter.printHexBinary(md5.digest()).toLowerCase
  }
}
