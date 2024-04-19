package technology.idlab.logging

import BasicFormatter
import java.util.logging.ConsoleHandler

class StandardOutput(): ConsoleHandler() {
    init {
        this.formatter = BasicFormatter()
        this.setOutputStream(System.out)
    }
}
