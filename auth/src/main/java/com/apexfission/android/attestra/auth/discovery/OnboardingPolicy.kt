package com.apexfission.android.attestra.auth.discovery

import com.apexfission.android.attestra.auth.passkey.AccountStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable enum class Requirement { @SerialName("required") REQUIRED, @SerialName("optional") OPTIONAL, @SerialName("disabled") DISABLED }
@Serializable enum class PasswordPlacement { @SerialName("with_email") WITH_EMAIL, @SerialName("after_verification") AFTER_VERIFICATION, @SerialName("hidden") HIDDEN }
@Serializable enum class CharacterRule { @SerialName("allowed") ALLOWED, @SerialName("required") REQUIRED, @SerialName("disallowed") DISALLOWED }
enum class OnboardingStep { PASSWORD, PASSKEY, ID_CAPTURE, REFRESH, COMPLETE }

@Serializable data class PasswordPolicy(
    val minLength: Int = 12, val maxLength: Int = 128,
    val lowercase: CharacterRule = CharacterRule.ALLOWED, val uppercase: CharacterRule = CharacterRule.ALLOWED,
    val numbers: CharacterRule = CharacterRule.ALLOWED, val symbols: CharacterRule = CharacterRule.ALLOWED,
) {
    private fun classes() = listOf("lowercase letters" to lowercase, "uppercase letters" to uppercase, "numbers" to numbers, "symbols" to symbols)
    fun validate() {
        require(minLength in 8..128 && maxLength in minLength..128)
        require(classes().any { it.second != CharacterRule.DISALLOWED })
    }
    fun requirements(): List<String> = listOf("$minLength–$maxLength characters; no spaces or non-ASCII characters") +
        classes().filter { it.second != CharacterRule.ALLOWED }.map { (name, rule) -> if (rule == CharacterRule.REQUIRED) "Include $name" else "Do not use $name" }
    fun problems(value: String): List<String> {
        val issues = mutableListOf<String>()
        if (value.length !in minLength..maxLength) issues += "Use $minLength–$maxLength characters"
        if (value.any { it !in '!'..'~' }) issues += "Use ASCII letters, numbers and punctuation without spaces"
        val present = listOf(value.any { it in 'a'..'z' }, value.any { it in 'A'..'Z' }, value.any { it in '0'..'9' }, value.any { it in '!'..'~' && !it.isLetterOrDigit() })
        classes().forEachIndexed { i, (name, rule) ->
            if (rule == CharacterRule.REQUIRED && !present[i]) issues += "Include $name"
            if (rule == CharacterRule.DISALLOWED && present[i]) issues += "Do not use $name"
        }
        return issues
    }
    /** Local heuristic only: independent of policy compliance, not a breach check or guarantee. */
    fun strength(value: String): String {
        val normalized = value.lowercase()
        val common = listOf("password", "qwerty", "letmein", "welcome", "123456", "admin")
        val repeated = (1..(value.length / 2)).any { size ->
            value.length % size == 0 && value == value.take(size).repeat(value.length / size)
        }
        val sequential = normalized.length >= 4 && normalized.zipWithNext().all { (a, b) -> b.code - a.code == 1 }
        if (value.length < 10 || value.toSet().size < 5 || repeated || sequential || common.any { normalized.contains(it) }) return "Weak"
        val classes = listOf(value.any { it in 'a'..'z' }, value.any { it in 'A'..'Z' },
            value.any { it in '0'..'9' }, value.any { it in '!'..'~' && !it.isLetterOrDigit() }).count { it }
        return if ((value.length >= 16 && classes >= 3) || (value.length >= 24 && value.toSet().size >= 12)) "Strong" else "Fair"
    }
}

@Serializable data class OnboardingPolicy(
    val idCapture: Requirement, val passkey: Requirement, val password: Requirement,
    val passwordPlacement: PasswordPlacement, val passwordPolicy: PasswordPolicy,
) {
    fun validate() {
        require((password == Requirement.DISABLED) == (passwordPlacement == PasswordPlacement.HIDDEN))
        passwordPolicy.validate()
    }
    fun next(status: AccountStatus, skipped: Set<OnboardingStep> = emptySet()): OnboardingStep {
        val steps = listOf(Triple(OnboardingStep.PASSWORD, password, status.passwordRegistered),
            Triple(OnboardingStep.PASSKEY, passkey, status.passkeyRegistered),
            Triple(OnboardingStep.ID_CAPTURE, idCapture, status.idCaptured))
        // Required steps always take precedence over optional offers and cannot be skipped locally.
        steps.firstOrNull { it.second == Requirement.REQUIRED && !it.third }?.let { return it.first }
        steps.firstOrNull { it.second == Requirement.OPTIONAL && !it.third && it.first !in skipped }?.let { return it.first }
        return if (status.onboardingComplete) OnboardingStep.COMPLETE else OnboardingStep.REFRESH
    }
    companion object {
        fun legacy(capture: Boolean) = OnboardingPolicy(if (capture) Requirement.OPTIONAL else Requirement.DISABLED,
            Requirement.REQUIRED, Requirement.DISABLED, PasswordPlacement.HIDDEN, PasswordPolicy())
    }
}
