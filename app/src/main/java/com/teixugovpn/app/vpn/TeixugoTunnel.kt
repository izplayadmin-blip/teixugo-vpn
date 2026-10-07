package com.teixugovpn.app.vpn

import com.wireguard.android.backend.Tunnel

/**
 * A lib com.wireguard.android exige um objeto que implemente `Tunnel` pra
 * cada túnel que você for controlar. Como o app só tem UM túnel ativo por
 * vez (um dispositivo = uma conexão), isso pode ser bem simples.
 */
class TeixugoTunnel(
    private val tunnelName: String,
    private var onStateChanged: (Tunnel.State) -> Unit = {},
) : Tunnel {

    override fun getName(): String = tunnelName

    override fun onStateChange(newState: Tunnel.State) {
        onStateChanged(newState)
    }

    fun setStateListener(listener: (Tunnel.State) -> Unit) {
        onStateChanged = listener
    }
}
