package org.eknet.publet.sharry

import java.util.concurrent.TimeUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.02.13 22:51
 */
case class Timeout(value: Long, unit: TimeUnit) {

  lazy val millis = TimeUnit.MILLISECONDS.convert(value, unit)

}
