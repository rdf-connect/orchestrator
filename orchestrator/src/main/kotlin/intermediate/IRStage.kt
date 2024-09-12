package technology.idlab.intermediate

import arrow.core.zip

private fun getWriters(options: Map<String, Pair<IRParameter, IRArgument>>): List<String> {
  val results = mutableListOf<String>()

  options.values.forEach { (parameter, arguments) ->
    when (parameter.kind) {
      IRParameter.Kind.SIMPLE -> {
        if (parameter.getSimple() == IRParameter.Type.WRITER) {
          val uri = arguments.getSimple().first()
          results.add(uri)
        }
      }
      IRParameter.Kind.COMPLEX -> {
        val nestedParams = parameter.getComplex()
        arguments.getComplex().forEach { argument ->
          val nestedOptions = getWriters(nestedParams.zip(argument))
          results.addAll(nestedOptions)
        }
      }
    }
  }

  return results
}

private fun getReaders(options: Map<String, Pair<IRParameter, IRArgument>>): List<String> {
  val results = mutableListOf<String>()

  options.values.forEach { (parameter, arguments) ->
    when (parameter.kind) {
      IRParameter.Kind.SIMPLE -> {
        if (parameter.getSimple() == IRParameter.Type.READER) {
          val uri = arguments.getSimple().first()
          results.add(uri)
        }
      }
      IRParameter.Kind.COMPLEX -> {
        val nestedParams = parameter.getComplex()
        arguments.getComplex().forEach { argument ->
          val nestedOptions = getReaders(nestedParams.zip(argument))
          results.addAll(nestedOptions)
        }
      }
    }
  }

  return results
}

data class IRStage(
    // The URI of the stage.
    val uri: String,
    // The processor.
    val processor: IRProcessor,
    // Concrete but unparsed arguments for the stage.
    val arguments: Map<String, IRArgument> = emptyMap(),
) {
  fun getReaders(): List<String> {
    return getReaders(processor.parameters.zip(arguments))
  }

  fun getWriters(): List<String> {
    return getWriters(processor.parameters.zip(arguments))
  }
}
