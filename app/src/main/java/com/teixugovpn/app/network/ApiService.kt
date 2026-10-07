package com.teixugovpn.app.network

import com.teixugovpn.app.network.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/challenge.php")
    suspend fun getChallenge(): Response<ChallengeResponse>

    @POST("api/auth/verify.php")
    suspend fun verifyEvent(@Body body: VerifyRequest): Response<VerifyResponse>

    @GET("api/me.php")
    suspend fun me(): Response<MeResponse>

    @POST("api/logout.php")
    suspend fun logout(): Response<SimpleResponse>

    @GET("api/plans.php")
    suspend fun getPlans(): Response<PlansResponse>

    @POST("api/wireguard/create-peer.php")
    suspend fun createPeer(@Body body: CreatePeerRequest): Response<CreatePeerResponse>

    @POST("api/wireguard/revoke-peer.php")
    suspend fun revokePeer(@Body body: RevokePeerRequest): Response<SimpleResponse>
}
