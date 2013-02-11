package org.eknet.publet.sharry

import _root_.com.google.common.eventbus.Subscribe
import _root_.com.google.inject.{Inject, Singleton}
import org.eknet.publet.web.guice.PubletStartedEvent
import org.eknet.publet.web.template.DefaultLayout
import org.eknet.publet.web.asset.{Group, AssetManager, AssetCollection}
import org.eknet.publet.vfs.util.ClasspathContainer
import org.eknet.publet.vfs.Path
import org.eknet.publet.Publet

@Singleton
class PubletSharrySetup @Inject() (publet: Publet, assetMgr: AssetManager) extends AssetCollection {

  override def classPathBase = "/org/eknet/publet/sharry/includes"

  @Subscribe
  def mountResources(event: PubletStartedEvent) {

    val sharryAssets = Group("publet-sharry.assets")
      .add(resource("js/jquery.sharry.js"))
      .require(DefaultLayout.Assets.bootstrap.name, DefaultLayout.Assets.jquery.name, DefaultLayout.Assets.mustache.name)

    val sharryAssetsConfigured = Group("publet-sharry.assets.configured")
      .forPath("/sharry/**")
      .use(sharryAssets.name)

    assetMgr setup (sharryAssets, sharryAssetsConfigured)
   
    assetMgr setup
      Group("default").use(sharryAssetsConfigured.name)

    val cont = new ClasspathContainer(base = "/org/eknet/publet/sharry/includes/templ")
    publet.mountManager.mount(Path("/sharry"), cont)
  }
}
