package com.teixugovpn.app.network.dto

data class ChallengeResponse(
    val success: Boolean,
    val nonce: String?,
    val error: String?,
)

data class NostrEventDto(
    val id: String,
    val pubkey: String,
    val created_at: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String,
)

data class VerifyRequest(
    val event: NostrEventDto,
)

data class VerifyResponse(
    val success: Boolean,
    val pubkey: String?,
    val error: String?,
)

data class AccountInfo(
    val pubkey: String,
    val pubkey_short: String,
    val trial_bytes_used: Long,
    val trial_bytes_cap: Long,
)

data class MeResponse(
    val success: Boolean,
    val logged_in: Boolean,
    val account: AccountInfo?,
)

data class Plan(
    val id: Int,
    val name: String,
    val slug: String,
    val duration_days: Int,
    val max_devices: Int,
    val price_brl_formatted: String,
    val tagline: String?,
    val features: List<String>,
    val is_featured: Boolean,
)

data class PlansResponse(
    val success: Boolean,
    val plans: List<Plan>,
)

data class CreatePeerRequest(
    val device_name: String,
    val server_id: Int? = null,
)

data class ServerInfo(
    val name: String,
    val country_code: String,
)

data class CreatePeerResponse(
    val success: Boolean,
    val peer_id: Int?,
    val config: String?,
    val server: ServerInfo?,
    val is_trial: Boolean?,
    val error: String?,
)

data class RevokePeerRequest(
    val peer_id: Int,
)

data class SimpleResponse(
    val success: Boolean,
    val message: String?,
    val error: String?,
)
