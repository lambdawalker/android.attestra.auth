package com.apexfission.android.attestra.auth.email

/** Matching metadata is kept only in memory, alongside the transient password. */
internal fun pendingPasswordMatches(expectedEmail: String?, expectedRequestId: String?, confirmedEmail: String?, confirmedRequestId: String?): Boolean =
    !expectedEmail.isNullOrBlank() && !expectedRequestId.isNullOrBlank() && !confirmedEmail.isNullOrBlank() &&
        expectedRequestId == confirmedRequestId && expectedEmail.trim().equals(confirmedEmail.trim(), ignoreCase = true)
