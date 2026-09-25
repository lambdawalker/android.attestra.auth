package com.apexfission.android.attestra.auth.email

import org.junit.Assert.assertEquals
import org.junit.Test

class ProofGeneratorTest {
    @Test fun challengeIsS256Of32ByteClientSecret() {
        val generator = ProofGenerator { bytes -> bytes.indices.forEach { bytes[it] = (it + 1).toByte() } }
        val proof = generator.create()
        assertEquals("AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA", proof.tokenA)
        assertEquals("riFsLvUkejeCwTXvonmj5M3GEJQnD10r5YxiBLemEsk", proof.challenge)
    }
}
