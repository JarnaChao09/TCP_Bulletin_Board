package context

import data.Group

interface GroupContext {
    val groups: MutableMap<String, Group>
}