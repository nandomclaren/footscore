const fs = require('fs');
const path = require('path');

const DATA_DIR = path.join(__dirname, '..', 'data');
const DATA_FILE = path.join(DATA_DIR, 'subscribers.json');

function load() {
  if (!fs.existsSync(DATA_FILE)) return [];
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
  } catch {
    return [];
  }
}

function persist(subscribers) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DATA_FILE, JSON.stringify(subscribers, null, 2));
}

let subscribers = load();

/**
 * Um "assinante" é identificado pelo próprio token do FCM. Cada vez que o app
 * salva uma preferência (ou o token é renovado), ele reenvia tudo e este
 * upsert substitui o registro anterior por completo.
 */
function upsertSubscriber({ fcmToken, favoriteTeamIds, hour, minute, timezone, notifyIfNoGames }) {
  const existingIndex = subscribers.findIndex((s) => s.fcmToken === fcmToken);
  const updated = {
    fcmToken,
    favoriteTeamIds,
    hour,
    minute,
    timezone,
    notifyIfNoGames,
    lastNotifiedDate: existingIndex >= 0 ? subscribers[existingIndex].lastNotifiedDate : null,
  };

  if (existingIndex >= 0) {
    subscribers[existingIndex] = updated;
  } else {
    subscribers.push(updated);
  }
  persist(subscribers);
  return updated;
}

function getAllSubscribers() {
  return subscribers;
}

function markNotifiedToday(fcmToken, dateStr) {
  const subscriber = subscribers.find((s) => s.fcmToken === fcmToken);
  if (subscriber) {
    subscriber.lastNotifiedDate = dateStr;
    persist(subscribers);
  }
}

module.exports = { upsertSubscriber, getAllSubscribers, markNotifiedToday };
