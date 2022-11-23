package data

import java.net.Socket

data class User(val id: String, val socket: Socket, val groups: ArrayDeque<String>)
