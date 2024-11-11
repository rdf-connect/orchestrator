package technology.idlab.rdfc.orchestrator.resolver.impl

import technology.idlab.rdfc.orchestrator.resolver.ResolverTest

class GitResolverTest : ResolverTest() {
  override val uri: String = "https://github.com/jenspots/rdfc-template-kt.git"
  override val resolver = GitResolver()
}
