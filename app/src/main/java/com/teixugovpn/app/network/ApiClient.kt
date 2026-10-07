package com.teixugovpn.app.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * A autenticação do backend funciona por sessão de cookie normal do PHP
 * (igual um site comum) -- não por token Bearer. Por isso precisamos de um
 * CookieJar que persista entre chamadas E entre reinicializações do app.
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("teixugo_cookies", Context.MODE_PRIVATE)
    private val cache = mutableMapOf<String, MutableList<Cookie>>()

    init {
        prefs.all.forEach { (host, raw) ->
            if (raw is String) {
                val cookie = Cookie.parse(HttpUrl.Builder().scheme("https").host(host).build(), raw)
                if (cookie != null) {
                    cache.getOrPut(host) { mutableListOf() }.add(cookie)
                }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cache[host] = cookies.toMutableList()
        val editor = prefs.edit()
        cookies.forEach { editor.putString(host, it.toString()) }
        editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cache[url.host] ?: emptyList()
    }

    fun clear() {
        cache.clear()
        prefs.edit().clear().apply()
    }
}

object ApiClient {
    // Painel Teixugo VPN na VPS de gerenciamento.
    private const val BASE_URL = "http://184.154.245.41/"

    private var retrofit: Retrofit? = null
    private var cookieJar: PersistentCookieJar? = null

    fun init(context: Context) {
        if (retrofit != null) return

        cookieJar = PersistentCookieJar(context.applicationContext)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar!!)
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("ApiClient.init(context) precisa ser chamado antes de usar.")

    fun clearSession() {
        cookieJar?.clear()
    }
}
