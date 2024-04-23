package technology.idlab.runner

data class Stage(val processor: Processor, val arguments: List<Any>) {
    fun execute() {
        processor.executeSync(arguments)
    }
}
