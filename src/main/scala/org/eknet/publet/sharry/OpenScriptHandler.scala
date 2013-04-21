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

import com.google.inject.Inject
import javax.inject.Named
import org.eknet.publet.vfs.Path
import org.eknet.publet.Publet
import org.eknet.publet.web.req.{SuperFilter, RequestHandlerFactory}
import javax.servlet.http.HttpServletRequest
import org.eknet.publet.web.ReqUtils
import org.eknet.publet.web.filter.Filters

/**
 * Filter that only authenticates but does no authorization, such that the
 * scripts can be accessed by anonymous users as well as with authenticated
 * ones.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 19.04.13 23:24
 */
class OpenScriptHandler @Inject() (@Named("sharryPath") path: Path, publet: Publet) extends RequestHandlerFactory {
  private lazy val openPath = path / "open"

  def getApplicableScore(req: HttpServletRequest) = {
    val util = new ReqUtils(req)
    if (util.applicationPath.prefixedBy(openPath)) {
      RequestHandlerFactory.EXACT_MATCH
    } else {
      RequestHandlerFactory.NO_MATCH
    }
  }

  def createFilter() = new SuperFilter(Seq(
    Filters.webContext,
    Filters.exceptionHandler,
    Filters.authc,
    Filters.publet(publet)
  ))

}
