package context

import ServerLogger

interface LoggingContext {
    val logger: ServerLogger
}