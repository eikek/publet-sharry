package org.eknet.publet.sharry

import org.eknet.publet.web.guice.{AbstractPubletModule, PubletModule, PubletBinding}

class PubletSharryModule extends AbstractPubletModule with PubletBinding with PubletModule {

  def configure() {
    bind[PubletSharrySetup].asEagerSingleton()
  }

  val name = "Sharry"

  override val version = Reflect.version
}
