package technology.idlab.runner

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.Log
import java.io.File
import kotlin.concurrent.thread

class Pipeline(config: File) {
    /** Processors described in the config. */
    private val processors: List<Processor> = Parser(config).stages

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
        processors.map {
            thread { it.exec() }
        }.map {
            it.join()
        }

        Log.shared.info("Pipeline executed successfully")
    }
}
