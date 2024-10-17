package resolver.impl

import java.io.File
import resolver.ResolverTest
import technology.idlab.resolver.impl.LocalResolver

class LocalResolverTest : ResolverTest() {
  override val uri: String = File("..").canonicalPath + "/packages/shacl-validator-py"
  override val resolver = LocalResolver()
}
