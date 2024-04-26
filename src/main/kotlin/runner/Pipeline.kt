package technology.idlab.runner

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.createLogger
import runner.Processor
import java.io.File

class Pipeline(config: File) {
    private val logger = createLogger()

    /** Processors described in the config. */
    private val processors: List<Processor> = Parser(config).getStages()

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() {
        // Run setup phase.
        logger.info("Running setup phase")
        runBlocking {
            processors.map {
                async { it.setup() }
            }.map {
                it.await()
            }
        }

        // Run execution phase.
        logger.info("Running execution phase")
        runBlocking {
            processors.map {
                async { it.exec() }
            }.map {
                it.await()
            }
        }

        logger.info("Pipeline executed successfully")
    }
}
