package technology.idlab.rdfc.orchestrator.resolver.impl

import java.io.File
import technology.idlab.rdfc.orchestrator.resolver.ResolverTest

class LocalResolverTest : ResolverTest() {
  override val uri: String = File("..").canonicalPath
  override val resolver = LocalResolver()
}
