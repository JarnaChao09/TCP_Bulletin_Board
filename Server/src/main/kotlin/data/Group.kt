package data

data class Group(val id: String, val users: MutableMap<String, User>, val messages: MutableList<Message>)
