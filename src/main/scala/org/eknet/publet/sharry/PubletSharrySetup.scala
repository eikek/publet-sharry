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

import _root_.com.google.common.eventbus.Subscribe
import _root_.com.google.inject.{Inject, Singleton}
import org.eknet.publet.web.guice.PubletStartedEvent
import org.eknet.publet.web.template.DefaultLayout
import org.eknet.publet.web.asset.{Group, AssetManager, AssetCollection}
import org.eknet.publet.vfs.util.{MapContainer, ClasspathContainer}
import org.eknet.publet.vfs.Path
import org.eknet.publet.Publet
import org.quartz.{DateBuilder, Scheduler}
import org.eknet.publet.quartz.QuartzDsl
import org.quartz.DateBuilder.IntervalUnit
import org.eknet.publet.web.scripts.WebScriptResource
import org.eknet.publet.sharry.ui.{AliasManage, ArchiveManage, MailSender, DownloadHandler, UploadHandler}
import org.eknet.publet.webeditor.{Assets => EditorAssets}
import com.google.inject.name.Named
import org.eknet.publet.web.{ConfigReloadedEvent, Config}

@Singleton
class PubletSharrySetup @Inject() (publet: Publet, assetMgr: AssetManager, scheduler: Scheduler, @Named("sharryPath") path: Path, config: Config) extends AssetCollection with QuartzDsl {

  override def classPathBase = "/org/eknet/publet/sharry/includes"

  @Subscribe
  def mountResources(event: PubletStartedEvent) {
    import org.eknet.publet.vfs.ResourceName.string2ResourceName

    val sharryAssets = Group("publet-sharry.assets")
      .add(resource("js/jquery.sharry.js"))
      .add(resource("js/jquery.sharry-archives.js"))
      .add(resource("js/jquery.sharry-alias.js"))
      .add(resource("js/sharry.startup.js"))
      .require(DefaultLayout.Assets.bootstrap.name)
      .require(DefaultLayout.Assets.jquery.name)
      .require(DefaultLayout.Assets.mustache.name, Assets.blueimpFileUpload.name)

    assetMgr setup (sharryAssets, Assets.blueimpFileUpload)
   
    val cont = new ClasspathContainer(base = "/org/eknet/publet/sharry/includes/templ")
    publet.mountManager.mount(path, cont)

    val scripts = new MapContainer
    scripts.addResource(new WebScriptResource("sharemail.json".rn, new MailSender))
    scripts.addResource(new WebScriptResource("listarchives.json".rn, new ArchiveManage))
    scripts.addResource(new WebScriptResource("managealias.json".rn, new AliasManage))
    publet.mountManager.mount(path / "actions", scripts)

    val open = new MapContainer
    open.addResource(new WebScriptResource("upload.json".rn, new UploadHandler))
    open.addResource(new DownloadHandler("download".rn))
    publet.mountManager.mount(path / "open", open)
  }

  private def createTrigger = {
    val interval = config("sharry.deleteJobInterval").map(_.toInt).getOrElse(24)
    newTrigger
      .withIdentity("daily-file-delete" in "sharry")
      .forJob(jobdef)
      .withSchedule(simpleSchedule.withIntervalInHours(interval).repeatForever())
      .startAt(DateBuilder.futureDate(2, IntervalUnit.HOUR))
      .build()
  }

  @Subscribe
  def scheduleDeletionJob(ev: PubletStartedEvent) {
    scheduler.scheduleJob(jobdef, createTrigger)
  }

  @Subscribe
  def rescheduleJob(e: ConfigReloadedEvent) {
    scheduler.rescheduleJob("daily-file-delete" in "sharry", createTrigger)
  }

  private object Assets extends AssetCollection {
    override def classPathBase = "/org/eknet/publet/webeditor/includes"


    val blueimpFileUpload = Group("sharry.blueimp.fileupload")
      .add(resource("img/loading.gif"))
      .add(resource("img/progressbar.gif"))
      .add(resource("img/publet_nopreview.png"))
      .add(resource("css/jquery.fileupload-ui.css"))
      .add(resource("js/jquery.fileupload.js"))
      .add(resource("js/jquery.fileupload-fp.js"))
      .add(resource("js/jquery.fileupload-ui.js"))
      .add(resource("js/locale.js"))
      .require(EditorAssets.jqueryIframeTransport.name, EditorAssets.jqueryUiWidget.name,
      EditorAssets.blueimpTmpl.name, EditorAssets.blueimpLoadImage.name, DefaultLayout.Assets.jquery.name)
  }
}
