package com.apexfission.android.attestra.auth.email
import org.junit.Assert.*
import org.junit.Test
class PendingPasswordTest {
    @Test fun pendingPasswordRequiresSameRequestAndConfirmedAccount() {
        assertTrue(pendingPasswordMatches(" Person@Example.com ", "request", "person@example.com", "request"))
        assertFalse(pendingPasswordMatches("person@example.com", "request", "other@example.com", "request"))
        assertFalse(pendingPasswordMatches("person@example.com", "request", "person@example.com", "other"))
        assertFalse(pendingPasswordMatches("person@example.com", null, "person@example.com", null))
        assertFalse(pendingPasswordMatches("person@example.com", "request", null, "request"))
    }
}
