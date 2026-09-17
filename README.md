# Footscore

App Android minimalista (Kotlin + Jetpack Compose) para notificação diária com os
resultados dos seus times de futebol favoritos.

## Arquitetura

```
┌──────────────┐   registra token FCM +    ┌──────────────────┐   consulta jogos   ┌────────────────┐
│  App Android │   favoritos/horário       │  Backend (Railway) │ ─────────────────▶ │  API-Football   │
│  (Compose)   │ ─────────────────────────▶│  Node.js + Express │ ◀───────────────── │  (RapidAPI)     │
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
2. A chave da API-Football nunca fica no APK — só no backend.
3. Se a fonte de dados de futebol mudar de novo no futuro, é só trocar
   `backend/src/footballApi.js` — o app Android não muda (ele só sabe falar
   com o próprio backend).

### Sobre a escolha da fonte de dados

Testei três alternativas antes de fechar nessa:

- **Sofascore** (API pública não-oficial): cobre tudo, mas testes reais
  mostraram bloqueio (403) mesmo com headers de navegador — típico de
  provedores que barram IPs de datacenter/nuvem, o que inclui o Railway. Descartado.
- **football-data.org**: oficial, documentado, sem risco de bloqueio — mas o
  plano grátis **não cobre Copa Libertadores nem Copa do Brasil**, só as
  principais ligas europeias + Brasileirão + Champions League. Descartado por
  não cobrir tudo que você pediu.
- **API-Football (RapidAPI)**: a escolhida. Cobre tudo (incluindo Libertadores
  e Copa do Brasil), plano grátis de 100 requisições/dia sem restrição por
  competição (o app usa ~2-3/dia), e é a própria infraestrutura da RapidAPI
  feita pra servir clientes de servidor — sem o risco de bloqueio de IP que
  o Sofascore tem.

⚠️ **A URL certa no RapidAPI mudou**: `rapidapi.com/api-sports/api/api-football/details`
(repare no `/details` no final — sem ele dá "API not found").

⚠️ **IDs de liga resolvidos dinamicamente, não hardcoded**: em vez de cravar
os IDs numéricos de cada liga no código (arriscado — não tive como confirmar
todos ao vivo, e já teria errado o de Copa do Brasil se tivesse chutado), o
backend guarda em `backend/src/leagues.js` só o **nome** de cada competição
("Copa Libertadores", "Serie A" + país "Brazil", etc.) e resolve o ID de
verdade pesquisando na própria API-Football na primeira vez que usa cada uma
(`GET /leagues?search=...`), com cache de 24h. Se algum dia uma busca trouxer
a liga errada, o ajuste é só mudar o nome/país em `leagues.js` — não precisa
mexer em mais nada.

## O que o app faz

1. Você escolhe seus times favoritos navegando por liga — Brasileirão, Copa do
   Brasil, Copa Libertadores, Premier League, La Liga, Champions League, Serie
   A italiana, Bundesliga, Ligue 1. A lista de ligas vem do backend
   (`GET /leagues`), não é hardcoded no app.
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
   precisar dele no passo 3 (deploy do backend).

## 2. Conseguir a API key gratuita da API-Football

1. Crie uma conta em https://rapidapi.com
2. Acesse **https://rapidapi.com/api-sports/api/api-football/details** (repare
   no `/details` — a URL sem isso não funciona mais).
3. Clique em "Subscribe to Test" e escolha o plano **Basic** (gratuito).
4. Na aba "Endpoints", copie o valor de `X-RapidAPI-Key` — vai **só** no
   backend, nunca no app.

## 3. Publicar o backend no Railway

1. No [Railway](https://railway.app), crie um novo projeto a partir deste
   repositório GitHub, apontando a **raiz do serviço para a pasta `backend/`**
   (em Settings → Root Directory).
2. Em Variables, defina:
   - `RAPIDAPI_KEY`: a chave do passo 2.
   - `APP_SHARED_SECRET`: qualquer string longa e aleatória, inventada por você
     (ex.: gere com `openssl rand -hex 32`). É o "cadeado" que impede qualquer
     pessoa de usar seu backend/sua cota da API.
   - `FIREBASE_SERVICE_ACCOUNT_BASE64`: o JSON da conta de serviço do passo 1.4,
     codificado em base64 em uma linha só:
     ```bash
     base64 -i caminho/para/service-account.json | tr -d '\n'
     ```
3. Faça o deploy (o Railway detecta Node automaticamente via `package.json` e
   roda `npm start`).
4. Em Settings → Networking, gere um domínio público. Você vai usar essa URL no app.
5. Teste (com o header `X-App-Secret: <valor do passo 2>`):
   ```bash
   curl -s "https://SEU-DOMINIO.up.railway.app/teams?slug=libertadores" \
     -H "X-App-Secret: SEU_SEGREDO"
   ```
   Deve voltar uma lista de times. Se voltar erro mencionando "Liga
   desconhecida" ou nome errado, ajuste `backend/src/leagues.js`.
6. (Opcional) Para o registro de dispositivos sobreviver a redeploys, adicione um
   Volume do Railway montado na pasta `data/` do serviço. Sem isso, um redeploy
   apaga `subscribers.json` — mas é inofensivo: basta abrir o app de novo, que
   ele reenvia o registro automaticamente.

Para testar localmente antes de publicar:

```bash
cd backend
cp .env.example .env   # preencha as três variáveis
npm install
npm start
```

## 4. Configurar o app

Copie o exemplo e preencha com a URL do Railway (passo 3.4) e o mesmo segredo
do passo 3.2:

```bash
cp local.properties.example local.properties
```

```properties
sdk.dir=/caminho/para/o/Android/sdk   # o Android Studio preenche sozinho
backend.baseUrl=https://seu-app.up.railway.app/   # termine com "/"
backend.secret=o_mesmo_valor_de_APP_SHARED_SECRET_no_Railway
```

Confirme também que `app/google-services.json` está no lugar (passo 1.3).

## 5. Rodar no Android Studio

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
│   ├── model/            League.kt (slug + label, vindo do backend), FavoriteTeam.kt
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
├── leagues.js         Catálogo de ligas (nome/país pra buscar — não IDs hardcoded)
├── routes.js         /leagues, /register, /teams, /test-notify
├── footballApi.js    Cliente da API-Football (resolve liga por nome, cache de data/liga)
├── notifier.js        Laço de verificação por minuto (node-cron)
├── firebase.js         Envio de push via firebase-admin
└── store.js            Persistência simples em JSON dos assinantes
```

## Licença

Uso pessoal.
