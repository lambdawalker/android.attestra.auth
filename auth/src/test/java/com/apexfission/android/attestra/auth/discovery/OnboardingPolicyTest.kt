package com.apexfission.android.attestra.auth.discovery

import com.apexfission.android.attestra.auth.passkey.AccountStatus
import org.junit.Assert.*
import org.junit.Test

class OnboardingPolicyTest {
    private val required = OnboardingPolicy(Requirement.REQUIRED, Requirement.REQUIRED,
        Requirement.REQUIRED, PasswordPlacement.AFTER_VERIFICATION, PasswordPolicy())
    @Test fun serverCompletionAndRequiredFactsAreBothNecessary() {
        assertEquals(OnboardingStep.PASSWORD, required.next(AccountStatus(), setOf(OnboardingStep.PASSWORD)))
        assertEquals(OnboardingStep.PASSKEY, required.next(AccountStatus(passwordRegistered = true), setOf(OnboardingStep.PASSKEY)))
        assertEquals(OnboardingStep.ID_CAPTURE, required.next(AccountStatus(true, true), setOf(OnboardingStep.ID_CAPTURE)))
        assertEquals(OnboardingStep.REFRESH, required.next(AccountStatus(true, true, true)))
        assertEquals(OnboardingStep.COMPLETE, required.next(AccountStatus(true, true, true, true)))
        assertEquals(OnboardingStep.PASSWORD, required.next(AccountStatus(onboardingComplete = true)))
    }
    @Test fun optionalSkipsAndDisabledStepsDoNotAssertCompletion() {
        val optional = required.copy(idCapture = Requirement.OPTIONAL, passkey = Requirement.DISABLED, password = Requirement.DISABLED, passwordPlacement = PasswordPlacement.HIDDEN)
        assertEquals(OnboardingStep.ID_CAPTURE, optional.next(AccountStatus()))
        assertEquals(OnboardingStep.REFRESH, optional.next(AccountStatus(), setOf(OnboardingStep.ID_CAPTURE)))
        assertEquals(OnboardingStep.COMPLETE, optional.next(AccountStatus(onboardingComplete = true), setOf(OnboardingStep.ID_CAPTURE)))
    }
    @Test fun passwordRulesRejectWhitespaceUnicodeAndForbiddenClasses() {
        val rules = PasswordPolicy(8, 16, CharacterRule.REQUIRED, CharacterRule.DISALLOWED, CharacterRule.REQUIRED, CharacterRule.ALLOWED)
        assertTrue(rules.problems("abcdefgh1!").isEmpty())
        for (bad in listOf("Abcdefgh1", "abcdefgh", "abcdef 1", "abcdefé1", "abcd1", "abcdefgh123456789")) assertTrue(bad, rules.problems(bad).isNotEmpty())
    }
    @Test fun strengthIsIndependentOfComplianceAndPenalizesPredictablePasswords() {
        val rules = PasswordPolicy(8, 128)
        assertTrue(rules.problems("passwordpassword").isEmpty())
        assertEquals("Weak", rules.strength("passwordpassword"))
        assertEquals("Weak", rules.strength("aaaaaaaaaaaaaaaaaaaa"))
        assertEquals("Weak", rules.strength("abcdefghijklmnop"))
        assertEquals("Strong", rules.strength("K8!vR2#zM9@qL4\$x"))
        assertEquals("Fair", rules.strength("jungleRiver7"))
    }
    @Test fun impossibleAndContradictoryPoliciesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { required.copy(passwordPlacement = PasswordPlacement.HIDDEN).validate() }
        assertThrows(IllegalArgumentException::class.java) { PasswordPolicy(7, 128).validate() }
        assertThrows(IllegalArgumentException::class.java) { PasswordPolicy(12, 8).validate() }
        assertThrows(IllegalArgumentException::class.java) { PasswordPolicy(8, 128, CharacterRule.DISALLOWED, CharacterRule.DISALLOWED, CharacterRule.DISALLOWED, CharacterRule.DISALLOWED).validate() }
    }
}
