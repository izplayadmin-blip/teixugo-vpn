package com.teixugovpn.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teixugovpn.app.network.ApiClient
import com.teixugovpn.app.network.dto.AccountInfo
import com.teixugovpn.app.network.dto.CreatePeerRequest
import com.teixugovpn.app.ui.theme.TeixugoGold
import com.teixugovpn.app.ui.theme.TeixugoGreen
import com.teixugovpn.app.ui.theme.TeixugoRed
import com.teixugovpn.app.ui.theme.TeixugoSurface
import com.teixugovpn.app.ui.theme.TeixugoTextMuted
import com.teixugovpn.app.vpn.WireGuardManager
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.launch

private enum class HomeTab { HOME, SERVERS, SETTINGS }

@Composable
fun HomeScreen(
    wireGuardManager: WireGuardManager,
    onLogout: () -> Unit,
) {
    var account by remember { mutableStateOf<AccountInfo?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(HomeTab.HOME) }
    var selectedServer by remember { mutableStateOf(vpnServers.first()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val res = ApiClient.service.me()
        account = res.body()?.account
    }

    fun toggleConnection() {
        isBusy = true
        statusMessage = null
        scope.launch {
            if (isConnected) {
                wireGuardManager.disconnect()
                isConnected = false
            } else {
                val res = ApiClient.service.createPeer(
                    CreatePeerRequest(device_name = "Android", server_id = selectedServer.id)
                )
                val body = res.body()

                if (body?.success != true || body.config == null) {
                    statusMessage = body?.error ?: "Nao foi possivel conectar. Verifique sua assinatura/trial."
                    isBusy = false
                    return@launch
                }

                val result = wireGuardManager.connect("teixugo", body.config) { state ->
                    isConnected = state == Tunnel.State.UP
                }

                if (result.isFailure) {
                    statusMessage = "Erro ao ativar tunel: ${result.exceptionOrNull()?.message}"
                } else {
                    isConnected = true
                }
            }
            isBusy = false
        }
    }

    val trialUsedMb = (account?.trial_bytes_used ?: 0) / 1_048_576.0
    val trialCapMb = (account?.trial_bytes_cap ?: 1) / 1_048_576.0
    val progress = (trialUsedMb / trialCapMb).toFloat().coerceIn(0f, 1f)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF080808), Color(0xFF151516), Color(0xFF0B0B0C))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TopBar(account = account, onLogout = onLogout)
                Spacer(Modifier.height(20.dp))

                when (selectedTab) {
                    HomeTab.HOME -> {
                        StatusBanner(isConnected = isConnected)
                        Spacer(Modifier.height(26.dp))

                        ConnectDial(
                            isConnected = isConnected,
                            isBusy = isBusy,
                            onClick = { toggleConnection() },
                        )

                        Spacer(Modifier.height(26.dp))

                        ServerCard(server = selectedServer, isConnected = isConnected)
                        Spacer(Modifier.height(12.dp))

                        UsageCard(
                            usedMb = trialUsedMb.toInt(),
                            capMb = trialCapMb.toInt(),
                            progress = progress,
                        )

                        statusMessage?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    HomeTab.SERVERS -> ServersView(
                        selectedServerId = selectedServer.id,
                        onServerSelected = { selectedServer = it },
                    )
                    HomeTab.SETTINGS -> SettingsView(account = account)
                }

                Spacer(Modifier.height(92.dp))
            }

            BottomNav(
                modifier = Modifier.align(Alignment.BottomCenter),
                isConnected = isConnected,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
    }
}

