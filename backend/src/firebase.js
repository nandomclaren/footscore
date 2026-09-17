const admin = require('firebase-admin');
const config = require('./config');

const serviceAccountJson = Buffer.from(config.firebaseServiceAccountBase64, 'base64').toString('utf-8');
const serviceAccount = JSON.parse(serviceAccountJson);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

/**
 * Sempre manda mensagem "data-only" (sem bloco "notification"): assim o app
 * chama onMessageReceived tanto em primeiro quanto em segundo plano e decide
 * sozinho como exibir a notificação, com o canal e o texto que quiser.
 */
async function sendNotification(fcmToken, title, body) {
  try {
    await admin.messaging().send({
      token: fcmToken,
      data: { title, body },
      android: { priority: 'high' },
    });
    return true;
  } catch (err) {
    console.error(`Falha ao enviar push para token ${fcmToken.slice(0, 12)}...:`, err.message);
    return false;
  }
}

module.exports = { sendNotification };
