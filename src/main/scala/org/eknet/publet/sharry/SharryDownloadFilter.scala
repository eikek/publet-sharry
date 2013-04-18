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

import org.eknet.publet.web.req.{SuperFilter, RequestHandlerFactory}
import javax.servlet.http.HttpServletRequest
import org.eknet.publet.web.filter.Filters
import com.google.inject.Inject
import javax.inject.Named
import org.eknet.publet.vfs.Path
import org.eknet.publet.web.{PubletRequestWrapper, ReqUtils}
import javax.servlet.{FilterConfig, FilterChain, ServletResponse, ServletRequest, Filter}
import org.eknet.publet.Publet

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 18.04.13 10:23
 */
class SharryDownloadFilter @Inject() (@Named("sharryPath") path: Path, publet: Publet) extends RequestHandlerFactory {

  private lazy val downloadPath = path / "download"

  def getApplicableScore(req: HttpServletRequest) = {
    val util = new ReqUtils(req)
    if (util.applicationPath.prefixedBy(downloadPath)) {
      RequestHandlerFactory.EXACT_MATCH
    } else {
      RequestHandlerFactory.NO_MATCH
    }
  }

  def createFilter() = new SuperFilter(Seq(
    Filters.webContext,
    Filters.exceptionHandler,
    new DownloadForwardFilter,
    Filters.publet(publet)
  ))

  class DownloadForwardFilter extends Filter with PubletRequestWrapper {

    def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
      val id = request.applicationPath.segments.last
      val req = Filters.forwardRequest(request, path /"download.html", true)
      req.setAttribute(idKey.name, id)
      chain.doFilter(req, response)
    }

    def init(filterConfig: FilterConfig) {}
    def destroy() {}
  }
}
