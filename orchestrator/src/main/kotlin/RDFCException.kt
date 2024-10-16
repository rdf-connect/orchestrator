package technology.idlab

import technology.idlab.intermediate.runner.RunnerType
import technology.idlab.log.Log

abstract class RDFCException : Exception()

/**
 * A process spawned with a given command failed with an exit code.
 *
 * @param command The command which was run and failed.
 * @param exitCode The exit code of the failed command.
 */
class CommandException(
    /** The command which was run and failed. */
    private val command: String,
    /** The exit code of the failed command. */
    private val exitCode: Int,
) : RDFCException() {
  init {
    Log.shared.severe("Command failed with exit code $exitCode: $command")
  }
}

/**
 * An exception was thrown during the execution of the pipeline.
 *
 * @param pipelineUri The URI of the pipeline which failed.
 * @param cause The exception which caused the pipeline to fail.
 */
class PipelineException(
    /** The URI of the pipeline which failed. */
    private val pipelineUri: String,
    /** The exception which caused the pipeline to fail. */
    override val cause: Exception? = null,
) : RDFCException()

/**
 * A package should have been resolved but was not found on disk.
 *
 * @param uri The URI of the unresolved package.
 */
class UnresolvedDependencyException(
    /** The URI of the unresolved dependency. */
    private val uri: String,
) : RDFCException()

/**
 * The working directory for a runner is invalid.
 *
 * @param directory The invalid directory.
 */
class InvalidWorkingDirectoryException(
    /**  */
    private val directory: String,
) : RDFCException()

/**
 * Thrown if the implementation of a processor is invalid.
 *
 * @param processorUri The URI of the processor which is invalid.
 */
class InvalidProcessorException(
    /**  */
    private val processorUri: String,
) : RDFCException()

/**
 * Attempted to access an argument which does not exist.
 *
 * @param name The name of the argument.
 * @param reason The reason why the access failed.
 */
class InvalidArgumentAccessException(
    /** The name of the argument. */
    private val name: String,
    /** The reason why the access failed. */
    private val reason: String? = null
) : RDFCException()

/**
 * An error occurred while attempting to connect to a remote service.
 *
 * @param cause The exception which caused the connection to fail.
 */
class ConnectionException(override val cause: Exception) : RDFCException()

/** A request was successfully sent, but the instructions were not recognized. */
class UnrecognizedRequestException : RDFCException()

/** No configuration file was found at the given path. */
class NoConfigurationFoundException : RDFCException()

/** An issue was found in the configuration file. */
class InvalidConfigurationException : RDFCException()

/** No pipeline was declared in the pipeline. */
class NoPipelineFoundException : RDFCException()

/**
 * The pipeline does not contain any stages.
 *
 * @param emptyPipelineUri The URI of the pipeline which is empty.
 */
class EmptyPipelineException(private val emptyPipelineUri: String) : RDFCException()

/**
 * A runner was referenced by a stage, but no such runner was found.
 *
 * @param runnerUri The URI of the runner which was not found.
 */
class NoSuchRunnerException(private val runnerUri: String) : RDFCException()

/**
 * A known type was used, but it has no corresponding implementation.
 *
 * @param type The type which was used, but not implemented.
 */
class UnsupportedRunnerTypeException(private val type: RunnerType) : RDFCException()

/**
 * Metadata was expected, but not found.
 *
 * @param uri The URI of the object which is missing metadata.
 * @param key The key which is missing.
 */
class MissingMetadataException(private val uri: String, private val key: String) : RDFCException()

/**
 * Attempted to load a JAR file from an invalid path.
 *
 * @param path The path which was invalid.
 */
class InvalidJarPathException(private val path: String) : RDFCException()
