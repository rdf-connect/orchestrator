package parser

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import runner.Runner
import technology.idlab.parser.Parser
import technology.idlab.parser.intermediate.IRParameter

abstract class ParserTest {
  // The parser to test.
  abstract val parser: Parser

  @Test
  fun processors() {
    val processors = parser.processors()
    assertEquals(2, processors.size, "There should be two processors.")

    // Get alpha processor and check its values.
    val alpha = processors.find { it.uri.endsWith("alpha") }
    assertNotNull(alpha, "Alpha processor should exist.")
    assertContains(alpha.metadata.keys, "class", "Alpha processor should have a class key.")
    assertEquals(
        "Alpha",
        alpha.metadata["class"],
        "Alpha processor should have a class key with value Alpha.")
    assertEquals(Runner.Target.JVM, alpha.target, "Alpha processor should target JVM.")
    assertEquals(2, alpha.parameters.size, "Alpha processor should have two parameters.")

    // Check its arguments.
    val one = alpha.parameters.find { it.name == "one" }
    assertNotNull(one, "Parameter one should exist.")
    assertEquals(IRParameter.Type.STRING, one.type, "Parameter one should be of type string.")
    assertEquals(IRParameter.Count.SINGLE, one.count, "Parameter one should be a single value.")
    assertEquals(IRParameter.Presence.REQUIRED, one.presence, "Parameter one should be required.")

    val two = alpha.parameters.find { it.name == "two" }
    assertNotNull(two, "Parameter two should exist.")
    assertEquals(IRParameter.Type.INT, two.type, "Parameter two should be of type integer.")
    assertEquals(IRParameter.Count.LIST, two.count, "Parameter two should be an array.")
    assertEquals(IRParameter.Presence.OPTIONAL, two.presence, "Parameter two should optional.")
  }

  @Test
  fun stages() {
    val stages = parser.stages()

    // Get the first alpha stage.
    val alphaOne = stages.find { it.uri.endsWith("alpha_one") }
    assertNotNull(alphaOne, "alpha_one stage should exist.")
    assertTrue(
        alphaOne.processor.uri.endsWith("alpha"), "alpha_one stage should use the alpha processor.")

    // Parse first argument.
    val one = alphaOne.arguments.find { it.name == "one" }
    assertNotNull(one, "alpha_one::one should exist.")
    assertEquals(1, one.value.size, "alpha_one::one should have one value.")
    assertEquals("Hello, World!", one.value[0], "alpha_one::one should be 'Hello, World!'.")

    // Parse second argument.
    val two = alphaOne.arguments.find { it.name == "two" }
    assertNull(two, "alpha_one::two should not exist.")
  }
}
