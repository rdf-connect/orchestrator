package technology.idlab.runner

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.createLogger
import java.io.File

class Pipeline(config: File) {
    private val logger = createLogger()

    /** Processors described in the config. */
    private val processors: MutableMap<String, Processor> = mutableMapOf()

    /** Concrete functions in the pipeline, also known as steps. */
    private val stages: List<Stage>

    init {
        val parser = Parser(config)
        val processors = parser.getProcessors()

        this.processors.putAll(processors.map { it.name to it })
        this.stages = parser.getStages(processors)
    }

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() {
        logger.info("Executing pipeline")
        runBlocking {
            stages.map {
                async { it.execute() }
            }.map {
                it.await()
            }
        }
        logger.info("Pipeline executed successfully")
    }
}
