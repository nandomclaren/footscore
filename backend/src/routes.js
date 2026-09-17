const express = require('express');
const config = require('./config');
const { upsertSubscriber } = require('./store');
const { getTeamsByLeague } = require('./footballApi');
const { sendNotification } = require('./firebase');

const router = express.Router();

function requireSecret(req, res, next) {
  if (req.header('X-App-Secret') !== config.appSecret) {
    return res.status(401).json({ error: 'unauthorized' });
  }
  next();
}

router.use(requireSecret);

// O app chama isso sempre que o usuário salva uma preferência ou o token do
// FCM é renovado. Substitui por completo o registro anterior desse token.
router.post('/register', (req, res) => {
  const { fcmToken, favoriteTeamIds, hour, minute, timezone, notifyIfNoGames } = req.body || {};

  const isValid =
    typeof fcmToken === 'string' && fcmToken.length > 0 &&
    Array.isArray(favoriteTeamIds) &&
    Number.isInteger(hour) && hour >= 0 && hour <= 23 &&
    Number.isInteger(minute) && minute >= 0 && minute <= 59 &&
    typeof timezone === 'string' && timezone.length > 0;

  if (!isValid) {
    return res.status(400).json({ error: 'payload inválido' });
  }

  const subscriber = upsertSubscriber({
    fcmToken,
    favoriteTeamIds,
    hour,
    minute,
    timezone,
    notifyIfNoGames: Boolean(notifyIfNoGames),
  });

  res.json({ ok: true, subscriber });
});

// Proxy do endpoint de times da API-Football, só para a tela de seleção do app
// (assim a chave da API nunca precisa sair do backend).
router.get('/teams', async (req, res) => {
  const leagueId = Number(req.query.league);
  const season = Number(req.query.season);
  if (!leagueId || !season) {
    return res.status(400).json({ error: 'parâmetros league e season são obrigatórios' });
  }
  try {
    const teams = await getTeamsByLeague(leagueId, season);
    res.json({ teams });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// Botão "Testar notificação agora" do app: dispara um push imediato,
// ignorando o agendamento diário.
router.post('/test-notify', async (req, res) => {
  const { fcmToken } = req.body || {};
  if (typeof fcmToken !== 'string' || !fcmToken) {
    return res.status(400).json({ error: 'fcmToken é obrigatório' });
  }
  const sent = await sendNotification(fcmToken, 'Footscore', 'Notificação de teste — tudo funcionando!');
  res.json({ ok: sent });
});

module.exports = router;
