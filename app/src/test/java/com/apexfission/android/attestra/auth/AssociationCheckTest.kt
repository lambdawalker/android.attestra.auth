package com.apexfission.android.attestra.auth

import org.junit.Assert.*
import org.junit.Test

class AssociationCheckTest {
    private val fingerprint = List(32) { "AB" }.joinToString(":")
    private fun statement(relation: String, packageName: String = "com.example.app", cert: String = fingerprint) =
        """{"relation":["delegate_permission/common.$relation"],"target":{"namespace":"android_app","package_name":"$packageName","sha256_cert_fingerprints":["$cert"]}}"""

    @Test fun combinesRelationsForTheSameIdentity() {
        val result = associationApproval("[${statement("handle_all_urls")},${statement("get_login_creds", cert = fingerprint.lowercase())}]", "com.example.app", setOf(fingerprint))
        assertTrue(result.appLinks)
        assertTrue(result.passkeys)
        assertNull(result.warning())
    }

    @Test fun missingIdentityWarnsForBothFeatures() {
        val result = associationApproval("[${statement("handle_all_urls", packageName = "com.other.app")}]", "com.example.app", setOf(fingerprint))
        assertFalse(result.appLinks)
        assertFalse(result.passkeys)
        assertNotNull(result.warning())
    }

    @Test fun unrelatedCertificateCannotApproveInstalledBuild() {
        val result = associationApproval("[${statement("handle_all_urls", cert = List(32) { "CD" }.joinToString(":"))}]", "com.example.app", setOf(fingerprint))
        assertFalse(result.appLinks)
    }

    @Test fun partialApprovalIdentifiesMissingFeature() {
        val result = associationApproval("[${statement("handle_all_urls")}]", "com.example.app", setOf(fingerprint))
        assertTrue(result.appLinks)
        assertFalse(result.passkeys)
        assertTrue(result.warning()!!.contains("passkeys"))
    }

    @Test fun allCurrentSignersMustBeApproved() {
        val result = associationApproval("[${statement("handle_all_urls")}]", "com.example.app", setOf(fingerprint, "CD".repeat(32)))
        assertFalse(result.appLinks)
    }

    @Test(expected = IllegalArgumentException::class) fun missingInstalledSignersIsNotAMismatch() {
        associationApproval("[]", "com.example.app", emptySet())
    }

    @Test(expected = IllegalStateException::class) fun malformedDocumentIsNotAMismatch() {
        associationApproval("{}", "com.example.app", setOf(fingerprint))
    }
}
