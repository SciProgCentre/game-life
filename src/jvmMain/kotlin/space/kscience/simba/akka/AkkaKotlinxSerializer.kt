package space.kscience.simba.akka

import akka.serialization.JSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

class AkkaKotlinxSerializer: JSerializer() {
    override fun identifier(): Int {
        return 80
    }

    override fun toBinary(o: Any?): ByteArray {
        if (o == null) return byteArrayOf()

        val module = SerializersModule {
            polymorphic(o::class)
        }

        val format = Json { serializersModule = module }
        return format.encodeToString(o).toByteArray()
    }

    override fun includeManifest(): Boolean {
        return true
    }

    override fun fromBinaryJava(bytes: ByteArray?, manifest: Class<*>?): Any {
        val module = SerializersModule {

        }

        val format = Json { serializersModule = module }

        return format.decodeFromStream<Any>(bytes!!.inputStream())
    }
}