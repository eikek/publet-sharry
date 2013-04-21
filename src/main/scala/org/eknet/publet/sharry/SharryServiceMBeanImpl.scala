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

import com.google.inject.{Inject, Singleton}
import org.eknet.publet.vfs.util.ByteSize
import org.eknet.publet.web.Config
import org.quartz.Scheduler

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.04.13 17:31
 */
@Singleton
class SharryServiceMBeanImpl @Inject() (sharry: SharryService, config: Config, scheduler: Scheduler) extends SharryServiceMBean {

  private def sharryImpl = sharry.asInstanceOf[SharryServiceImpl]

  def getFolderSize = ByteSize.bytes.normalizeString(sharryImpl.sharry.folderSize)

  def getMaxUploadSize = ByteSize.bytes.normalizeString(maxUploadSize(config))

  def getArchiveCount = sharry.listArchives.size

  def removeOutdatedFiles() {
    scheduler.triggerJob(jobdef.getKey)
  }

  def removeAllArchives() {
    sharry.removeFiles(_ => true)
    sharryImpl.sharry.removeFiles(_ => true)
  }
}
