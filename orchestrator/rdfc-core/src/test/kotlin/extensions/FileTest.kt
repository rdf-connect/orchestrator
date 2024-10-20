package technology.idlab.rdfc.core.extensions

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FileTest {
  @Test
  fun empty() {
    val file = File("")
    assertEquals("", file.rawPath())
  }

  @Test
  fun slashes() {
    val file = File("/////")
    assertEquals("/", file.rawPath())
  }

  @Test
  fun filePrefix() {
    val file = File("file:/home/admin")
    assertEquals("/home/admin", file.rawPath())
  }

  @Test
  fun filePrefixDoubleSlash() {
    val file = File("file://home/admin")
    assertEquals("/home/admin", file.rawPath())
  }
}
