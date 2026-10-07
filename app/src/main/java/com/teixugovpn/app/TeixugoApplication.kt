package com.teixugovpn.app

import android.app.Application
import com.teixugovpn.app.network.ApiClient

class TeixugoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
