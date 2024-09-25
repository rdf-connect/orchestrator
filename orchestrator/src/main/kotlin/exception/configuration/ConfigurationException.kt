package technology.idlab.exception.configuration

import technology.idlab.exception.OrchestratorException

abstract class ConfigurationException : OrchestratorException()

/** No configuration file was found at the given path. */
class NoConfigurationFoundException : ConfigurationException()

/** An issue was found in the configuration file. */
class InvalidConfigurationException : ConfigurationException()

/** No pipeline was declared in the pipeline. */
class NoPipelineFoundException : ConfigurationException()

/** The pipeline does not contain any stages. */
class EmptyPipelineException(private val emptyPipelineUri: String) : ConfigurationException()

/** A runner was referenced by a stage, but no such runner was found. */
class NoSuchRunnerException(private val runnerUri: String) : ConfigurationException()
