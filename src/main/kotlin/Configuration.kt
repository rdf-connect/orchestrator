package technology.idlab

import java.io.File

class Configuration(configPath: String) {
    private val processors: Array<Processor>

    init {
        val searchPath = File(configPath).parent
        processors = Array(1) { Processor(searchPath) }
    }

    fun executeAll() {
        for (processor in processors) {
            processor.execute()
        }
    }
}
