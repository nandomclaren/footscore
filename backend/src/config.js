require('dotenv').config();

function required(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Variável de ambiente obrigatória ausente: ${name}`);
  }
  return value;
}

module.exports = {
  port: process.env.PORT || 3000,
  appSecret: required('APP_SHARED_SECRET'),
  firebaseServiceAccountBase64: required('FIREBASE_SERVICE_ACCOUNT_BASE64'),
};
