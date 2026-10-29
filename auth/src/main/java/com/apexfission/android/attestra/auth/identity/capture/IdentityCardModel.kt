package com.apexfission.android.attestra.auth.identity.capture

// Sentinel catalog from tfmodel 0.1.2 (Apache-2.0), lambdawalker/android.apexfission.carddetector.
// Assets are extracted from that pinned release by prepareCardModelAssets.
internal object IdentityCardModel {
    const val path = "cdl/tflite/Y11-640E197F16.tflite"
    val classes = mapOf(0 to "horizontal_card", 1 to "vertical_card", 2 to "horizontal_card_back",
        3 to "photo", 4 to "slim-barcode", 5 to "pdf417", 6 to "mrz-text", 7 to "barcode", 8 to "qrcode")
    val cardClasses = setOf(0, 1, 2)
}
