# Footscore

App Android minimalista (Kotlin + Jetpack Compose) para notificação diária com os
resultados dos seus times de futebol favoritos.

## Arquitetura

```
┌──────────────┐   registra token FCM +    ┌──────────────────┐   consulta jogos   ┌────────────────┐
│  App Android │   favoritos/horário       │  Backend (Railway) │ ─────────────────▶ │  Sofascore      │
│  (Compose)   │ ─────────────────────────▶│  Node.js + Express │ ◀───────────────── │  (API pública)  │
└──────────────┘                           └────────┬──────────┘                    └────────────────┘
       ▲                                             │ push via FCM
       └─────────────────────────────────────────────┘  no horário configurado
```

O app **não fala direto com nenhuma API de futebol**: ele registra no backend
(hospedado no Railway) o token do Firebase Cloud Messaging, os times favoritos e
o horário desejado. Um cron dentro do backend roda a cada minuto, confere se é o
horário de algum assinante (no fuso horário dele) e, se sim, busca os jogos de
hoje/ontem, filtra pelos times favoritos e dispara uma notificação push. Isso
tem vantagens sobre rodar tudo no celular com WorkManager:

1. Não depende do Android não matar a tarefa em segundo plano (Doze,
   otimização de bateria, fabricantes agressivos como Xiaomi/Samsung) — quem
   decide notificar é o servidor, que empurra via push.
2. Se a fonte de dados de futebol mudar de novo no futuro, é só trocar
   `backend/src/footballApi.js` — o app Android não muda.

### ⚠️ Fonte de dados: API pública (não-oficial) do Sofascore

Tentei usar a API-Football (RapidAPI) primeiro, mas a listagem sumiu do
marketplace ("API not found"). A alternativa implementada é a **API pública que
o próprio site/app do Sofascore usa** (`api.sofascore.com`) — não-oficial, sem
chave, gratuita. Mapeei os endpoints e nomes de campos cruzando documentação de
projetos open-source (não RapidAPI/API-Football), mas **não consegui testar uma
chamada real** a partir do ambiente onde montei isso, porque a rede de lá
bloqueava esse domínio por política própria. Duas coisas para ficar de olho:

- Rode este teste no seu computador (rede doméstica normal) antes de configurar
  Firebase/Railway, só para confirmar que os endpoints estão certos:
  ```bash
  curl -s "https://api.sofascore.com/api/v1/unique-tournament/17/seasons" \
    -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36" \
    -H "Accept: application/json"
  ```
  Deve voltar um JSON com `"seasons": [...]`.
- Por ser não-oficial, existe o risco de a Sofascore bloquear IPs de datacenter
  (Cloudflare costuma fazer isso com provedores como Railway/AWS/GCP) mesmo com
  headers de navegador. Isso só dá pra confirmar depois do deploy: teste
  `https://SEU-BACKEND.up.railway.app/teams?tournamentId=17` — se voltar um erro
  502 mencionando "Sofascore respondeu 403", é bloqueio de IP, e a solução seria
  trocar de fonte de dados novamente (ex.: voltar para uma API paga que aceite
  tráfego de servidor, como a Football-Data.org ou outra do RapidAPI).

## O que o app faz

1. Você escolhe seus times favoritos navegando por liga (Brasileirão, Premier League,
   La Liga, Champions League, Serie A italiana, Bundesliga, Ligue 1).
2. Você escolhe um horário diário para receber a notificação.
3. O app registra tudo isso no backend (token do FCM + favoritos + horário + fuso).
4. Todo dia, nesse horário, o **backend** verifica se algum time favorito jogou
   (hoje ou ontem) e, se sim, envia um push com os placares.
5. Se ninguém jogou, nenhuma notificação é enviada — a menos que você ative
   "Avisar mesmo quando não houver jogos".

## Tema

O app segue **Material You**: em Android 12+ ele usa as cores dinâmicas extraídas
do papel de parede do sistema (`dynamicLightColorScheme`/`dynamicDarkColorScheme`
em `ui/theme/Theme.kt`); em versões mais antigas cai para uma paleta verde fixa.
Os modos **claro e escuro** são automáticos, seguindo o tema do sistema
(`isSystemInDarkTheme()`), incluindo o tema base da Activity
(`values/themes.xml` e `values-night/themes.xml`) para não haver flash de tela
branca em dispositivos no modo escuro antes do Compose carregar.

## Stack

**App**
- Kotlin + Jetpack Compose + Material 3
- MVVM (ViewModel + StateFlow, `UserPreferencesRepository` como fonte única de verdade)
- DataStore (Preferences) para salvar times favoritos e horário
- Retrofit 2 + Kotlinx Serialization para falar com o backend
- Firebase Cloud Messaging para receber os pushes

**Backend** (`backend/`)
- Node.js + Express
- `node-cron` para o laço de verificação por minuto
- `firebase-admin` para enviar os pushes
- Armazenamento simples em arquivo JSON (`backend/data/subscribers.json`)

## 1. Criar o projeto Firebase (para o push funcionar)

1. Acesse https://console.firebase.google.com e crie um projeto novo (gratuito).
2. Dentro do projeto, adicione um app Android com o pacote `com.footscore.app`.
3. Baixe o arquivo `google-services.json` gerado e coloque em `app/google-services.json`
   (esse arquivo é ignorado pelo git — cada pessoa usa o seu próprio projeto Firebase).
