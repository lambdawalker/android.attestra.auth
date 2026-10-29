package com.apexfission.android.attestra.auth.capture

/** UI routing is independent of camera ownership and backend request orchestration. */
internal enum class CaptureDestination { CAMERA, LOADING, REVIEW, FINISH, RECOVERY }

internal fun captureDestination(ui: CaptureUi, initializing: Boolean = false, reviewSavedDetails: Boolean = false): CaptureDestination = when {
    initializing -> CaptureDestination.LOADING
    ui.camera != null -> CaptureDestination.CAMERA
    ui.signInRequired -> CaptureDestination.RECOVERY
    !reviewSavedDetails && ui.hasSavedReview() -> CaptureDestination.FINISH
    ui.record?.canReviewCard() == true && ui.draft != null -> CaptureDestination.REVIEW
    ui.message != null || ui.signInRequired -> CaptureDestination.RECOVERY
    ui.busy || ui.record.isProcessingCard() -> CaptureDestination.LOADING
    else -> CaptureDestination.RECOVERY
}

internal fun CaptureRecord?.isProcessingCard(): Boolean =
    this?.state == "finalizing" || (this?.state == "ready" && extraction?.status == "pending")

/** Finish only when the server has acknowledged the exact visible values. */
internal fun CaptureUi.hasSavedReview(): Boolean =
    !busy && message == null && !signInRequired && !reviewDirty &&
        record?.canReviewCard() == true && draft != null &&
        record?.reviewed?.data == draft && record?.reviewed?.originCountry == originCountry
