@file:OptIn(ExperimentalSerializationApi::class)

package command

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

typealias Data = String

@Serializable
@JsonClassDiscriminator("command")
sealed interface Server {
    val user: String
    val command: String
    val status: Int

    @Serializable
    @SerialName("connect")
    data class Connect(
        override val user: String,
        override val status: Int,
        val content: String
    ) : Server {
        override val command: String
            get() = "connect"
    }

    @Serializable
    data class Content(val group: String, val content: String)

    @Serializable
    @SerialName("join")
    data class Join(
        override val user: String,
        override val status: Int,
        val content: Content,
    ) : Server {
        override val command: String
            get() = "join"
    }

    @Serializable
    @SerialName("leave")
    data class Leave(
        override val user: String,
        override val status: Int,
        val content: Content
    ) : Server {
        override val command: String
            get() = "leave"
    }

    @Serializable
    @SerialName("list")
    data class Ls(
        override val user: String,
        override val status: Int,
        val content: Content
    ) : Server {
        override val command: String
            get() = "ls"

        @Serializable
        data class Content(val group: String, val type: String, val data: List<Data>, val reason: String)
    }

    @Serializable
    @SerialName("send")
    data class Send(
        override val user: String,
        override val status: Int,
        val content: Message
    ) : Server {
        override val command: String
            get() = "send"

        @Serializable
        data class Message(val id: String, val user: String, val date: String, val subject: String)
    }

    @Serializable
    @SerialName("show")
    data class Show(
        override val user: String,
        override val status: Int,
        val content: Content
    ) : Server {
        override val command: String
            get() = "show"

        @Serializable
        data class Content(val id: String, val data: Data, val reason: String)
    }
}