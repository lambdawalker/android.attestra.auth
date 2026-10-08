package com.apexfission.android.attestra.identitymock

import com.apexfission.android.attestra.auth.identity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MockIdentityServerTest {
    private class Store : MockStateStore {
        var json: String? = null
        override fun read() = json
        override fun write(value: String) { json = value }
    }
    private val id = UUID.randomUUID().toString()
    private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte())
    @Test fun selectedScenarioSurvivesServerRecreationForNewEvidence() = runBlocking {
        val store = Store()
        val server = MockIdentityServer(store)
        server.scenario = MockScenario.UNREADABLE
        val resumed = MockIdentityServer(store)
        assertEquals(MockScenario.UNREADABLE, resumed.scenario)
        val client = resumed.client()
        val api = IdentityApi(MockIdentityServer.ORIGIN, client) { MockIdentityServer.TOKEN }
        val draft = api.create(id)
        for (side in DocumentSide.entries) api.upload(id, draft.evidenceVersion, side, jpeg)
        assertFalse(api.extract(id, draft.evidenceVersion).readable)
        client.close()
    }
    @Test fun realClientExercisesMockHttpContractAndPersistentStatus() = runBlocking {
        val store = Store()
        val server = MockIdentityServer(store, MockScenario.APPROVED)
        val client = server.client(); val api = IdentityApi(MockIdentityServer.ORIGIN, client) { MockIdentityServer.TOKEN }
        val draft = api.create(id)
        assertEquals(draft, api.create(id))
        api.upload(id, draft.evidenceVersion, DocumentSide.FRONT, jpeg)
        api.upload(id, draft.evidenceVersion, DocumentSide.BACK, jpeg)
        val extraction = api.extract(id, draft.evidenceVersion)
        val submitted = SubmitIdentity(draft.evidenceVersion, extraction.extracted, extraction.extracted.copy(fullName = "Do not persist this"))
        assertEquals(IdentityOutcome.PENDING, api.submit(id, submitted).decision)
        assertEquals(IdentityOutcome.PENDING, api.status(id)!!.decision)
        assertEquals(IdentityOutcome.APPROVED, api.status(id)!!.decision)
        assertFalse(store.json!!.contains("Do not persist this"))
        client.close()
        val resumedClient = MockIdentityServer(store).client()
        assertEquals(IdentityOutcome.APPROVED, IdentityApi(MockIdentityServer.ORIGIN, resumedClient) { MockIdentityServer.TOKEN }.status(id)!!.decision)
        resumedClient.close()
    }
    @Test fun acceptedSubmissionRejectsChangedBodyAndEvidenceOverwrite() = runBlocking {
        val client = MockIdentityServer(Store()).client()
        val api = IdentityApi(MockIdentityServer.ORIGIN, client) { MockIdentityServer.TOKEN }
        val draft = api.create(id)
        for (side in DocumentSide.entries) api.upload(id, draft.evidenceVersion, side, jpeg)
        val extraction = api.extract(id, draft.evidenceVersion)
        val body = SubmitIdentity(draft.evidenceVersion, extraction.extracted, extraction.extracted)
        api.submit(id, body); assertTrue(api.submit(id, body).accepted)
        try { api.submit(id, body.copy(corrected = body.corrected.copy(fullName = "Changed"))); fail() }
        catch (e: IdentityApiException) { assertEquals(409, e.status) }
        try { api.upload(id, draft.evidenceVersion, DocumentSide.FRONT, jpeg); fail() }
        catch (e: IdentityApiException) { assertEquals(409, e.status) }
        client.close()
    }
    @Test fun unknownSubmissionAndWrongCredentialAreNotSuccessfulResponses() = runBlocking {
        val client = MockIdentityServer(Store()).client()
        assertNull(IdentityApi(MockIdentityServer.ORIGIN, client) { MockIdentityServer.TOKEN }.status(id))
        try { IdentityApi(MockIdentityServer.ORIGIN, client) { "wrong" }.status(id); fail() }
        catch (e: IdentityApiException) { assertEquals(401, e.status) }
        client.close()
    }
    @Test fun scenariosKeepInconclusiveAndRejectionDistinct() = runBlocking {
        for (scenario in listOf(MockScenario.REJECTED, MockScenario.INCONCLUSIVE, MockScenario.PENDING)) {
            val client = MockIdentityServer(Store(), scenario).client()
            val api = IdentityApi(MockIdentityServer.ORIGIN, client) { MockIdentityServer.TOKEN }
            val draft = api.create(id)
            for (side in DocumentSide.entries) api.upload(id, draft.evidenceVersion, side, jpeg)
            val extracted = api.extract(id, draft.evidenceVersion).extracted
            api.submit(id, SubmitIdentity(draft.evidenceVersion, extracted, extracted))
            api.status(id)
            val result = api.status(id)!!
            assertEquals(IdentityOutcome.valueOf(scenario.name), result.decision)
            assertEquals(scenario == MockScenario.INCONCLUSIVE, result.canRetry)
            client.close()
        }
    }
}
