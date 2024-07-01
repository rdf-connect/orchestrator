package parser

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    assertEquals(1, processors.size, "There should be one processor.")

    // Get processor and check its values.
    val processor = processors.find { it.uri.endsWith("processor") }
    assertNotNull(processor, "Processor should exist.")
    assertContains(processor.metadata.keys, "class", "processor processor should have a class key.")
    assertEquals(
        "MyProcessor",
        processor.metadata["class"],
        "Processor should have a 'class' key with value 'MyProcessor'.")
    assertEquals(Runner.Target.JVM, processor.target, "processor processor should target JVM.")
    assertEquals(3, processor.parameters.size, "processor processor should have two parameters.")

    // Check its arguments.
    val arg1 = processor.parameters["arg1"]
    assertNotNull(arg1, "Parameter arg1 should exist.")
    assertEquals(
        IRParameter.Type.STRING, arg1.getSimple(), "Parameter arg1 should be of type string.")
    assertEquals(IRParameter.Count.SINGLE, arg1.count, "Parameter arg1 should be a single value.")
    assertEquals(IRParameter.Presence.REQUIRED, arg1.presence, "Parameter arg1 should be required.")

    val arg2 = processor.parameters["arg2"]
    assertNotNull(arg2, "Parameter arg2 should exist.")
    assertEquals(
        IRParameter.Type.INT, arg2.getSimple(), "Parameter arg2 should be of type integer.")
    assertEquals(IRParameter.Count.LIST, arg2.count, "Parameter arg2 should be an array.")
    assertEquals(IRParameter.Presence.OPTIONAL, arg2.presence, "Parameter arg2 should optional.")

    val arg3 = processor.parameters["arg3"]
    assertNotNull(arg3, "Parameter arg3 should exist.")
    assertEquals(2, arg3.getComplex().size, "Parameter arg3 should have two values.")
    assertEquals(IRParameter.Count.SINGLE, arg3.count, "Parameter arg3 should be a single value.")
    assertEquals(IRParameter.Presence.REQUIRED, arg3.presence, "Parameter arg3 should be required.")

    val arg4 = arg3["arg4"]
    assertNotNull(arg4, "Parameter arg4 should exist.")
    assertEquals(
        IRParameter.Type.STRING, arg4.getSimple(), "Parameter arg4 should be of type string.")
    assertEquals(IRParameter.Count.SINGLE, arg4.count, "Parameter arg4 should be a single value.")
    assertEquals(IRParameter.Presence.REQUIRED, arg4.presence, "Parameter arg4 should be required.")

    val arg5 = arg3["arg5"]
    assertNotNull(arg5, "Parameter arg5 should exist.")
    assertEquals(
        IRParameter.Type.INT, arg5.getSimple(), "Parameter arg5 should be of type integer.")
    assertEquals(IRParameter.Count.SINGLE, arg5.count, "Parameter arg5 should be a single value.")
    assertEquals(IRParameter.Presence.OPTIONAL, arg5.presence, "Parameter arg5 should be optional.")
  }

  @Test
  fun stages() {
    val stages = parser.stages()

    // Get the stage.
    assertEquals(1, stages.size, "There should be one stage.")
    val stage = stages[0]
    assertTrue(stage.processor.uri.endsWith("processor"), "Stage should use the correct processor.")

    // Parse first argument.
    val arg1 = stage.arguments["arg1"]
    assertNotNull(arg1, "arg1 should exist.")
    assertEquals(1, arg1.getSimple().size, "arg1 should have one value")
    assertEquals("Hello, World!", arg1.getSimple()[0], "arg1 should be 'Hello, World!'.")

    // Parse second argument.
    val arg2 = stage.arguments["arg2"]
    assertNotNull(arg2, "arg2 should exist.")
    val arg2Values = arg2.getSimple().sorted()
    assertEquals(3, arg2.getSimple().size, "arg2 should have three values")
    assertEquals("1", arg2Values[0], "arg2 should be '1'.")
    assertEquals("2", arg2Values[1], "arg2 should be '2'.")
    assertEquals("3", arg2Values[2], "arg2 should be '3'.")

    // Parse third argument.
    val arg3 = stage.arguments["arg3"]
    assertNotNull(arg3, "arg3 should exist.")
    assertEquals(1, arg3.getComplex().size, "arg3 should have one instance")
    val arg = arg3.getComplex()[0]

    // Parse fourth argument.
    val arg4 = arg["arg4"]
    assertNotNull(arg4, "arg4 should exist.")
    assertEquals(1, arg4.getSimple().size, "arg4 should have one value")
    assertEquals("Hello, World!", arg4.getSimple()[0], "arg4 should be 'Hello, World!'.")

    // Parse fifth argument.
    val arg5 = arg["arg5"]
    assertNotNull(arg5, "arg5 should exist.")
    assertEquals(1, arg5.getSimple().size, "arg5 should have one value")
    assertEquals("1", arg5.getSimple()[0], "arg5 should be '1'.")
  }
}
