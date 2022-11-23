package content

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = IgnoredContent.Serializer::class)
object IgnoredContent {
    override fun toString(): String = "<ignored>"

    class Serializer : KSerializer<IgnoredContent> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("Ignored Content", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): IgnoredContent {
            return IgnoredContent
        }

        override fun serialize(encoder: Encoder, value: IgnoredContent) {
            encoder.encodeString("")
        }
    }
}