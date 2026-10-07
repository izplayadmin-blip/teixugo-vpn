package com.teixugovpn.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.teixugovpn.app.auth.AuthRepository
import com.teixugovpn.app.nostr.SecureKeyStorage
import com.teixugovpn.app.ui.screens.HomeScreen
import com.teixugovpn.app.ui.screens.OnboardingScreen
import com.teixugovpn.app.ui.theme.TeixugoVpnTheme
import com.teixugovpn.app.vpn.WireGuardManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var wireGuardManager: WireGuardManager

    /**
     * O Android exige uma autorização explícita do usuário antes de
     * qualquer app criar uma VPN (diálogo padrão do sistema, "Este app
     * quer configurar uma conexão VPN"). Isso só aparece na primeira vez.
     *
     * NÃO TESTADO NESTE AMBIENTE: confirme que esse launcher realmente
     * dispara o diálogo do sistema corretamente ao testar num aparelho/emulador.
     */
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* resultado tratado implicitamente -- o backend já tenta subir o túnel em seguida */ }

    private fun ensureVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyStorage = SecureKeyStorage(this)
        authRepository = AuthRepository(keyStorage)
        wireGuardManager = WireGuardManager(this)

        // Pede a permissão de VPN cedo (na abertura do app), pra não
        // interromper o fluxo bem no meio do primeiro "Conectar".
        ensureVpnPermission()

        setContent {
            TeixugoVpnTheme {
                var loggedIn by remember { mutableStateOf(false) }
                var checkingSession by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    // Se já existe chave local, tenta logar automaticamente
                    if (authRepository.hasLocalIdentity()) {
                        scope.launch {
                            val result = authRepository.login()
                            loggedIn = result is com.teixugovpn.app.auth.AuthResult.Success
                            checkingSession = false
                        }
                    } else {
                        checkingSession = false
                    }
                }

                if (!checkingSession) {
                    if (loggedIn) {
                        HomeScreen(
                            wireGuardManager = wireGuardManager,
                            onLogout = {
                                scope.launch {
                                    authRepository.logout()
                                    loggedIn = false
                                }
                            },
                        )
                    } else {
                        OnboardingScreen(
                            authRepository = authRepository,
                            onLoggedIn = { loggedIn = true },
                        )
                    }
                }
            }
        }
    }
}
