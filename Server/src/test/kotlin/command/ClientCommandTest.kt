package command

import content.IgnoredContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ClientCommandTest : StringSpec({
    "parsing json with \"command\": \"connect\" should return Connect" {
        val user = "userConnect"
        json<Client>(user, "connect", "\"\"") shouldBe Client.Connect(user, IgnoredContent)
    }

    "parsing json with \"command\": \"join\" should return Join" {
        val user = "userJoin"
        val group = "groupJoin"
        json<Client>(user, "join", "\"$group\"") shouldBe Client.Join(user, group)
    }

    "parsing json with \"command\": \"leave\" should return Leave" {
        val user = "userLeave"
        val group = "groupLeave"
        json<Client>(user, "leave", "\"$group\"") shouldBe Client.Leave(user, group)
    }

    "parsing json with \"command\": \"list\" and \"type\": \"message\" should return Ls(Message)" {
        val user = "userListMessage"
        val group = "groupListMessage"
        val amount = -1
        val content = """
            {
                "type": "message",
                "group": "$group",
                "amount": $amount
            }
        """.trimIndent()
        json<Client>(user, "list", content) shouldBe Client.Ls(
            user,
            Client.Ls.Message(group, amount)
        )
    }

    "parsing json with \"command\": \"list\" and \"type\": \"user\" should return Ls(User)" {
        val user = "userListUser"
        val group = "groupListUser"
        val amount = -1
        val content = """
            {
                "type": "user",
                "group": "$group",
                "amount": $amount
            }
        """.trimIndent()
        json<Client>(user, "list", content) shouldBe Client.Ls(user, Client.Ls.User(group, amount))
    }

    "parsing json with \"command\": \"send\" should return Send" {
        val user = "userSend"
        val group = "groupSend"
        val subject = "subjectSend"
        val message = "subjectMessage"
        val content = """
            {
                "group": "$group",
                "subject": "$subject",
                "message": "$message"
            }
        """.trimIndent()
        json<Client>(user, "send", content) shouldBe Client.Send(
            user,
            Client.Send.Message(group, subject, message)
        )
    }

    "parsing json with \"command\": \"show\" and \"type\": \"message\" should return Show(Message)" {
        val user = "userShow"
        val group = "groupShow"
        val id = "idShow"
        val content = """
            {
                "type": "message",
                "group": "$group",
                "id": "$id"
            }
        """.trimIndent()
        json<Client>(user, "show", content) shouldBe Client.Show(user, Client.Show.Message(group, id))
    }

    "parsing json with \"command\": \"show\" and \"type\": \"user\" should return Show(User)" {
        val user = "userShow"
        val group = "groupShow"
        val id = "idShow"
        val content = """
            {
                "type": "user",
                "group": "$group",
                "id": "$id"
            }
        """.trimIndent()
        json<Client>(user, "show", content) shouldBe Client.Show(user, Client.Show.User(group, id))
    }
})

inline fun <reified R> json(username: String, command: String, content: String): R = Json.decodeFromString(
    """
        {
            "user": "$username",
            "command": "$command",
            "content": $content
        }
    """.trimIndent()
)