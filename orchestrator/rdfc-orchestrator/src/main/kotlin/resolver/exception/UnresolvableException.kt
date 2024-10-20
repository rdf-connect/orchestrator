package technology.idlab.resolver.exception

import technology.idlab.resolver.ResolverException

/**
 * The dependency type is not recognized.
 *
 * @param uri The URI of the dependency which is not recognized.
 */
class UnresolvableException(val uri: String) : ResolverException()
