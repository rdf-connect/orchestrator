package processors

import runner.Runner
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

class NodeTransparent {
  companion object {
    private val processor =
        IRProcessor(
            "transparent",
            Runner.Target.NODEJS,
            mapOf(
                "input" to
                    IRParameter(
                        "input",
                        IRParameter.Type.READER,
                        IRParameter.Presence.REQUIRED,
                        IRParameter.Count.SINGLE,
                    ),
                "output" to
                    IRParameter(
                        "output",
                        IRParameter.Type.WRITER,
                        IRParameter.Presence.REQUIRED,
                        IRParameter.Count.SINGLE,
                    ),
            ),
            mapOf("import" to "../std/transparent.js"),
        )

    fun stage(channelInURI: String, channelOutURI: String): IRStage {
      return IRStage(
          "transparent_stage",
          processor,
          mapOf(
              "input" to IRArgument(processor.parameters["input"]!!, listOf(channelInURI)),
              "output" to IRArgument(processor.parameters["output"]!!, listOf(channelOutURI))),
      )
    }
  }
}
