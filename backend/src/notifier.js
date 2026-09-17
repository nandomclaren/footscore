const cron = require('node-cron');
const { getAllSubscribers, markNotifiedToday } = require('./store');
const { getFixturesByDate } = require('./footballApi');
const { sendNotification } = require('./firebase');

function nowInTimezone(timezone) {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(new Date());
  return {
    hour: Number(parts.find((p) => p.type === 'hour').value),
    minute: Number(parts.find((p) => p.type === 'minute').value),
  };
}

function dateStringInTimezone(timezone, offsetDays) {
  const date = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  return new Intl.DateTimeFormat('en-CA', { timeZone: timezone }).format(date); // yyyy-MM-dd
}

async function checkSubscriber(subscriber) {
  const today = dateStringInTimezone(subscriber.timezone, 0);
  if (subscriber.lastNotifiedDate === today) return; // já verificado hoje, evita duplicar

  const yesterday = dateStringInTimezone(subscriber.timezone, -1);
  const [fixturesToday, fixturesYesterday] = await Promise.all([
    getFixturesByDate(today),
    getFixturesByDate(yesterday),
  ]);

  const favoriteIds = new Set(subscriber.favoriteTeamIds);
  const seenIds = new Set();
  const relevant = [...fixturesToday, ...fixturesYesterday].filter((event) => {
    if (seenIds.has(event.id)) return false;
    seenIds.add(event.id);
    const finished = event.status?.type === 'finished';
    const involvesFavorite =
      favoriteIds.has(event.homeTeam?.id) || favoriteIds.has(event.awayTeam?.id);
    return finished && involvesFavorite;
  });

  if (relevant.length > 0) {
    const body = relevant
      .map((e) => `${e.homeTeam.name} ${e.homeScore?.current ?? '-'} x ${e.awayScore?.current ?? '-'} ${e.awayTeam.name}`)
      .join('\n');
    await sendNotification(subscriber.fcmToken, 'Footscore', body);
  } else if (subscriber.notifyIfNoGames) {
    await sendNotification(subscriber.fcmToken, 'Footscore', 'Nenhum dos seus times jogou hoje ou ontem.');
  }

  markNotifiedToday(subscriber.fcmToken, today);
}

function start() {
  // A cada minuto, compara o relógio no fuso horário de cada assinante com o
  // horário que ele configurou; cada um só é checado uma vez por dia.
  cron.schedule('* * * * *', async () => {
    for (const subscriber of getAllSubscribers()) {
      const { hour, minute } = nowInTimezone(subscriber.timezone);
      if (hour === subscriber.hour && minute === subscriber.minute) {
        try {
          await checkSubscriber(subscriber);
        } catch (err) {
          console.error(`Falha ao checar assinante ${subscriber.fcmToken.slice(0, 12)}...:`, err.message);
        }
      }
    }
  });
}

module.exports = { start };