@Composable
private fun TopBar(account: AccountInfo?, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Row {
                    Text("TEIXUGO ", fontWeight = FontWeight.Black, color = Color.White)
                    Text("VPN", fontWeight = FontWeight.Black, color = TeixugoGold)
                }
                Text(
                    "Conta ${account?.pubkey_short ?: "..."}",
                    color = TeixugoTextMuted,
                    fontSize = 12.sp,
                )
            }
        }
        TextButton(onClick = onLogout) {
            Text("Sair", color = TeixugoGold, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusBanner(isConnected: Boolean) {
    val color = if (isConnected) TeixugoGreen else TeixugoRed
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    if (isConnected) "Protegido" else "Desconectado",
                    color = color,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (isConnected) "Sua conexao esta segura" else "Voce nao esta protegido",
                    color = TeixugoTextMuted,
                    fontSize = 12.sp,
                )
            }
            BrandMark(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ConnectDial(
    isConnected: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(214.dp)
            .clip(CircleShape)
            .clickable(enabled = !isBusy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF141414))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x44F5B725), Color.Transparent),
                    radius = size.minDimension * 0.56f,
                )
            )
            drawArc(
                color = TeixugoGold,
                startAngle = -90f,
                sweepAngle = if (isConnected) 360f else 318f,
                useCenter = false,
                topLeft = Offset(13.dp.toPx(), 13.dp.toPx()),
                size = Size(size.width - 26.dp.toPx(), size.height - 26.dp.toPx()),
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(Color(0xFF0C0C0D), radius = size.minDimension * 0.34f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(modifier = Modifier.size(82.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    isBusy -> "..."
                    isConnected -> "00:05:48"
                    else -> "Conectar"
                },
                color = if (isConnected) Color.White else TeixugoGold,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
            Text(
                if (isConnected) "Conectado" else "Toque para iniciar",
                color = TeixugoTextMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ServerCard(server: ServerRow, isConnected: Boolean) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.flag, color = TeixugoGold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(server.country, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(server.city, color = TeixugoTextMuted, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (isConnected) server.ping else "Selecionado", color = TeixugoGold, fontSize = 12.sp)
                Text("||||", color = TeixugoGreen, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ServersView(
    selectedServerId: Int,
    onServerSelected: (ServerRow) -> Unit,
) {
    SectionTitle(title = "Servidores", subtitle = "Escolha um pais ou use a conexao mais rapida.")
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip("Todos", selected = true, modifier = Modifier.weight(1f))
        FilterChip("Mais rapido", selected = false, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    vpnServers.forEach {
        ServerListItem(
            row = it,
            selected = it.id == selectedServerId,
            onClick = { onServerSelected(it) },
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SettingsView(account: AccountInfo?) {
    var autoConnect by remember { mutableStateOf(true) }
    var killSwitch by remember { mutableStateOf(true) }

    SectionTitle(title = "Configuracoes", subtitle = "Preferencias de conexao, seguranca e suporte.")
    Spacer(Modifier.height(16.dp))
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(modifier = Modifier.size(58.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Conta Premium", color = TeixugoGold, fontWeight = FontWeight.Black)
                Text("Plano ativo", color = Color.White, fontSize = 13.sp)
                Text(account?.pubkey_short ?: "Conta Teixugo", color = TeixugoTextMuted, fontSize = 12.sp)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    SettingRow("Protocolo", "WireGuard")
    SettingSwitch("Auto conectar", "Conectar ao iniciar o app", autoConnect) { autoConnect = it }
    SettingSwitch("Kill Switch", "Bloquear trafego sem VPN", killSwitch) { killSwitch = it }
    SettingRow("Tema do app", "Escuro")
    Spacer(Modifier.height(12.dp))
    SettingRow("Servidores favoritos", ">")
    SettingRow("Conexoes recentes", ">")
    SettingRow("Suporte", ">")
    SettingRow("Versao do app", "1.0.0")
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = TeixugoTextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = CircleShape,
        color = if (selected) TeixugoGold else TeixugoSurface,
        border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (selected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}

private data class ServerRow(
    val id: Int,
    val flag: String,
    val country: String,
    val city: String,
    val ping: String,
    val endpoint: String,
    val statusUrl: String,
)

private val vpnServers = listOf(
    ServerRow(1, "BR", "Brasil", "Sao Paulo", "20 ms", "209.14.84.61:51820", "http://209.14.84.61:8088/status"),
    ServerRow(2, "US", "Estados Unidos", "Nova York", "32 ms", "208.122.17.54:51820", "http://208.122.17.54:8088/status"),
)

@Composable
private fun ServerListItem(row: ServerRow, selected: Boolean, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.flag, color = TeixugoGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(row.country, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(row.city, color = TeixugoTextMuted, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.ping, color = if (selected) TeixugoGold else TeixugoTextMuted, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Text(if (selected) "*" else "+", color = TeixugoGold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String) {
    AppCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(value, color = TeixugoTextMuted, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingSwitch(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    AppCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(body, color = TeixugoTextMuted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = TeixugoGold,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF333333),
                ),
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun UsageCard(usedMb: Int, capMb: Int, progress: Float) {
    AppCard {
        Text("Periodo de teste", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = TeixugoGold,
            trackColor = Color(0xFF333333),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$usedMb MB usados", color = TeixugoTextMuted, fontSize = 12.sp)
            Text("$capMb MB", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TeixugoSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun BottomNav(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0xEE111112),
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = selectedTab == HomeTab.HOME,
            onClick = { onTabSelected(HomeTab.HOME) },
            icon = { Text("H", color = if (selectedTab == HomeTab.HOME) TeixugoGold else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black) },
            label = { Text("Inicio", color = if (selectedTab == HomeTab.HOME) TeixugoGold else TeixugoTextMuted, fontSize = 11.sp) },
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.SERVERS,
            onClick = { onTabSelected(HomeTab.SERVERS) },
            icon = { Text("S", color = if (selectedTab == HomeTab.SERVERS) TeixugoGold else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black) },
            label = { Text("Servidores", color = if (selectedTab == HomeTab.SERVERS) TeixugoGold else TeixugoTextMuted, fontSize = 11.sp) },
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.SETTINGS,
            onClick = { onTabSelected(HomeTab.SETTINGS) },
            icon = { Text(if (isConnected) "OK" else "A", color = if (selectedTab == HomeTab.SETTINGS) TeixugoGold else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black) },
            label = { Text("Ajustes", color = if (selectedTab == HomeTab.SETTINGS) TeixugoGold else TeixugoTextMuted, fontSize = 11.sp) },
        )
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.teixugovpn.app.R.drawable.teixugo_logo_mark),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
