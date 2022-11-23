@file:OptIn(ExperimentalSerializationApi::class)

package command

import content.IgnoredContent
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("command")
sealed interface Client {
    val user: String
    val command: String

    @Serializable
    @SerialName("connect")
    data class Connect(override val user: String, val content: IgnoredContent) : Client {
        override val command: String
            get() = "connect"
    }

    @Serializable
    @SerialName("join")
    data class Join(override val user: String, @SerialName("content") val group: String) : Client {
        override val command: String
            get() = "join"
    }

    @Serializable
    @SerialName("leave")
    data class Leave(override val user: String, @SerialName("content") val group: String) : Client {
        override val command: String
            get() = "leave"
    }

    @Serializable
    @SerialName("list")
    data class Ls(override val user: String, val content: Content) : Client {

        override val command: String
            get() = "ls"

        @Serializable
        sealed interface Content {
            val group: String
            val amount: Int
        }

        @Serializable
        @SerialName("user")
        data class User(override val group: String, override val amount: Int) : Content

        @Serializable
        @SerialName("message")
        data class Message(override val group: String, override val amount: Int) : Content

        @Serializable
        @SerialName("group")
        data class Group(override val group: String, override val amount: Int) : Content
    }

    @Serializable
    @SerialName("send")
    data class Send(override val user: String, @SerialName("content") val message: Message) : Client {
        override val command: String
            get() = "send"

        @Serializable
        data class Message(val group: String, val subject: String, val message: String)
    }

    @Serializable
    @SerialName("show")
    data class Show(override val user: String, val content: Content) : Client {
        override val command: String
            get() = "show"

        @Serializable
        sealed interface Content {
            val group: String
            val id: String
        }

        @Serializable
        @SerialName("user")
        data class User(override val group: String, override val id: String) : Content

        @Serializable
        @SerialName("message")
        data class Message(override val group: String, override val id: String) : Content
    }
}