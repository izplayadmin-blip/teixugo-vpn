package com.teixugovpn.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teixugovpn.app.auth.AuthRepository
import com.teixugovpn.app.auth.AuthResult
import com.teixugovpn.app.ui.theme.TeixugoGold
import com.teixugovpn.app.ui.theme.TeixugoSurface
import com.teixugovpn.app.ui.theme.TeixugoTextMuted
import kotlinx.coroutines.launch

private enum class OnboardingStep { CHOOSE, BACKUP, RESTORE }

@Composable
fun OnboardingScreen(
    authRepository: AuthRepository,
    onLoggedIn: () -> Unit,
) {
    var step by remember { mutableStateOf(OnboardingStep.CHOOSE) }
    var generatedKey by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var confirmedBackup by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doLogin() {
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val result = authRepository.login()) {
                is AuthResult.Success -> onLoggedIn()
                is AuthResult.Failure -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF060606), Color(0xFF161617), Color(0xFF080808))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(34.dp))

                when (step) {
                    OnboardingStep.CHOOSE -> ChooseIdentity(
                        onCreate = {
                            generatedKey = authRepository.createNewIdentity()
                            step = OnboardingStep.BACKUP
                        },
                        onRestore = { step = OnboardingStep.RESTORE },
                    )

                    OnboardingStep.BACKUP -> BackupIdentity(
                        generatedKey = generatedKey,
                        confirmedBackup = confirmedBackup,
                        isLoading = isLoading,
                        onConfirmedChange = { confirmedBackup = it },
                        onContinue = { doLogin() },
                    )

                    OnboardingStep.RESTORE -> RestoreIdentity(
                        restoreInput = restoreInput,
                        isLoading = isLoading,
                        onRestoreInput = { restoreInput = it },
                        onSubmit = {
                            val ok = authRepository.restoreIdentity(restoreInput)
                            if (!ok) {
                                errorMessage = "Chave invalida. Confira os 64 caracteres."
                            } else {
                                doLogin()
                            }
                        },
                    )
                }

                errorMessage?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChooseIdentity(onCreate: () -> Unit, onRestore: () -> Unit) {
    BrandHero()
    Spacer(Modifier.height(34.dp))
    FeatureCard(title = "Privacidade Total", body = "Sem e-mail, senha ou dados pessoais.")
    Spacer(Modifier.height(10.dp))
    FeatureCard(title = "Seguranca Avancada", body = "Sua conta nasce como uma chave local neste aparelho.")
    Spacer(Modifier.height(28.dp))
    PrimaryButton(text = "Criar conta", onClick = onCreate)
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onRestore,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TeixugoGold.copy(alpha = 0.65f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TeixugoGold),
    ) {
        Text("Ja tenho uma chave", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BackupIdentity(
    generatedKey: String,
    confirmedBackup: Boolean,
    isLoading: Boolean,
    onConfirmedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    ScreenTitle("Guarde sua chave", "Ela e a unica forma de acessar sua conta depois.")
    Spacer(Modifier.height(20.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        SelectionContainer {
            Text(
                generatedKey,
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Text(
        "Copie e guarde em um lugar seguro. Qualquer pessoa com essa chave acessa sua conta.",
        color = TeixugoTextMuted,
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = confirmedBackup,
            onCheckedChange = onConfirmedChange,
            colors = CheckboxDefaults.colors(checkedColor = TeixugoGold),
        )
        Text("Eu guardei minha chave.", color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(20.dp))
    PrimaryButton(
        text = if (isLoading) "Entrando..." else "Continuar",
        enabled = confirmedBackup && !isLoading,
        onClick = onContinue,
    )
}

@Composable
private fun RestoreIdentity(
    restoreInput: String,
    isLoading: Boolean,
    onRestoreInput: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    ScreenTitle("Entrar com sua chave", "Cole sua chave Teixugo para restaurar o acesso.")
    Spacer(Modifier.height(22.dp))
    OutlinedTextField(
        value = restoreInput,
        onValueChange = onRestoreInput,
        label = { Text("Chave de 64 caracteres") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TeixugoGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
            focusedLabelColor = TeixugoGold,
            unfocusedLabelColor = TeixugoTextMuted,
            cursorColor = TeixugoGold,
        ),
    )
    Spacer(Modifier.height(18.dp))
    PrimaryButton(
        text = if (isLoading) "Entrando..." else "Entrar",
        enabled = !isLoading,
        onClick = onSubmit,
    )
}

@Composable
private fun BrandHero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(168.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x44F5B725), Color.Transparent)),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 28.dp.toPx()),
                )
            }
            TeixugoShield(modifier = Modifier.size(118.dp))
        }
        Spacer(Modifier.height(18.dp))
        Row {
            Text("TEIXUGO ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 36.sp)
            Text("VPN", color = TeixugoGold, fontWeight = FontWeight.Black, fontSize = 36.sp)
        }
        Text(
            "Privacidade. Seguranca. Liberdade.",
            color = TeixugoTextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TeixugoShield(modifier = Modifier.size(86.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TeixugoTextMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FeatureCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TeixugoSurface.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("+", color = TeixugoGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(body, color = TeixugoTextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TeixugoGold,
            contentColor = Color.Black,
            disabledContainerColor = TeixugoGold.copy(alpha = 0.35f),
            disabledContentColor = Color.Black.copy(alpha = 0.55f),
        ),
    ) {
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TeixugoShield(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.teixugovpn.app.R.drawable.teixugo_logo_mark),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
