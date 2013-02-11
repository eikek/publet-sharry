package org.eknet.publet.sharry

import java.util.UUID
import scala.util.parsing.combinator.RegexParsers
import grizzled.slf4j.Logging
import java.nio.file.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 00:03
 */
case class FileName(time: Long = System.currentTimeMillis(), until: Long = System.currentTimeMillis(), owner: String, unique: String = FileName.uniqueString, ext: String = "zip") {

  val fullName = {
    val buf = new StringBuilder
    buf.append(time).append(".")
    buf.append(until).append(".")
    buf.append(unique).append(".")
    buf.append(owner).append(".")
    buf.append(ext)
    buf.toString()
  }

  lazy val date = new java.util.Date(time)

}

object FileName extends Logging {

  def apply(name: Path): FileName = FileName(name.getFileName.toString)

  def apply(name: String): FileName = {
    try {
      Parser.parseAll(Parser.filename, name).get
    } catch {
      case e: RuntimeException => throw new RuntimeException("Parsing filename failed: "+ name, e)
    }
  }

  def tryParse(name: String) = {
    try {
      Some(Parser.parseAll(Parser.filename, name).get)
    } catch {
      case e: RuntimeException => {
        warn("Invalid filename encountered: "+ name)
        None
      }
    }
  }

  def uniqueString = UUID.randomUUID().toString.replace("-", "")

  val outdated: FileName => Boolean = _.until <= System.currentTimeMillis()

  private object Parser extends RegexParsers {

    def filename = timestamp ~"."~ timestamp ~"."~ unique ~"."~ owner ~"."~ ext ^^ {
      case added ~"."~ until ~"."~ uniq ~"."~ login ~"."~ extension => new FileName(added, until, uniq, login, extension)
      case x@_ => sys.error("Wrong file name: "+ x)
    }

    private val timestamp = "[0-9]+".r ^^ (_.toLong)
    private val owner = "[\\w]+".r
    private val unique = "[\\w]+".r
    private val ext = "[a-zA-Z0-9]+".r
  }

}
