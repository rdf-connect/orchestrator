package technology.idlab.configuration

import technology.idlab.parser.Parser

/**
 * The Configuration class is an extension of the parser, and allows for much more powerful querying
 * of the model, as well as provide important assertions of the model's consistency.
 */
abstract class Configuration(parser: Parser) {}