4. Ainda no Firebase Console, vá em **Configurações do projeto → Contas de serviço**
   e clique em "Gerar nova chave privada". Isso baixa um JSON — guarde-o, você vai
   precisar dele no passo 2 (deploy do backend).

## 2. Publicar o backend no Railway

Não precisa de nenhuma conta/chave de API de futebol — só Firebase.

1. No [Railway](https://railway.app), crie um novo projeto a partir deste
   repositório GitHub, apontando a **raiz do serviço para a pasta `backend/`**
   (em Settings → Root Directory).
2. Em Variables, defina:
   - `APP_SHARED_SECRET`: qualquer string longa e aleatória, inventada por você
     (ex.: gere com `openssl rand -hex 32`). É o "cadeado" que impede qualquer
     pessoa de usar seu backend.
   - `FIREBASE_SERVICE_ACCOUNT_BASE64`: o JSON da conta de serviço do passo 1.4,
     codificado em base64 em uma linha só:
     ```bash
     base64 -i caminho/para/service-account.json | tr -d '\n'
     ```
3. Faça o deploy (o Railway detecta Node automaticamente via `package.json` e
   roda `npm start`).
4. Em Settings → Networking, gere um domínio público. Você vai usar essa URL no app.
5. Teste `https://SEU-DOMINIO.up.railway.app/teams?tournamentId=17` com o header
   `X-App-Secret: <o mesmo valor do passo 2>` — veja o aviso sobre bloqueio de IP
   acima se isso falhar.
6. (Opcional) Para o registro de dispositivos sobreviver a redeploys, adicione um
   Volume do Railway montado na pasta `data/` do serviço. Sem isso, um redeploy
   apaga `subscribers.json` — mas é inofensivo: basta abrir o app de novo, que
   ele reenvia o registro automaticamente.

Para testar localmente antes de publicar:

```bash
cd backend
cp .env.example .env   # preencha as duas variáveis
npm install
npm start
```

## 3. Configurar o app

Copie o exemplo e preencha com a URL do Railway (passo 2.4) e o mesmo segredo
do passo 2.2:

```bash
cp local.properties.example local.properties
```

```properties
sdk.dir=/caminho/para/o/Android/sdk   # o Android Studio preenche sozinho
backend.baseUrl=https://seu-app.up.railway.app/   # termine com "/"
backend.secret=o_mesmo_valor_de_APP_SHARED_SECRET_no_Railway
```

Confirme também que `app/google-services.json` está no lugar (passo 1.3).

## 4. Rodar no Android Studio

1. Abra a pasta do projeto no Android Studio (Giraffe/Koala ou mais recente).
2. Deixe o Gradle sincronizar (ele baixa o wrapper do Gradle 8.6 automaticamente
   na primeira sincronização).
3. Rode o app (`Run > Run 'app'`) em um emulador **com Google Play Services**
   (necessário para o FCM) ou um celular real com Android 8.0+ (API 26+).
4. Na primeira abertura, aceite a permissão de notificações (Android 13+).
5. Escolha uma liga, toque nos times para favoritar, defina o horário. Cada
   mudança é enviada automaticamente ao backend.
6. Toque em **"Testar notificação agora"** para validar o fluxo ponta a ponta
   (app → backend → FCM → notificação) sem esperar o horário agendado.

> Se o `gradlew` reclamar de wrapper ausente, abra o projeto direto no Android
> Studio — ele detecta e recria o wrapper automaticamente — ou rode
> `gradle wrapper --gradle-version 8.6` uma vez com um Gradle instalado localmente.

## Estrutura do código

```
app/src/main/java/com/footscore/app/
├── data/
│   ├── model/            League.kt (IDs de torneio do Sofascore), FavoriteTeam.kt
│   ├── local/             UserPreferences.kt, UserPreferencesRepository.kt (DataStore)
│   ├── remote/            BackendApiConfig.kt, BackendApiService.kt, dto/*.kt
│   └── repository/        FootballRepository.kt, DeviceRegistrationRepository.kt
├── notification/           NotificationHelper.kt, FootscoreMessagingService.kt (FCM)
├── ui/
│   ├── theme/              Color.kt, Type.kt, Theme.kt
│   └── home/                HomeViewModel.kt, HomeScreen.kt, components/TimePickerDialog.kt
└── MainActivity.kt

backend/src/
├── index.js        Ponto de entrada (Express + start do cron)
├── config.js        Leitura das variáveis de ambiente
├── routes.js         /register, /teams, /test-notify
├── footballApi.js    Cliente da API pública do Sofascore (com cache por data/temporada)
├── notifier.js        Laço de verificação por minuto (node-cron)
├── firebase.js         Envio de push via firebase-admin
└── store.js            Persistência simples em JSON dos assinantes
```

### IDs de torneio (liga)

Os IDs usados em `data/model/League.kt` são os "uniqueTournament" do Sofascore —
o número no final da URL de cada torneio em sofascore.com (ex.:
`sofascore.com/football/tournament/england/premier-league/17` → `17`).
Confirmados via busca (Brasileirão = 325, Premier League = 17, La Liga = 8,
Champions League = 7, Serie A itaiana = 23, Bundesliga = 35, Ligue 1 = 34).
Caso algum pare de funcionar, é só achar o torneio em sofascore.com e copiar o
número da URL.

## Licença

Uso pessoal.
