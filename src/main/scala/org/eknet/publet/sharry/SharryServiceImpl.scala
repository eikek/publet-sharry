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

import org.eknet.publet.sharry.lib.{FileName, Sharry, SharryImpl}
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import java.nio.file.{Files, Path}
import org.eknet.publet.ext.graphdb.{BlueprintGraph, GraphDb}
import org.eknet.publet.sharry.SharryService.{ArchiveInfo, AddResponse, AddRequest}
import java.security.SecureRandom
import com.tinkerpop.blueprints.Vertex
import java.util.UUID
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 17.04.13 22:01
 */
@Singleton
class SharryServiceImpl  @Inject()(@Named("sharryFolder") folder: Path,
                                   @Named("maxSharryFolderSize") maxSize: Long,
                                   @Named("sharry-db") db: GraphDb) extends SharryService {

  import SharryServiceImpl.randomPassword
  import SharryServiceImpl.randomId

  private val sharry: Sharry = new SharryImpl(folder, maxSize)
  private val filenameProp = "sharry-lib-filename"
  private val uniqueIdProp = "sharry-unique-id"

  for (prop <- List(filenameProp, uniqueIdProp)) {
    if (!db.graph.getIndexedKeys(classOf[Vertex]).contains(prop)) {
      db.graph.createKeyIndex(prop, classOf[Vertex])
    }
  }

  def addFiles(request: AddRequest) = {
    val req = request match {
      case AddRequest(_, _, pw, _, _) if (pw.isEmpty) => request.copy(password = randomPassword(18))
      case r => r
    }
    def createResponse(req: AddRequest, fn: FileName) = AddResponse(
      archive = fn,
      filename = req.filename.getOrElse(fn.checksum.take(10)+"."+fn.ext),
      password = req.password,
      id = randomId(10)
    )
    val resp = sharry.addFiles(req.files, req.owner, req.password, req.timeout)
      .right.map(createResponse(req, _))

    resp.right.foreach { saveResponse }
    resp
  }

  def decryptFile(name: FileName, password: String, out: OutputStream) {
    sharry.decryptFile(name, password, out)
  }

  def findArchive(name: String): Option[ArchiveInfo] = {
    import collection.JavaConversions._
    val prop = FileName.tryParse(name).map(_ => filenameProp).getOrElse(uniqueIdProp)
    db.withTx { g: BlueprintGraph =>
      g.getVertices(prop, name).headOption.map { v =>
        ArchiveInfo (
          archive = FileName(v.getProperty(filenameProp).asInstanceOf[String]),
          name = v.getProperty("givenName").asInstanceOf[String],
          id = v.getProperty(uniqueIdProp).asInstanceOf[String]
        )
      }
    }
  }

  def removeFiles(filter: ArchiveInfo => Boolean) = {
    val counter = new AtomicInteger(0)
    listArchives.withFilter(filter).foreach {ai =>
      sharry.lookupFile(ai.archive).map(f => Files.deleteIfExists(f))
      deleteNode(ai.id)
      counter.incrementAndGet()
    }
    counter.get()
  }

  def listArchives = sharry.listFiles.map(_.fullName).flatMap(findArchive)

  private def saveResponse(resp: AddResponse) {
    db.withTx { g:BlueprintGraph =>
      val v = g.addVertex()
      v.setProperty(filenameProp, resp.archive.fullName)
      v.setProperty(uniqueIdProp, resp.id)
      v.setProperty("givenName", resp.filename)
    }
  }

  private def deleteNode(id: String): Option[FileName] = {
    import collection.JavaConversions._
    db.withTx { g: BlueprintGraph =>
      g.getVertices(uniqueIdProp, id).headOption.map { v =>
        val fn = FileName(v.getProperty(filenameProp).asInstanceOf[String])
        g.removeVertex(v)
        fn
      }
    }
  }

}

object SharryServiceImpl {
  private val chars = List('-', '$', '+') ::: ('a' to 'z').toList ::: ('A' to 'Z').toList ::: ('0' to '9').toList
  private val random = new SecureRandom()

  private def randomPassword(len: Int) = {
    val pw = for (i <- 1 to len) yield chars(random.nextInt(chars.size))
    pw.toArray
  }

  private def randomId(len: Int) = new String(randomPassword(len))
}