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

import org.eknet.publet.quartz.QuartzJob
import org.quartz.JobExecutionContext
import grizzled.slf4j.Logging
import org.eknet.publet.sharry.lib.FileName
import org.eknet.publet.sharry.SharryService.ArchiveInfo
import com.google.inject.Inject

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 12.02.13 08:15
 * 
 */
class FileDeleteJob @Inject() (sharry: SharryService) extends QuartzJob with Logging {

  def perform(context: JobExecutionContext) {
    val nofiles = sharry.removeFiles(FileName.outdated)
    info("Removed "+ nofiles +" shared files")
  }

  implicit def toFilter(f: FileName => Boolean): ArchiveInfo => Boolean = ai => f(ai.archive)
}
