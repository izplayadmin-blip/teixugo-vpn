# Teixugo VPN — App Android

Cliente Android nativo (Kotlin + Jetpack Compose) que fala com a mesma API
PHP do site: mesma autenticação por chave Nostr, mesmo backend de planos/
trial, e usa a biblioteca oficial do WireGuard pra estabelecer o túnel de
verdade no aparelho.

## ⚠️ Limitação importante deste entrega

**Eu não consegui compilar nem rodar este projeto.** Este ambiente não tem
Android SDK nem acesso aos repositórios do Google/Maven Central (só
consigo baixar de npm, pypi, crates.io e GitHub). Isso significa:

- O código foi escrito com cuidado e segue os padrões corretos de cada
  biblioteca, mas **nunca passou por um `./gradlew build` de verdade**.
- Os pontos de maior risco de erro de compilação estão comentados no
  código com `ATENÇÃO` (principalmente em `NostrCrypto.kt`, onde uso a
  biblioteca `secp256k1-kmp` — não consegui confirmar 100% os nomes exatos
  de método da versão mais recente).
- A parte de permissão de VPN do Android (`MainActivity.kt`,
  `ensureVpnPermission()`) também não foi testada num aparelho/emulador
  de verdade.

**Fluxo recomendado:** abra este projeto no Android Studio, tente compilar,
e me mande o erro exato (a mensagem completa do compilador). Eu corrijo na
hora — é bem mais rápido eu ajustar um erro de compilação específico do
que tentar adivinhar tudo sem poder testar.

## O que já está implementado

- **Identidade Nostr** (`nostr/NostrCrypto.kt`, `nostr/SecureKeyStorage.kt`):
  gera a chave no dispositivo, guarda criptografada (Android Keystore via
  `EncryptedSharedPreferences`), assina o desafio de login.
- **Autenticação** (`auth/AuthRepository.kt`): mesmo fluxo challenge→sign→
  verify do site, com sessão via cookie persistente (`network/ApiClient.kt`).
- **WireGuard** (`vpn/WireGuardManager.kt`, `vpn/TeixugoTunnel.kt`): sobe/
  derruba o túnel a partir do `.conf` que a API devolve.
- **Telas** (`ui/screens/`): onboarding (criar/restaurar chave, com aviso
  de backup) e tela principal (conectar/desconectar, status do trial).

## O que falta / próximos passos

1. Tela de gerenciamento de múltiplos dispositivos (a API já suporta,
   `revoke-peer.php`, só falta a tela)
2. Tela de assinatura/pagamento (por ora, mais simples abrir o navegador
   pro checkout cripto do site do que reimplementar isso nativamente)
3. Ícone de verdade (troquei por um placeholder simples em
   `res/drawable/ic_launcher_foreground.xml` — veja o comentário lá)
4. Testar de verdade a permissão de VPN e o túnel em um aparelho físico
   (emulador Android geralmente não suporta VPN direito)
5. Assinatura do APK e publicação (Play Store tem regras específicas pra
   apps de VPN — vale revisar a política deles antes de submeter)

## Como compilar

1. Abra a pasta `teixugo-android/` no Android Studio (versão recente,
   Hedgehog ou mais nova)
2. Deixe o Gradle sincronizar (ele vai baixar as dependências listadas em
   `app/build.gradle.kts`)
3. Troque a URL em `network/ApiClient.kt` (`BASE_URL`) pelo domínio real
   se ainda não for `teixugovpn.com`
4. Rode num aparelho físico (não emulador) pra testar a VPN de verdade

## Estrutura

```
app/src/main/java/com/teixugovpn/app/
├── MainActivity.kt              # navegação + pedido de permissão VPN
├── TeixugoApplication.kt        # inicializa o cliente de rede
├── nostr/
│   ├── NostrCrypto.kt           # gerar chave, assinar eventos
│   └── SecureKeyStorage.kt      # guarda a chave privada criptografada
├── auth/
│   └── AuthRepository.kt        # fluxo de login (challenge/sign/verify)
├── network/
│   ├── ApiService.kt            # endpoints (Retrofit)
│   ├── ApiClient.kt             # http client + cookies persistentes
│   └── dto/Models.kt            # objetos de request/response
├── vpn/
│   ├── WireGuardManager.kt      # conecta/desconecta o túnel
│   └── TeixugoTunnel.kt         # implementação exigida pela lib WireGuard
└── ui/
    ├── screens/
    │   ├── OnboardingScreen.kt  # criar conta / restaurar chave
    │   └── HomeScreen.kt        # conectar + status do trial
    └── theme/Theme.kt           # cores da marca (preto/dourado)
```
