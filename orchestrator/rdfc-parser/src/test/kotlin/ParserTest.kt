package technology.idlab.rdfc.parser

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import technology.idlab.rdfc.core.intermediate.argument.LiteralArgument
import technology.idlab.rdfc.core.intermediate.argument.NestedArgument
import technology.idlab.rdfc.core.intermediate.parameter.LiteralParameter
import technology.idlab.rdfc.core.intermediate.parameter.LiteralParameterType
import technology.idlab.rdfc.core.intermediate.parameter.NestedParameter
import technology.idlab.rdfc.parser.impl.JenaParser

class ParserTest {
  private fun parse(resource: String): Parser {
    val uri = this::class.java.getResource(resource)
    val file = File(uri!!.toURI())
    val rootParser = JenaParser(listOf(file))
    val files =
        rootParser.dependencies().map { File(it.uri.removePrefix("file://") + "/index.ttl") }
    return JenaParser(listOf(listOf(file), files).flatten())
  }

  @Test
  fun pipelines() {
    val parser = parse("/pipelines/dummy/index.ttl")
    val pipelines = parser.pipelines()
    val packages = parser.packages()
    assertEquals(1, pipelines.size, "There should be one pipeline.")
    assertEquals(1, pipelines[0].stages.size, "There should be one stage.")
    assertEquals(1, pipelines.size, "There should be one package.")
    assertEquals(1, packages[0].processors.size, "There should be one processor.")
  }

  @Test
  fun packages() {
    // Parse the package in the file.
    val parser = parse("/pipelines/dummy/index.ttl")
    val pkg = parser.packages().single()

    // Check the contents of the data class.
    assertEquals("1.0.0", pkg.version)
    assertEquals("Jens Pots", pkg.author)
    assertEquals("A simple description.", pkg.description)
    assertEquals("https://example.com.git", pkg.repo)
    assertEquals("MIT", pkg.license)
    assertEquals(4, pkg.prepare.size)
    assertEquals("make", pkg.prepare.get(0))
    assertEquals("make install", pkg.prepare.get(1))
    assertEquals("make test", pkg.prepare.get(2))
    assertEquals("make clean", pkg.prepare.get(3))

    // Check the processors.
    assertEquals(1, pkg.processors.size)
    assertTrue(pkg.processors[0].parameters["message"].single)

    // Check the runners.
    assertEquals(1, pkg.runners.size)
    assertEquals("command", pkg.runners[0].entrypoint)
  }

  @Test
  fun processors() {
    val parser = parse("/pipelines/basic/index.ttl")

    val processors = parser.packages().map { it.processors }.flatten()
    assertEquals(1, processors.size)

    // Get processor and check its values.
    val processor = processors.find { it.uri.endsWith("Processor") }
    assertNotNull(processor)
    assertContains(processor.metadata.keys, "class")
    assertEquals("MyProcessor", processor.metadata["class"])
    assertEquals(3, processor.parameters.type.size)

    // Check its arguments.
    val arg1 = processor.parameters["arg1"]
    assertNotNull(arg1)

    if (arg1 !is LiteralParameter) {
      throw AssertionError()
    }

    assertEquals(LiteralParameterType.STRING, arg1.type)
    assertTrue(arg1.single)
    assertFalse(arg1.optional)

    val arg2 = processor.parameters["arg2"]
    assertNotNull(arg2)

    if (arg2 !is LiteralParameter) {
      throw AssertionError()
    }

    assertEquals(LiteralParameterType.INT, arg2.type)
    assertFalse(arg2.single)
    assertTrue(arg2.optional)

    val arg3 = processor.parameters["arg3"]
    assertNotNull(arg3)

    if (arg3 !is NestedParameter) {
      throw AssertionError()
    }

    assertEquals(2, arg3.type.size)
    assertTrue(arg3.single)
    assertFalse(arg3.optional)

    val arg4 = arg3["arg4"]
    assertNotNull(arg4)

    if (arg4 !is LiteralParameter) {
      throw AssertionError()
    }

    assertEquals(LiteralParameterType.STRING, arg4.type)
    assertTrue(arg4.single)
    assertFalse(arg4.optional)

    val arg5 = arg3["arg5"]
    assertNotNull(arg5)

    if (arg5 !is LiteralParameter) {
      throw AssertionError()
    }

    assertEquals(LiteralParameterType.INT, arg5.type)
    assertTrue(arg5.single)
    assertTrue(arg5.optional)
  }

  @Test
  fun stages() {
    val parser = parse("/pipelines/basic/index.ttl")
    val pipelines = parser.pipelines()
    val stages = pipelines.single().stages

    // Get the stage.
    assertEquals(1, stages.size)
    val stage = stages[0]
    assertTrue(stage.processor.uri.endsWith("Processor"))

    // Parse first argument.
    val arg1 = stage.arguments["arg1"]
    assertNotNull(arg1)

    if (arg1 !is LiteralArgument) {
      throw AssertionError()
    }

    assertEquals(1, arg1.values.size)
    assertEquals("Hello, World!", arg1.values[0])

    // Parse second argument.
    val arg2 = stage.arguments["arg2"]
    assertNotNull(arg2, "arg2 should exist.")

    if (arg2 !is LiteralArgument) {
      throw AssertionError()
    }

    val arg2Values = arg2.values.sorted()
    assertEquals(3, arg2.values.size)
    assertEquals("1", arg2Values[0])
    assertEquals("2", arg2Values[1])
    assertEquals("3", arg2Values[2])

    // Parse third argument.
    val arg3 = stage.arguments["arg3"]
    assertNotNull(arg3)

    if (arg3 !is NestedArgument) {
      throw AssertionError()
    }

    assertEquals(1, arg3.values.size)
    val arg = arg3.values[0]

    // Parse fourth argument.
    val arg4 = arg["arg4"]
    assertNotNull(arg4)

    if (arg4 !is LiteralArgument) {
      throw AssertionError()
    }

    assertEquals(1, arg4.values.size, "arg4 should have one value")
    assertEquals("Hello, World!", arg4.values[0])

    // Parse fifth argument.
    val arg5 = arg["arg5"]
    assertNotNull(arg5)

    if (arg5 !is LiteralArgument) {
      throw AssertionError()
    }

    assertEquals(1, arg5.values.size)
    assertEquals("1", arg5.values[0])
  }
}
