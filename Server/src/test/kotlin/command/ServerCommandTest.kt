package command

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ServerCommandTest : StringSpec({
    "Connect command should output correct JSON" {
        val user = "connectUser"
        val status = 200
        val content = "Successfully connected"
        Json.parseToJsonElement(
            Json.encodeToString<Server>(
                Server.Connect(
                    user,
                    status,
                    content
                )
            )
        ) shouldBe jsonElement(
            user,
            "connect",
            status,
            "\"$content\""
        )
    }
})

fun jsonElement(username: String, command: String, status: Int, content: String) = Json.parseToJsonElement(
    """
        {
            "user": "$username",
            "command": "$command",
            "status": $status,
            "content": $content
        }
    """.trimIndent()
)