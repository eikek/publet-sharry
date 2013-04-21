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

import org.eknet.publet.sharry.lib.{Timeout, FileName, Sharry, SharryImpl}
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import java.nio.file.{Files, Path}
import org.eknet.publet.ext.graphdb.{BlueprintGraph, GraphDb}
import org.eknet.publet.sharry.SharryService.{Alias, ArchiveInfo, AddResponse, AddRequest}
import java.security.SecureRandom
import com.tinkerpop.blueprints.{Direction, Vertex}
import java.util.UUID
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import org.eknet.scue.GraphDsl

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 17.04.13 22:01
 */
@Singleton
class SharryServiceImpl  @Inject()(@Named("sharryFolder") folder: Path,
                                   @Named("sharry.maxFolderSize") maxSize: Long,
                                   @Named("sharry-db") db: GraphDb) extends SharryService with GraphDsl {

  private implicit val graph = db.graph

  private val sharry: Sharry = new SharryImpl(folder, maxSize)
  private val filenameProp = "sharry-lib-filename"
  private val uniqueIdProp = "sharry-unique-id"
  private val loginProp = "sharry-username"
  private val aliasProp = "sharry-username-alias"
  private val givenName = "sharry-givenName"
  private val senderProp = "sharry-sender"

  for (prop <- List(filenameProp, uniqueIdProp, loginProp, aliasProp)) {
    if (!db.graph.getIndexedKeys(classOf[Vertex]).contains(prop)) {
      db.graph.createKeyIndex(prop, classOf[Vertex])
    }
  }

  def addFiles(request: AddRequest) = {
    val req = request.password match {
      case pw if (pw.length==0) => request.copy(password = RandomId.generatePassword(18))
      case _ => request
    }
    def createResponse(req: AddRequest, fn: FileName) = AddResponse(
      archive = fn,
      filename = req.filename.getOrElse(fn.checksum.take(10)+"."+fn.ext),
      password = req.password,
      id = RandomId.generate(10, 15),
      sender = req.sender.getOrElse(fn.owner)
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
    val prop = FileName.tryParse(name).map(_ => filenameProp).getOrElse(uniqueIdProp)
    withTx {
      singleVertex(prop := name) map { v =>
        ArchiveInfo(
          archive = FileName(v.get[String](filenameProp).get),
          name = v.get[String](givenName).get,
          id = v.get[String](uniqueIdProp).get,
          sender = v.get[String](senderProp).get
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

  def listArchives = sharry.listFiles.map(_.fullName).flatMap(findArchive).toList.sortBy(- _.archive.time)

  private def saveResponse(resp: AddResponse) {
    withTx {
      val v = newVertex
      v(filenameProp) = resp.archive.fullName
      v(uniqueIdProp) = resp.id
      v(givenName) = resp.filename
      v(senderProp) = resp.sender
    }
  }

  private def deleteNode(id: String): Option[FileName] = {
    withTx {
      singleVertex(uniqueIdProp := id).map { v =>
        val fn = FileName(v.get[String](filenameProp).get)
        graph.removeVertex(v)
        fn
      }
    }
  }

  private def aliasToNode(alias: Alias, v: Vertex) {
    v(aliasProp) = alias.name
    v("enabled") = alias.enabled
    v("notification") = alias.notification
    alias.timeout.map { to => v("timeout") = to.millis }
    if (!alias.defaultPassword.isEmpty) {
      v("defaultPassword") = new String(alias.defaultPassword)
    }
  }
  private def nodeToAlias(v: Vertex): Alias = withTx {
    import Timeout._
    Alias(
      name = v.get[String](aliasProp).get,
      enabled = v.get[Boolean]("enabled").getOrElse(false),
      notification = v.get[Boolean]("notification").getOrElse(false),
      timeout = v.get[Long]("timeout").map(_.millis),
      defaultPassword = v.get[String]("defaultPassword").getOrElse("").toCharArray
    )
  }

  def updateAlias(login: String, alias: Alias) {
    withTx {
      val vlogin = vertex(loginProp := login)
      val valias = vertex(aliasProp := alias.name, v => {
        vlogin --> "alias" --> v
      })
      aliasToNode(alias, valias)
    }
  }


  def removeAlias(alias: String) {
    withTx {
      singleVertex(aliasProp := alias).map { v =>
        graph.removeVertex(v)
      }
    }
  }

  def findUser(alias: String) = withTx {
    singleVertex(aliasProp := alias).filter(v => v("enabled") == Some(true)).flatMap {v =>
      (v -<- "alias").mapEnds(lv => lv.get[String](loginProp).get).headOption
    }
  }


  def findAlias(alias: String) = withTx {
    singleVertex(aliasProp := alias).map(nodeToAlias)
  }

  def listAliases(login: String) = {
    singleVertex(loginProp := login).map { v =>
      v ->-() mapEnds(nodeToAlias)
    } getOrElse(Seq())
  }
}
