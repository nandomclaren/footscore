# Footscore

App Android minimalista (Kotlin + Jetpack Compose) para notificação diária com os
resultados dos seus times de futebol favoritos.

## O que o app faz

1. Você escolhe seus times favoritos navegando por liga (Brasileirão, Premier League,
   La Liga, Champions League, Serie A italiana, Bundesliga, Ligue 1).
2. Você escolhe um horário diário para receber a notificação.
3. Todo dia, nesse horário, o app roda em segundo plano (WorkManager), consulta a
   API-Football (via RapidAPI) por jogos de hoje e de ontem e, se algum time
   favorito jogou, dispara uma notificação com os placares.
4. Se ninguém jogou, nenhuma notificação é enviada — a menos que você ative
   "Avisar mesmo quando não houver jogos" nas configurações do app.

## Tema

O app segue **Material You**: em Android 12+ ele usa as cores dinâmicas extraídas
do papel de parede do sistema (`dynamicLightColorScheme`/`dynamicDarkColorScheme`
em `ui/theme/Theme.kt`); em versões mais antigas cai para uma paleta verde fixa.
Os modos **claro e escuro** são automáticos, seguindo o tema do sistema
(`isSystemInDarkTheme()`), incluindo o tema base da Activity
(`values/themes.xml` e `values-night/themes.xml`) para não haver flash de tela
branca em dispositivos no modo escuro antes do Compose carregar.

## Stack

- Kotlin + Jetpack Compose + Material 3
- MVVM (ViewModel + StateFlow, `UserPreferencesRepository` como fonte única de verdade)
- DataStore (Preferences) para salvar times favoritos e horário
- Retrofit 2 + Kotlinx Serialization para consumir a API
- WorkManager para a execução diária em segundo plano
- NotificationManager com canal dedicado + tratamento de `POST_NOTIFICATIONS` (Android 13+)

## 1. Conseguir a API key gratuita

O app usa a **API-Football** via RapidAPI (plano gratuito cobre uso pessoal, ~100
requisições/dia — o app faz só 2 por dia, uma para "hoje" e outra para "ontem").

1. Crie uma conta em https://rapidapi.com/api-sports/api/api-football
2. Assine o plano gratuito ("Basic") dessa API.
3. Copie a sua chave (`X-RapidAPI-Key`).

## 2. Configurar a chave no projeto

A chave **nunca** é commitada no repositório. Ela fica em `local.properties`
(arquivo local, já ignorado pelo git) e é injetada em `BuildConfig.RAPIDAPI_KEY`
pelo `app/build.gradle.kts`.

```bash
cp local.properties.example local.properties
```

Edite `local.properties` e preencha:

```properties
sdk.dir=/caminho/para/o/Android/sdk   # o Android Studio preenche sozinho
rapidapi.key=SUA_CHAVE_AQUI
```

## 3. Rodar no Android Studio

1. Abra a pasta do projeto no Android Studio (Giraffe/Koala ou mais recente).
2. Deixe o Gradle sincronizar (ele vai baixar o wrapper do Gradle 8.6 automaticamente
   na primeira sincronização — não é necessário ter o Gradle instalado global).
3. Confirme que `local.properties` tem a `rapidapi.key` preenchida (passo 2).
4. Rode o app (`Run > Run 'app'`) em um emulador ou celular com Android 8.0+ (API 26+).
5. Na primeira abertura, aceite a permissão de notificações (Android 13+).
6. Escolha uma liga, toque nos times para favoritar, defina o horário e toque em
   **"Testar notificação agora"** para validar o fluxo completo sem esperar o
   horário agendado.

> Se o `gradlew` reclamar de wrapper ausente, abra o projeto direto no Android
> Studio — ele detecta e recria o wrapper automaticamente — ou rode
> `gradle wrapper --gradle-version 8.6` uma vez com um Gradle instalado localmente.

## Estrutura do código

```
app/src/main/java/com/footscore/app/
├── data/
│   ├── model/            League.kt, FavoriteTeam.kt
│   ├── local/             UserPreferences.kt, UserPreferencesRepository.kt (DataStore)
│   ├── remote/            ApiConfig.kt, FootballApiService.kt, dto/*.kt
│   └── repository/        FootballRepository.kt
├── worker/                 DailySummaryWorker.kt, WorkScheduler.kt
├── notification/           NotificationHelper.kt
├── ui/
│   ├── theme/              Color.kt, Type.kt, Theme.kt
│   └── home/                HomeViewModel.kt, HomeScreen.kt, components/TimePickerDialog.kt
└── MainActivity.kt
```

### Como o agendamento diário funciona

O `WorkManager` não tem um "rode todo dia às 20h" nativo (o `PeriodicWorkRequest`
não garante horário fixo). Por isso o app usa o padrão recomendado: um
`OneTimeWorkRequest` com o delay exato até o próximo horário configurado
(`WorkScheduler.computeInitialDelayMillis`); ao terminar, o próprio
`DailySummaryWorker` reagenda o próximo disparo para o dia seguinte, sempre lendo
o horário mais atual salvo no DataStore.

### IDs de liga

Os IDs de liga usados (`data/model/League.kt`) são os documentados pela
API-Football (Brasileirão = 71, Premier League = 39, La Liga = 140,
Champions League = 2, etc.). Caso a API mude algum ID, esse é o único arquivo
que precisa de ajuste.

## Próximos passos possíveis (não implementados nesta versão)

Como você mencionou ter um plano no Railway, uma evolução natural seria mover a
lógica de "checar jogos e decidir notificar" para um pequeno backend lá,
substituindo o WorkManager local por push notifications (Firebase Cloud
Messaging) disparadas por um cron job no Railway. Isso evita depender do
Doze/otimização de bateria do Android matando o trabalho em segundo plano, e
esconde sua API key do APK. Não implementei isso agora para manter o escopo do
app pessoal simples — me avise se quiser que eu monte esse backend.

## Licença

Uso pessoal.
