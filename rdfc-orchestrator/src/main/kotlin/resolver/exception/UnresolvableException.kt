package technology.idlab.rdfc.orchestrator.resolver.exception

import technology.idlab.rdfc.orchestrator.resolver.ResolverException

/**
 * The dependency type is not recognized.
 *
 * @param uri The URI of the dependency which is not recognized.
 */
class UnresolvableException(val uri: String) : ResolverException()
