package com.apexfission.android.attestra.auth.email

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailFlowControllerTest {
    private val token = "A".repeat(43)
    private val session = AuthSession("access", "id", "refresh", 3600, "Bearer")
    private class MemoryStorage : AuthStorage {
        var saved: PendingEmail? = null
        var tokens: AuthSession? = null
        override fun pending() = saved
        override fun savePending(value: PendingEmail) { saved = value }
        override fun clearPending() { saved = null }
        override fun saveSession(session: AuthSession) { tokens = session }
        override fun session() = tokens
    }
    private inner class FakeApi : EmailVerificationGateway {
        var localCalls = 0
        var manualCalls = 0
        override suspend fun signup(email: String, challenge: String) = token
        override suspend fun resend(requestId: String) = Unit
        override suspend fun confirmLocal(requestId: String, tokenB: String, tokenA: String): AuthSession { localCalls++; return session }
        override suspend fun confirmCode(requestId: String, tokenB: String, tokenC: String): AuthSession { manualCalls++; return session }
    }

    @Test fun localProofConfirmsAfterLinkOpensAndStoresSession() = runBlocking {
        val api = FakeApi(); val store = MemoryStorage()
        val controller = EmailFlowController(api, store, ProofGenerator { bytes -> bytes.fill(1) }, now = { 1_000L }, log = { _, _ -> })
        controller.start("person@example.test")
        assertTrue(controller.screen is EmailScreen.Wait)
        assertEquals(0, api.localCalls)
        controller.openLink(VerificationLink(token, token))
        assertEquals(1, api.localCalls)
        assertEquals(session, store.tokens)
        assertEquals(null, store.saved)
        assertTrue(controller.screen is EmailScreen.Verified)
    }

    @Test fun anotherDeviceNeverConfirmsBeforeSixDigitSubmission() = runBlocking {
        val api = FakeApi(); val store = MemoryStorage()
        val controller = EmailFlowController(api, store, ProofGenerator { bytes -> bytes.fill(1) }, log = { _, _ -> })
        controller.openLink(VerificationLink(token, token))
        assertTrue(controller.screen is EmailScreen.Code)
        assertEquals(0, api.localCalls)
        assertEquals(0, api.manualCalls)
        controller.confirmCode("12345")
        assertEquals(0, api.manualCalls)
        controller.confirmCode("000123")
        assertEquals(1, api.manualCalls)
        assertTrue(controller.screen is EmailScreen.Verified)
    }

    @Test fun replacedLinkNeverUsesStoredAOnOtherRequest() = runBlocking {
        val api = FakeApi(); val store = MemoryStorage()
        val controller = EmailFlowController(api, store, ProofGenerator { bytes -> bytes.fill(1) }, log = { _, _ -> })
        controller.start("person@example.test")
        controller.openLink(VerificationLink("B".repeat(43), token))
        assertFalse(controller.screen is EmailScreen.Verified)
        assertEquals(0, api.localCalls)
    }
}
