package com.teixugovpn.app.vpn

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * Ponte entre o app e a biblioteca oficial do WireGuard.
 *
 * Fluxo de uso:
 *   1. App chama a API (create-peer.php) e recebe o texto de um .conf
 *   2. connect(confText) faz o parse desse texto e sobe o túnel
 *   3. disconnect() derruba
 *
 * IMPORTANTE (não testado): a lib com.wireguard.android exige que a
 * Activity/Service chamadora já tenha pedido a permissão de VPN do Android
 * via `VpnService.prepare()` ANTES de chamar connect() -- isso normalmente
 * é feito uma vez, na primeira conexão, mostrando o diálogo padrão do
 * sistema ("Este app quer criar uma conexão VPN"). Eu deixei um comentário
 * no MainActivity mostrando onde isso entra; confirme esse fluxo ao testar,
 * pois eu não consegui rodar isso de verdade neste ambiente.
 */
class WireGuardManager(context: Context) {

    private val backend: Backend = GoBackend(context.applicationContext)
    private var currentTunnel: TeixugoTunnel? = null

    suspend fun connect(
        deviceLabel: String,
        confText: String,
        onStateChanged: (Tunnel.State) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = Config.parse(ByteArrayInputStream(confText.toByteArray(Charsets.UTF_8)))
            val tunnel = TeixugoTunnel(deviceLabel, onStateChanged)
            backend.setState(tunnel, Tunnel.State.UP, config)
            currentTunnel = tunnel
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tunnel = currentTunnel ?: return@withContext Result.success(Unit)
            backend.setState(tunnel, Tunnel.State.DOWN, null)
            currentTunnel = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean {
        val tunnel = currentTunnel ?: return false
        return backend.getState(tunnel) == Tunnel.State.UP
    }

    /** Estatísticas de tráfego do túnel ativo (bytes enviados/recebidos), se disponível. */
    fun getStatistics() = currentTunnel?.let { backend.getStatistics(it) }
}
