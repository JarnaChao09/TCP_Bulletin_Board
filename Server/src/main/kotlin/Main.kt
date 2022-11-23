import command.Client
import command.Data
import command.Server
import context.GroupContext
import context.LoggingContext
import context.UserContext
import data.Group
import data.Message
import data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) = runBlocking {
    val context = object : LoggingContext, GroupContext, UserContext {
        override val logger: ServerLogger = ServerLogger()
        override val groups: MutableMap<String, Group> = mutableMapOf()
        override val users: MutableMap<String, User> = mutableMapOf()
    }

    val serverSocket = withContext(Dispatchers.IO) {
        ServerSocket(args[0].toInt())
    }

    println("Server is listening at ${serverSocket.localSocketAddress} on port ${serverSocket.localPort}")

    with(context) {
        generateGroups(List(5) {
            "group${it + 1}"
        })
    }
    while (true) {
        val client = withContext(Dispatchers.IO) {
            serverSocket.accept()
        }
        println("Accepted $client")

        launch {
            with(context) {
                handleClient(client)
            }
        }
    }
}

context(GroupContext)
fun generateGroups(names: List<String>) {
    for (name in names) {
        groups[name] = Group(name, mutableMapOf(), mutableListOf())
    }
}

context(LoggingContext, GroupContext, UserContext)
suspend fun handleClient(client: Socket) {
    val (rChannel, sChannel) = client.channels()
    val shutdown: suspend () -> Unit = {
        withContext(Dispatchers.IO) {
            rChannel.close()
            sChannel.close()
            client.close()
        }
    }
    while (true) {
        try {
            logger.error("[CURRENT CLIENTS]: $users")
            val received = withContext(Dispatchers.IO) {
                rChannel.readLine()
            } ?: break
            val command = Json.decodeFromString<Client>(received)
            logger.info("[RECEIVED]: $command")
            handleCommand(client, command, sChannel, shutdown)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    cleanup(client)
    shutdown()
    logger.error("[CURRENT CLIENTS]: $users")
}

context(LoggingContext, GroupContext, UserContext)
suspend fun handleCommand(
    client: Socket,
    command: Client,
    sChannel: DataOutputStream,
    shutdown: suspend () -> Unit
) {
    when (command) {
        is Client.Connect -> {
            if (command.user in users) {
                sChannel.send(Json.encodeToString(
                    Server.Connect(
                        command.user,
                        400,
                        "Username taken"
                    )
                ))
                shutdown()
                return
            } else {
                users[command.user] = User(command.user, client, ArrayDeque())
                logger.warn("[CLIENT CONNECTED]")
                sChannel.send(Json.encodeToString(command.toServer(200)))
            }
        }

        is Client.Join -> {
            if (command.group in groups) {
                val currentGroup = groups[command.group]!!
                val usersGroups = users[command.user]!!.groups
                val usersCurrentGroup = usersGroups.firstOrNull() ?: ""
                if (command.user in currentGroup.users && usersCurrentGroup == currentGroup.id) {
                    sChannel.send(Json.encodeToString<Server>(
                        Server.Join(
                            command.user,
                            200,
                            Server.Content(
                                command.group,
                                "${command.user} is already in ${command.group}"
                            )
                        )
                    ))
                } else {
                    val (action, has) = if (command.group in usersGroups) {
                        "switching to" to "returned to"
                    } else {
                        "joining" to "joined"
                    }
                    sChannel.send(Json.encodeToString<Server>(Server.Join(
                        command.user,
                        200,
                        Server.Content(
                            command.group,
                            "$action ${command.group} successful"
                        )
                    )))
                    for (otherUser in currentGroup.users.values) {
                        if (otherUser.id != command.user && otherUser.groups.first() == command.group) {
                            otherUser.socket.writeChannel().send(
                                Json.encodeToString<Server>(
                                    Server.Join(
                                        command.user,
                                        200,
                                        Server.Content(
                                            command.group,
                                            "${command.user} has $has ${command.group}"
                                        )
                                    )
                                )
                            )
                        }
                    }
                    usersGroups.apply {
                        this.remove(command.group)
                        this.addFirst(command.group)
                    }
                    currentGroup.messages.takeLast(2).forEach {
                        sChannel.send(
                            Json.encodeToString<Server>(
                                Server.Send(
                                    it.user,
                                    200,
                                    Server.Send.Message(
                                        it.id,
                                        it.user,
                                        it.date,
                                        it.subject
                                    ),
                                )
                            )
                        )
                    }
                    currentGroup.users[command.user] = users[command.user]!!
                }
            } else {
                sChannel.send(Json.encodeToString<Server>(Server.Join(
                    command.user,
                    400,
                    Server.Content(
                        users[command.user]!!.groups.firstOrNull() ?: "",
                        "Invalid group id: ${command.group}"
                    )
                )))
            }
        }

        is Client.Leave -> {
            if (command.group in groups) {
                val currentGroup = groups[command.group]!!
                val user = currentGroup.users.remove(command.user)
                if (user == null) {
                    logger.error("${command.user} was not in ${command.group}")
                    sChannel.send(Json.encodeToString<Server>(Server.Leave(
                        command.user,
                        400,
                        Server.Content(
                            users[command.user]!!.groups.firstOrNull() ?: "",
                            "${command.user} was not in ${command.group}"
                        )
                    )))
                } else {
                    users[command.user]!!.groups.remove(currentGroup.id)
                    for (otherUser in currentGroup.users.values) {
                        if (otherUser.groups.first() == command.group) {
                            otherUser.socket.writeChannel().send(
                                Json.encodeToString<Server>(
                                    Server.Leave(
                                        command.user,
                                        200,
                                        Server.Content(
                                            command.group,
                                            "${command.user} has left ${command.group}"
                                        )
                                    )
                                )
                            )
                        }
                    }
                    sChannel.send(Json.encodeToString<Server>(Server.Leave(
                        command.user,
                        200,
                        Server.Content(
                            users[command.user]!!.groups.firstOrNull() ?: "",
                            "${command.user} left ${command.group} successfully"
                        )
                    )))
                }
            } else {
                sChannel.send(Json.encodeToString<Server>(Server.Leave(
                    command.user,
                    400,
                    Server.Content(
                        users[command.user]!!.groups.firstOrNull() ?: "",
                        "Invalid group id: ${command.group}"
                    )
                )))
            }
            logger.warn("[CLIENT IS LEAVING]")
        }

        is Client.Ls -> {
            when (val content = command.content) {
                is Client.Ls.Message -> {
                    sChannel.send(Json.encodeToString(command.toServer(
                        400,
                        reason = "Fetching for a list of messages is currently unsupported"
                    )))
                }
                is Client.Ls.User -> {
                    if (content.group in groups && command.user in groups[content.group]!!.users) {
                        sChannel.send(Json.encodeToString(command.toServer(
                            200,
                            groups[content.group]!!.users.keys.toList().run {
                                if (content.amount == -1) {
                                    this
                                } else {
                                    this.take(content.amount)
                                }
                            },
                        )))
                    } else {
                        sChannel.send(Json.encodeToString(command.toServer(
                            400,
                            reason = "${command.user} is not in the group ${content.group}"
                        )))
                    }
                }
                is Client.Ls.Group -> {
                    sChannel.send(Json.encodeToString(command.toServer(
                        200,
                        groups.keys.toList().run {
                            if (content.amount == -1) {
                                this
                            } else {
                                this.take(content.amount)
                            }
                        },
                    )))
                }
            }
        }

        is Client.Send -> {
            val date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            val usersCurrentGroup = users[command.user]!!.groups.firstOrNull() ?: ""
            if (command.message.group in groups) {
                if (usersCurrentGroup == command.message.group) {
                    val currentGroup = groups[command.message.group]!!
                    if (command.user in currentGroup.users) {
                        val message = Message(
                            "${command.message.group}-msg-${currentGroup.messages.size + 1}",
                            command.user,
                            date,
                            command.message.subject,
                            command.message.message
                        )
                        currentGroup.messages.add(message)
                        currentGroup.users.forEach { (_, user: User) ->
                            if (user.groups.first() == command.message.group) {
                                user.socket.writeChannel().send(
                                    Json.encodeToString<Server>(
                                        Server.Send(
                                            command.user,
                                            200,
                                            Server.Send.Message(
                                                message.id,
                                                message.user,
                                                message.date,
                                                message.subject
                                            )
                                        )
                                    )
                                )
                            }
                        }
                    } else {
                        sChannel.send(
                            Json.encodeToString<Server>(
                                Server.Send(
                                    command.user,
                                    400,
                                    Server.Send.Message(
                                        "msgfailed",
                                        command.user,
                                        date,
                                        "Message Failed to Send (${command.user} not in ${command.message.group})",
                                    )
                                )
                            )
                        )
                    }
                } else {
                    sChannel.send(
                        Json.encodeToString<Server>(
                            Server.Send(
                                command.user,
                                400,
                                Server.Send.Message(
                                    "msgfailed",
                                    command.user,
                                    date,
                                    "Not currently in ${command.message.group}",
                                )
                            )
                        )
                    )
                }
            } else {
                sChannel.send(Json.encodeToString<Server>(
                    Server.Send(
                        command.user,
                        400,
                        Server.Send.Message(
                            "msgfailed",
                            command.user,
                            date,
                            "Message Failed to Send (Invalid Group ID)",
                        )
                    )
                ))
            }
        }

        is Client.Show -> {
            when (val content = command.content) {
                is Client.Show.User -> {
                    sChannel.send(Json.encodeToString(command.toServer(
                        400,
                        data = listOf(content.id, "showfailed"),
                        reason = "Showing the information of a users is currently unsupported"
                    )))
                }
                is Client.Show.Message -> {
                    val message = groups[content.group]?.messages?.find { it.id == content.id }
                    if (message != null) {
                        sChannel.send(Json.encodeToString(command.toServer(
                            200,
                            data = listOf(message.id, message.message)
                        )))
                    } else {
                        sChannel.send(Json.encodeToString(command.toServer(
                            400,
                            data = listOf(content.id, "showfailed"),
                            reason = "Invalid message id: ${content.id}"
                        )))
                    }
                }
            }
        }
    }
}

context(LoggingContext, GroupContext, UserContext)
suspend fun cleanup(client: Socket) {
    val user = users.entries.find { (_, user: User) -> user.socket == client }!!.key
    users[user]!!.groups.clear()
    users.remove(user)
    for (group in groups.values) {
        if (group.users.remove(user) != null) {
            for (otherUser in group.users.values) {
                if (group.id == otherUser.groups.first()) {
                    otherUser.socket.writeChannel().send(
                        Json.encodeToString<Server>(
                            Server.Leave(
                                user,
                                200,
                                Server.Content(
                                    group.id,
                                    "$user has left ${group.id}"
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}

suspend fun Socket.readChannel() = withContext(Dispatchers.IO) {
    this@readChannel.getInputStream()
}.reader().buffered()

suspend fun Socket.writeChannel() = DataOutputStream(withContext(Dispatchers.IO) {
    this@writeChannel.getOutputStream()
})

suspend fun Socket.channels() = this.readChannel() to this.writeChannel()

suspend fun DataOutputStream.send(message: String) = withContext(Dispatchers.IO) {
    this.run {
        writeBytes("$message\n")
        flush()
    }
}

fun Client.toServer(status: Int, data: List<Data> = listOf(), reason: String = ""): Server = when (this) {
    is Client.Connect -> Server.Connect(
        this.user,
        status,
        "connection ${if (status == 200) "" else "un"}successful"
    )
    is Client.Join -> Server.Join(
        this.user,
        status,
        Server.Content(
            this.group,
            "join ${this.group} ${if (status == 200) "" else "un"}successful"
        )
    )
    is Client.Leave -> Server.Leave(
        this.user,
        status,
        Server.Content(
            this.group,
            "leave ${this.group} ${if (status == 200) "" else "un"}successful"
        )
    )
    is Client.Ls -> Server.Ls(
        this.user,
        status,
        Server.Ls.Content(
            this.content.group,
            when (this.content) {
                is Client.Ls.User -> "user"
                is Client.Ls.Message -> "message"
                is Client.Ls.Group -> "group"
            },
            data,
            reason,
        ),
    )
    is Client.Send -> throw Exception("Unreachable")
    is Client.Show -> Server.Show(this.user, status, Server.Show.Content(
        data[0],
        data[1],
        reason,
    ))
}