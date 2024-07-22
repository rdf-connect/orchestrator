package runner.impl.jvm

import kotlinx.coroutines.channels.Channel
import runner.RunnerTest
import technology.idlab.runner.Runner
import technology.idlab.runner.impl.jvm.JVMRunner

class JVMRunnerTest : RunnerTest() {
  override val target = "https://www.rdf-connect.com/#/JVMRunner"

  override val metadata: Map<String, String> = mapOf("class" to "technology.idlab.std.Transparent")

  override fun createRunner(): Runner = JVMRunner(Channel())
}
