package technology.idlab.runner

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import runner.Processor
import technology.idlab.logging.Log
import java.io.File

class Pipeline(config: File) {
    /** Processors described in the config. */
    private val processors: List<Processor> = Parser(config).getStages()

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() {
        // Run setup phase.
        Log.shared.info("Running setup phase")
        runBlocking {
            processors.map {
                async { it.setup() }
            }.map {
                it.await()
            }
        }

        // Run execution phase.
        Log.shared.info("Running execution phase")
        runBlocking {
            processors.map {
                async { it.exec() }
            }.map {
                it.await()
            }
        }

        Log.shared.info("Pipeline executed successfully")
    }
}
