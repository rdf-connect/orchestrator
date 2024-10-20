package technology.idlab.runner.exception

import technology.idlab.runner.RunnerException

/**
 * Attempted to access an argument which does not exist.
 *
 * @param name The name of the argument.
 * @param reason The reason why the access failed.
 */
class InvalidArgumentAccessException(private val name: String, private val reason: String? = null) :
    RunnerException()
