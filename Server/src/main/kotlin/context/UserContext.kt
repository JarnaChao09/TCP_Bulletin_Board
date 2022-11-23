package context

import data.User

interface UserContext {
    val users: MutableMap<String, User>
}