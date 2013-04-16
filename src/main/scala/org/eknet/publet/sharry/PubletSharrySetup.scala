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
import org.eknet.publet.vfs.{ContentType, ResourceName, ContentResource, Path}
import org.eknet.publet.Publet
import org.quartz.{DateBuilder, Scheduler}
import org.eknet.publet.quartz.QuartzDsl
import org.quartz.DateBuilder.IntervalUnit
import org.eknet.publet.web.scripts.WebScriptResource
import org.eknet.publet.sharry.ui.{DownloadHandler, UploadHandler}
import org.eknet.publet.webeditor.{Assets => EditorAssets}
import org.eknet.publet.web.{ErrorResponse, CustomContent}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

@Singleton
class PubletSharrySetup @Inject() (publet: Publet, assetMgr: AssetManager, scheduler: Scheduler) extends AssetCollection with QuartzDsl {

  override def classPathBase = "/org/eknet/publet/sharry/includes"

  @Subscribe
  def mountResources(event: PubletStartedEvent) {
    import org.eknet.publet.vfs.ResourceName.string2ResourceName

    val sharryAssets = Group("publet-sharry.assets")
      .add(resource("js/jquery.sharry.js"))
      .add(resource("js/sharry.startup.js"))
      .require(DefaultLayout.Assets.bootstrap.name, DefaultLayout.Assets.jquery.name, DefaultLayout.Assets.mustache.name)

    val sharryAssetsConfigured = Group("publet-sharry.assets.configured")
      .forPath("/sharry/**")
      .use(sharryAssets.name)

    assetMgr setup (sharryAssets, sharryAssetsConfigured, Assets.blueimpFileUpload)
   
    assetMgr setup
      Group("default").use(sharryAssetsConfigured.name)

    val cont = new ClasspathContainer(base = "/org/eknet/publet/sharry/includes/templ")
    publet.mountManager.mount(Path("/sharry"), cont)

    val scripts = new MapContainer
    scripts.addResource(new WebScriptResource("upload.json".rn, new UploadHandler))
    scripts.addResource(new DownloadHandler("download".rn))
    publet.mountManager.mount(Path("/sharry/actions"), scripts)
  }

  @Subscribe
  def scheduleDeletionJob(ev: PubletStartedEvent) {
    val jobdef = newJob[FileDeleteJob]
      .withIdentity("file-delete" in "sharry")
      .withDescription("Removes outdated shared files")
      .build()
    val trigger = newTrigger
      .withIdentity("daily-file-delete" in "sharry")
      .forJob(jobdef)
      .withSchedule(simpleSchedule.withIntervalInHours(24).repeatForever())
      .startAt(DateBuilder.futureDate(2, IntervalUnit.HOUR))
      .build()

    scheduler.scheduleJob(jobdef, trigger)
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
