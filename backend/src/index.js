const express = require('express');
const config = require('./config');
const routes = require('./routes');
const notifier = require('./notifier');

const app = express();
app.use(express.json());

app.get('/health', (_req, res) => res.json({ status: 'ok' }));
app.use(routes);

notifier.start();

app.listen(config.port, () => {
  console.log(`Footscore backend rodando na porta ${config.port}`);
});
