package com.apexfission.android.attestra.auth

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalUserResetTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun clearsAuthAndMockStateButPreservesUnrelatedFiles() {
        val directory = temporary.newFolder()
        val record = File(directory, "identity-mock-user.json").apply { writeText("sample") }
        val pending = File(directory, "identity-mock-user.json.tmp").apply { writeText("sample") }
        val unrelated = File(directory, "settings.json").apply { writeText("keep") }
        var cleared = false
        resetLocalUser(directory) { cleared = true }
        assertTrue(cleared)
        assertFalse(record.exists())
        assertFalse(pending.exists())
        assertEquals("keep", unrelated.readText())
        resetLocalUser(directory) {} // Repeating a completed reset is safe.
    }

    @Test fun failsWhenMockStorageCannotBeListed() {
        val notDirectory = temporary.newFile()
        assertThrows(IllegalStateException::class.java) { resetLocalUser(notDirectory) {} }
    }

    @Test fun propagatesAuthFailureInsteadOfClaimingSuccess() {
        val directory = temporary.newFolder()
        assertThrows(IllegalStateException::class.java) {
            resetLocalUser(directory) { error("Storage unavailable") }
        }
    }
}
