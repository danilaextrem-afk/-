package ru.vladis.realtorai.data

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** An authenticated stream with a terminal record, so a truncated archive cannot be restored. */
object BackupEncryption {
    private val magic = "RAIENC01".toByteArray(Charsets.US_ASCII)
    private const val chunkSize = 64 * 1024
    private const val tagSize = 16
    private const val maxChunks = 8192

    fun detected(input: InputStream): Boolean {
        require(input.markSupported())
        input.mark(magic.size)
        val header = ByteArray(magic.size)
        var read = 0
        while (read < header.size) {
            val count = input.read(header, read, header.size - read)
            if (count == -1) break
            read += count
        }
        input.reset()
        return read == header.size && header.contentEquals(magic)
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size in 8..256)
        val parameters = PBEKeySpec(password, salt, 240_000, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(parameters).encoded, "AES")
        } finally { parameters.clearPassword() }
    }

    private fun cipher(mode: Int, key: SecretKeySpec, prefix: ByteArray, index: Int, size: Int): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            val nonce = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
            init(mode, key, GCMParameterSpec(128, nonce))
            updateAAD(magic)
            updateAAD(ByteBuffer.allocate(4).putInt(size).array())
        }

    fun output(stream: OutputStream, password: CharArray): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val prefix = ByteArray(8).also(random::nextBytes)
        val secret = key(password, salt)
        val target = DataOutputStream(stream.buffered())
        target.write(magic)
        target.write(salt)
        target.write(prefix)
        return object : OutputStream() {
            private val buffer = ByteArray(chunkSize)
            private var used = 0
            private var index = 0
            private var closed = false

            private fun record(size: Int) {
                check(index < maxChunks) { "Архив превышает допустимый размер" }
                val encrypted = cipher(Cipher.ENCRYPT_MODE, secret, prefix, index++, size).doFinal(buffer, 0, size)
                target.writeInt(size)
                target.write(encrypted)
                used = 0
            }

            override fun write(value: Int) {
                check(!closed)
                buffer[used++] = value.toByte()
                if (used == buffer.size) record(used)
            }

            override fun write(data: ByteArray, off: Int, len: Int) {
                check(!closed)
                var offset = off
                val end = off + len
                while (offset < end) {
                    val count = minOf(buffer.size - used, end - offset)
                    data.copyInto(buffer, used, offset, offset + count)
                    used += count
                    offset += count
                    if (used == buffer.size) record(used)
                }
            }

            override fun close() {
                if (closed) return
                closed = true
                try {
                    if (used > 0) record(used)
                    record(0)
                } finally {
                    buffer.fill(0)
                    target.close()
                }
            }
        }
    }

    fun decrypt(source: InputStream, output: OutputStream, password: CharArray) {
        val input = DataInputStream(source.buffered())
        val header = ByteArray(magic.size).also(input::readFully)
        require(header.contentEquals(magic)) { "Неверный формат защищённой копии" }
        val salt = ByteArray(16).also(input::readFully)
        val prefix = ByteArray(8).also(input::readFully)
        val secret = key(password, salt)
        for (index in 0 until maxChunks) {
            val size = input.readInt()
            require(size in 0..chunkSize) { "Повреждённая резервная копия" }
            val data = ByteArray(size + tagSize).also(input::readFully)
            val clear = cipher(Cipher.DECRYPT_MODE, secret, prefix, index, size).doFinal(data)
            if (size == 0) {
                require(input.read() == -1) { "Лишние данные после защищённого архива" }
                return
            }
            try { output.write(clear) } finally { clear.fill(0) }
        }
        error("Отсутствует завершающая запись архива")
    }
}
