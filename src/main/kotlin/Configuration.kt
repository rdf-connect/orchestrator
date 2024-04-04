package technology.idlab

import java.io.File
import kotlinx.coroutines.*

class Configuration(configPath: String) {
    /** Processors described in the config. */
    private val processors: Array<Processor>

    init {
        val searchPath = File(configPath).parent
        processors = Array(2) {
            Processor(searchPath)
            Processor(searchPath)
        }
    }

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() = runBlocking {
        processors.map { async { it.executeSync() } }.map { it.await() }
    }
}
