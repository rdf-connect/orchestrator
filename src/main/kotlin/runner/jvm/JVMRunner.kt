package technology.idlab.runtime.jvm

import kotlin.concurrent.thread
import kotlinx.coroutines.flow.flow
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.runtime.Runner

class JVMRunner : Runner() {
  // The processors that are available to the runtime.
  private val processors = mutableMapOf<String, Class<Processor>>()
  private val stages = mutableMapOf<String, Processor>()
  private val readers = mutableMapOf<String, Reader>()

  // Handle outgoing messages.
  private val incomingFlow =
      flow<Unit> {
        for (message in outgoing) {
          // Get the reader.
          val (readerURI, payload) = message
          val reader = readers[readerURI]!!

          // Push data to the reader.
          reader.push(payload)
        }
      }

  override suspend fun prepare(processor: IRProcessor) {
    TODO("Not yet implemented")
  }

  override suspend fun prepare(stage: IRStage) {
    val processor = processors[stage.processor.uri]!!
    val arguments = mutableMapOf<String, Any>()
    val constructor = processor.getConstructor(Map::class.java)
    this.stages[stage.uri] = constructor.newInstance(arguments) as Processor
  }

  override suspend fun exec() {
    this.stages.values.map { thread { it.exec() } }.map { it.join() }
  }

  override suspend fun status(): Status {
    TODO("Not yet implemented")
  }
}
