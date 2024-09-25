package technology.idlab.exception

abstract class OrchestratorException : Exception()

/*
 * Indicates that an exception was thrown, but the cause is not yet mapped to a specific exception.
 */
class UnknownException() : OrchestratorException()

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
) : OrchestratorException()

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
    override val cause: Exception,
) : OrchestratorException()

/**
 * A package should have been resolved but was not found on disk.
 *
 * @param uri The URI of the unresolved package.
 */
class UnresolvedDependencyException(
    /** The URI of the unresolved dependency. */
    private val uri: String,
) : OrchestratorException()

/**
 * The working directory for a runner is invalid.
 *
 * @param directory The invalid directory.
 */
class InvalidWorkingDirectoryException(
    /**  */
    private val directory: String,
) : OrchestratorException()
