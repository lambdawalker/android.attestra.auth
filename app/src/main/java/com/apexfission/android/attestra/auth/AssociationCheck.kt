package com.apexfission.android.attestra.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/** Public website configuration only; this does not establish Android's device verification state. */
internal data class AssociationApproval(val appLinks: Boolean, val passkeys: Boolean) {
    fun warning(): String? = when {
        !appLinks && !passkeys -> "This build isn't approved for this domain. Passkeys and verified App Links won't work."
        !appLinks -> "This build isn't approved for verified App Links on this domain."
        !passkeys -> "This build isn't approved for passkeys on this domain."
        else -> null
    }
}

internal fun associationApproval(document: String, packageName: String, fingerprints: Set<String>): AssociationApproval {
    require(fingerprints.isNotEmpty()) { "No signing certificates available" }
    val statements = Json.parseToJsonElement(document) as? JsonArray
        ?: error("Expected an association array")
    fun normalize(value: String): String = value.replace(":", "").uppercase()
    val approved = fingerprints.associate { normalize(it) to mutableSetOf<String>() }
    for (statement in statements) {
        val item = statement as? JsonObject ?: error("Invalid association statement")
        val target = item["target"] as? JsonObject ?: error("Invalid association target")
        if (target["namespace"]?.jsonPrimitive?.content != "android_app" ||
            target["package_name"]?.jsonPrimitive?.content != packageName) continue
        val certs = target["sha256_cert_fingerprints"] as? JsonArray ?: error("Missing fingerprints")
        val relations = item["relation"] as? JsonArray ?: error("Missing relations")
        val names = relations.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content ?: error("Invalid relation") }
        for (cert in certs) {
            val value = (cert as? JsonPrimitive)?.takeIf { it.isString }?.content ?: error("Invalid certificate")
            approved[normalize(value)]?.addAll(names)
        }
    }
    return AssociationApproval(
        approved.values.all { "delegate_permission/common.handle_all_urls" in it },
        approved.values.all { "delegate_permission/common.get_login_creds" in it },
    )
}
