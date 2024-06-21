package runner.impl

import runner.Runner
import runner.RunnerTest
import runner.jvm.JVMRunner

class JVMRunnerTest : RunnerTest() {
  override val target: Runner.Target = Runner.Target.JVM

  override val metadata: Map<String, String> = mapOf("class" to "technology.idlab.std.Transparent")

  override fun createRunner(): Runner = JVMRunner()
}
