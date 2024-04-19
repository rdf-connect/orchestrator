package technology.idlab

abstract class Template {
    abstract fun onInitialisation(): Unit

    abstract fun onExit(): Unit
}
