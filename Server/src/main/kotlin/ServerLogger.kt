import java.io.PrintStream

class ServerLogger(private val output: PrintStream = System.out) {
    fun info(msg: String) {
        output.println(" [INFO]: $msg")
    }

    fun warn(msg: String) {
        output.println("$ANSI_BLUE [WARN]: $msg$ANSI_RESET")
    }

    fun error(msg: String) {
        output.println("$ANSI_RED[ERROR]: $msg$ANSI_RESET")
    }

    companion object {
        private const val ANSI_RESET = "\u001B[0m"
        private const val ANSI_RED = "\u001B[31m"
        private const val ANSI_BLUE = "\u001B[34m"
    }
}