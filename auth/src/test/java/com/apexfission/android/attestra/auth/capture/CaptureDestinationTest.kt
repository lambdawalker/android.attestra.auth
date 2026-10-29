package com.apexfission.android.attestra.auth.capture

import com.apexfission.android.attestra.auth.identity.DocumentSide
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureDestinationTest {
    private fun record(state: String = "ready", extraction: CardExtraction? = null) =
        CaptureRecord("a".repeat(32), 1, 1, "v1", "id_card", state, 9999999999, extraction = extraction)

    @Test fun freshEntryAndBackendPendingShowLoading() {
        assertEquals(CaptureDestination.LOADING, captureDestination(CaptureUi(), initializing = true))
        for (r in listOf(record("finalizing"), record(), record(extraction = CardExtraction("pending")))) {
            assertEquals(CaptureDestination.LOADING, captureDestination(CaptureUi(record = r)))
        }
    }
    @Test fun cameraDoesNotShowCaptureManagement() {
        assertEquals(CaptureDestination.CAMERA, captureDestination(CaptureUi(camera = DocumentSide.FRONT)))
    }
    @Test fun failedExtractionOpensReviewEvenWithEmptyData() {
        val ui = CaptureUi(record = record(extraction = CardExtraction("failed")), draft = CardData())
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui))
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui.copy(busy = true, message = "Save failed")))
    }
    @Test fun completeExtractionOpensReview() {
        assertEquals(CaptureDestination.REVIEW, captureDestination(CaptureUi(
            record = record(extraction = CardExtraction("complete")), draft = CardData(fullName = "Test"))))
    }
    @Test fun transportAndFileFailuresOfferRecoveryRatherThanBlankIdentity() {
        assertEquals(CaptureDestination.RECOVERY, captureDestination(CaptureUi(record = record(), message = "Offline")))
        assertEquals(CaptureDestination.RECOVERY, captureDestination(CaptureUi(record = record("failed"))))
    }
    @Test fun onlyAcknowledgedMatchingReviewFinishes() {
        val data = CardData(fullName = "Saved Person")
        val r = record(extraction = CardExtraction("failed")).copy(reviewed = ReviewedCard(data, 123, "MX"))
        val ui = CaptureUi(record = r, draft = data, originCountry = "MX")
        assertEquals("FINISH", captureDestination(ui).name)
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui.copy(busy = true)))
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui.copy(message = "Save failed")))
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui.copy(reviewDirty = true)))
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui.copy(draft = data.copy(fullName = "Unsaved"))))
        assertEquals(CaptureDestination.REVIEW, captureDestination(ui.copy(originCountry = "US")))
        assertEquals(CaptureDestination.RECOVERY, captureDestination(ui.copy(signInRequired = true)))
    }

}
