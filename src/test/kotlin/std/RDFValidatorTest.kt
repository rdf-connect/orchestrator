package std

import bridge.DummyReader
import bridge.DummyWriter
import java.io.File
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.toByteArray
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import technology.idlab.exception.RunnerException
import technology.idlab.runner.Pipeline
import technology.idlab.std.RDFValidator

/** Ontology location. */
private val ontology =
    Thread.currentThread().contextClassLoader.getResource("pipeline.ttl")!!.let { File(it.file) }

/** Textual representation of a valid SHACL file. */
private const val validShape =
    """
@prefix ex: <http://example.org#> .
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

ex:PointShape
    a sh:NodeShape;
    sh:targetClass ex:Point;
    sh:closed true;
    sh:ignoredProperties (rdf:type);
    sh:property [
        sh:path ex:x;
        sh:message "Requires an integer X coordinate";
        sh:name "X-coordinate";
        sh:datatype xsd:int;
        sh:minCount 1;
        sh:maxCount 1;
    ], [
        sh:path ex:y;
        sh:message "Requires an integer Y coordinate";
        sh:name "Y-coordinate";
        sh:datatype xsd:int;
        sh:minCount 1;
        sh:maxCount 1;
    ].
"""

/** Textual representation of an invalid SHACL file. */
private val invalidShape = validShape.replace("xsd:int", "xkcd:int")

/** Textual representation of valid RDF of a Point shape. */
private const val validInput =
    """
@prefix ex: <http://example.org#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

ex:ValidPoint 
  a ex:Point;
  ex:x "1"^^xsd:int;
  ex:y "2"^^xsd:int.
"""

/** File with invalid Point shape. */
val invalidInput = validInput.replace("xsd:int", "xsd:string")

/** A file containing a valid definition of SHACL shapes. */
private val shapeFile = File.createTempFile("shapes", "ttl").apply { writeText(validShape) }

/** A file containing an invalid definition of SHACL shapes. */
private val invalidShapeFile =
    File.createTempFile("invalid_shapes", "ttl").apply { writeText(invalidShape) }

/** Simple pipeline containing the processor. */
private val pipeline =
    """
@prefix jvm: <https://w3id.org/conn/jvm#>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix owl: <http://www.w3.org/2002/07/owl#>.

<> owl:imports <${ontology.absolutePath}>.

# Range -> Filter
<reader> a jvm:MemoryChannelReader.
<writer> a jvm:MemoryChannelWriter.

# Define a filter processor.
[]
  a jvm:RDFValidator;
  jvm:input <reader>;
  jvm:output <writer>;
  jvm:shapes "${shapeFile.absolutePath}";
  jvm:error_is_fatal true;
  jvm:print_report true.
"""

/** Simple pipeline containing the processor using default values. */
private val pipelineDefaults =
    """
@prefix jvm: <https://w3id.org/conn/jvm#>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix owl: <http://www.w3.org/2002/07/owl#>.

<> owl:imports <${ontology.absolutePath}>.

# Range -> Filter
<reader> a jvm:MemoryChannelReader.
<writer> a jvm:MemoryChannelWriter.

# Define a filter processor.
[]
  a jvm:RDFValidator;
  jvm:input <reader>;
  jvm:output <writer>;
  jvm:shapes "${shapeFile.absolutePath}".
"""

private val pipelineFile = File.createTempFile("pipeline", "ttl").apply { writeText(pipeline) }
private val pipelineDefaultsFile =
    File.createTempFile("pipeline", "ttl").apply { writeText(pipelineDefaults) }

class RDFValidatorTest {
  @Test
  fun definition() {
    // Initialize pipeline.
    val pipeline = Pipeline(pipelineFile)

    // Extract processor.
    val processors = pipeline.processors
    assertEquals(1, processors.size)
    val validator = processors[0] as RDFValidator

    // Check arguments.
    assertEquals(shapeFile.absolutePath, validator.getArgument("shapes"))
    assertEquals(Optional.of(true), validator.getArgument("error_is_fatal"))
    assertEquals(Optional.of(true), validator.getArgument("print_report"))
  }

  @Test
  fun definitionDefaults() {
    // Initialize pipeline.
    val pipeline = Pipeline(pipelineDefaultsFile)

    // Extract processor.
    val processors = pipeline.processors
    assertEquals(1, processors.size)
    val validator = processors[0] as RDFValidator

    // Check arguments.
    assertEquals(shapeFile.absolutePath, validator.getArgument("shapes"))
    assertEquals(Optional.empty<Boolean>(), validator.getArgument("error_is_fatal"))
    assertEquals(Optional.empty<Boolean>(), validator.getArgument("print_report"))
  }

  @Test
  fun conforms() {
    // Setup channels.
    val input = DummyReader(arrayOf(validInput.toByteArray()))
    val output = DummyWriter()

    // Create a new RDFValidator instance.
    val validator =
        RDFValidator(
            mapOf(
                "shapes" to shapeFile.path,
                "input" to input,
                "output" to output,
                "error_is_fatal" to Optional.of(true),
                "print_report" to Optional.of(true),
            ))

    // Execute validator.
    validator.exec()

    // Check if the output is correct.
    assertEquals(1, output.getValues().size)
    assertEquals(validInput, output.getValues()[0].decodeToString())
  }

  @Test
  fun doesNotConformAndIsFatal() {
    // Setup channels.
    val input = DummyReader(arrayOf(invalidInput.toByteArray()))
    val output = DummyWriter()

    // Create a new RDFValidator instance.
    val validator =
        RDFValidator(
            mapOf(
                "shapes" to shapeFile.path,
                "input" to input,
                "output" to output,
                "error_is_fatal" to Optional.of(true),
                "print_report" to Optional.of(false),
            ))

    // Execute validator.
    assertThrows<RunnerException> { validator.exec() }

    // Nothing has been written.
    assertEquals(0, output.getValues().size)
  }

  @Test
  fun doesNotConform() {
    // Setup channels.
    val input = DummyReader(arrayOf(invalidInput.toByteArray()))
    val output = DummyWriter()

    // Create a new RDFValidator instance.
    val validator =
        RDFValidator(
            mapOf(
                "shapes" to shapeFile.path,
                "input" to input,
                "output" to output,
                "error_is_fatal" to Optional.of(false),
                "print_report" to Optional.of(false),
            ))

    // Execute validator.
    assertDoesNotThrow { validator.exec() }

    // Nothing has been written.
    assertEquals(0, output.getValues().size)
  }

  @Test
  fun invalidSHACL() {
    // Fails during initialization of the SHACL model.
    assertThrows<RunnerException> {
      RDFValidator(
          mapOf(
              "shapes" to invalidShapeFile.path,
              "input" to DummyReader(arrayOf()),
              "output" to DummyWriter(),
              "error_is_fatal" to Optional.of(true),
              "print_report" to Optional.of(false),
          ))
    }
  }
}
