package processors

import runner.Runner
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

class NodeTransparent {
  companion object {
    val processor =
        IRProcessor(
            "transparent",
            Runner.Target.NODEJS,
            mapOf(
                "input" to
                    IRParameter(
                        IRParameter.Type.READER,
                        presence = IRParameter.Presence.REQUIRED,
                        count = IRParameter.Count.SINGLE,
                    ),
                "output" to
                    IRParameter(
                        IRParameter.Type.WRITER,
                        presence = IRParameter.Presence.REQUIRED,
                        count = IRParameter.Count.SINGLE,
                    ),
            ),
            mapOf("import" to "../std/transparent.js"),
        )

    fun stage(channelInURI: String, channelOutURI: String): IRStage {
      return IRStage(
          "transparent_stage",
          processor.uri,
          mapOf(
              "input" to IRArgument(listOf(channelInURI)),
              "output" to IRArgument(listOf(channelOutURI))),
      )
    }
  }
}
