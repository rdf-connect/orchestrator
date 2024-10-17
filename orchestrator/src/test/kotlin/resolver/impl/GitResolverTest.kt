package resolver.impl

import resolver.ResolverTest
import technology.idlab.resolver.impl.GitResolver

class GitResolverTest : ResolverTest() {
  override val uri: String = "https://github.com/jenspots/rdfc-template-kt.git"
  override val resolver = GitResolver()
}
