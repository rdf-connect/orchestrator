package technology.idlab.extensions

import java.io.File

fun File.rawPath() = this.toPath().toString().removePrefix("file:")
