package ru.vladis.realtorai.data

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class BackupEncryptionTest {
    @Test fun roundTripAndRejectsTamperingWrongPasswordAndTruncatedEnd() {
        val payload = ByteArray(150_000) { (it % 137).toByte() }
        val password = "a strong password".toCharArray()
        val target = ByteArrayOutputStream()
        BackupEncryption.output(target, password).use { it.write(payload) }
        val archive = target.toByteArray()
        assertTrue(BackupEncryption.detected(archive.inputStream()))
        fun restore(bytes: ByteArray, key: CharArray): ByteArray =
            ByteArrayOutputStream().also { BackupEncryption.decrypt(bytes.inputStream(), it, key) }.toByteArray()
        assertArrayEquals(payload, restore(archive, password))
        assertTrue(runCatching { restore(archive, "incorrect password".toCharArray()) }.isFailure)
        assertTrue(runCatching { restore(archive.copyOf(archive.size - 1), password) }.isFailure)
        val corrupt = archive.copyOf().apply { this[50] = (this[50].toInt() xor 1).toByte() }
        assertTrue(runCatching { restore(corrupt, password) }.isFailure)
    }
}
