package org.eknet.publet.sharry.lib

import java.io.{IOException, ByteArrayOutputStream}
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream
import org.eknet.publet.sharry.lib
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable.ListBuffer

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.03.13 23:35
 */
class SharrySuite extends FunSuite with ShouldMatchers {

  import Timeout._

  val textfile1 = new Entry {
    def name = "textfile"
    def inputStream = classOf[SharrySuite].getResourceAsStream("/textfile1.txt")
  }
  val textfile2 = new Entry {
    def name = "textfile2.txt"
    def inputStream = classOf[SharrySuite].getResourceAsStream("/textfile2.txt")
  }

  test ("valid encrypting/decrypting") {
    val testfolder = Paths.get("target", "test-crypt")
    testfolder.ensureDirectories()
    testfolder.deleteTree()
    val sharry = new SharryImpl(testfolder, 1)
    val result = sharry.addFiles(Seq(textfile1, textfile2), "eike", "testpw", Some(2.seconds))

    val name = result.fold(e => throw e, identity)
    val output = testfolder.resolve("testout.zip")
    output.deleteIfExists()
    sharry.decryptFile(name, "testpw", output)
    sharry.decryptFile(name, "testpw", new ByteArrayOutputStream())

    intercept[IOException] {
      sharry.decryptFile(name, "testpwWRONG", output)
    }
    intercept[IOException] {
      sharry.decryptFile(name, "testpwXZ", new ByteArrayOutputStream())
    }

    val zin = new ZipInputStream(output.getInput())
    var entry = zin.getNextEntry
    val counter = new AtomicInteger(0)
    while (entry != null) {
      counter.incrementAndGet() should (be > (0) and be < (3))
      entry.getName should (be (textfile1.name) or (be (textfile2.name)))
      entry = zin.getNextEntry
    }
    output.deleteIfExists()
    sharry.lookupFile(name) should not be (None)
    sharry.lookupFile(name).get.deleteIfExists()
  }


}
