package technology.idlab.rdfc.core

/**
 * All exceptions which are explicitly thrown in RDFC should inherit from this interface. The only
 * exception to this rule are exceptions which are thrown by the Kotlin standard library, such as by
 * `check`, `require`, etc. and are considered to be unrecoverable.
 */
abstract class RDFCException : Exception()
