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

import org.eknet.publet.web.guice.{AbstractPubletModule, PubletModule, PubletBinding}
import org.eknet.publet.web.Config
import java.nio.file.{Path => JPath}
import com.google.inject.{Scopes, Singleton, Provides}
import org.eknet.publet.vfs.util.{UrlResource, ByteSize}
import com.google.inject.name.Named
import grizzled.slf4j.Logging
import org.eknet.publet.ext.graphdb.GraphDbProvider
import org.eknet.publet.vfs.{Resource, Path}
import org.eknet.publet.web.util.Key
import org.eknet.publet.sharry.test.TestUserStore
import org.eknet.publet.auth.store.{UserStore, PermissionStore}

class PubletSharryModule extends AbstractPubletModule with PubletBinding with PubletModule with Logging {

  private val defaultFolderSize = ByteSize.mib.toBytes(200)

  def configure() {
    bind[PubletSharrySetup].asEagerSingleton()
    bind[SharryService].to[SharryServiceImpl]
    bindRequestHandler.add[SharryDownloadFilter]
    bindRequestHandler.add[SharryUserUploadFilter]
    bindRequestHandler.add[OpenScriptHandler]

    bindDocumentation(List(doc("sharry.md")))

    bind[TestUserStore]
    setOf[UserStore].add[TestUserStore].in(Scopes.SINGLETON)
    setOf[PermissionStore].add[TestUserStore].in(Scopes.SINGLETON)
  }

  def doc(name: String) = Resource.classpath("org/eknet/publet/sharry/doc/"+name)

  @Provides@Named("sharryPath")
  def bindSharryPath(config: Config) = Path(config("sharry.path").getOrElse("/sharry"))

  @Provides@Named("sharryFolder")
  def bindFolder(config: Config): JPath = config.workDir("sharry-folder").toPath

  @Provides@Named("maxSharryFolderSize")
  def bindFolderSize(config: Config): Long = config("sharry.maxFolderSize") match {
    case Some(x) => parseSize(x).getOrElse(defaultFolderSize)
    case None => defaultFolderSize
  }

  @Provides@Singleton@Named("sharry-db")
  def sharryDb(dbprovider: GraphDbProvider) = {
    dbprovider.getDatabase("sharry-db")
  }

  private[this] def parseSize(size: String) = {
    val sizeRegex = """((\d+)(\.\d+)?)(.*)""".r
    size match {
      case sizeRegex(num, i, p, unit) => {
        if (unit.isEmpty) Some(i.toLong)
        else Some(ByteSize.fromString(unit).toBytes(num.toDouble))
      }
      case _ => {
        warn("Cannot parse folder size string: "+ size+ "! Using default.")
        None
      }
    }
  }

  val name = "Sharry"

  override val version = Reflect.version
}
