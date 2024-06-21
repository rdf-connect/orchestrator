package runner.impl

import runner.Runner
import runner.RunnerTest

class NodeRunnerTest : RunnerTest() {
  override val target: Runner.Target = Runner.Target.NODEJS

  override val metadata: Map<String, String> = mapOf("import" to "../std/transparent.js")

  override fun createRunner(): Runner {
    return NodeRunner(5000)
  }
}
