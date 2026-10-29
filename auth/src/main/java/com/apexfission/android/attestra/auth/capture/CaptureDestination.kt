package com.apexfission.android.attestra.auth.capture

/** UI routing is independent of camera ownership and backend request orchestration. */
internal enum class CaptureDestination { CAMERA, LOADING, REVIEW, RECOVERY }

internal fun captureDestination(ui: CaptureUi, initializing: Boolean = false): CaptureDestination = when {
    initializing -> CaptureDestination.LOADING
    ui.camera != null -> CaptureDestination.CAMERA
    ui.record?.canReviewCard() == true && ui.draft != null -> CaptureDestination.REVIEW
    ui.message != null || ui.signInRequired -> CaptureDestination.RECOVERY
    ui.busy || ui.record.isProcessingCard() -> CaptureDestination.LOADING
    else -> CaptureDestination.RECOVERY
}

internal fun CaptureRecord?.isProcessingCard(): Boolean =
    this?.state == "finalizing" || (this?.state == "ready" && extraction?.status in setOf(null, "pending"))
