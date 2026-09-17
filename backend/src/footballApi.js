// Cliente para a API pública (não-oficial, sem chave) do Sofascore — a mesma
// que o site/app deles usa. Como é não-documentada, ela pode mudar sem aviso;
// se algum dia parar de responder, é o primeiro lugar a olhar.
const BASE_URL = 'https://api.sofascore.com/api/v1';
const FIXTURES_CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutos
const SEASON_CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24h — a temporada atual quase nunca muda

const fixturesCache = new Map(); // date -> { fetchedAt, data }
const seasonCache = new Map(); // tournamentId -> { fetchedAt, seasonId }

async function callApi(path) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      // Sem um User-Agent de navegador, a Sofascore costuma responder 403.
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      Accept: 'application/json',
    },
  });
  if (!response.ok) {
    throw new Error(`Sofascore respondeu ${response.status} para ${path}`);
  }
  return response.json();
}

async function getFixturesByDate(date) {
  const cached = fixturesCache.get(date);
  if (cached && Date.now() - cached.fetchedAt < FIXTURES_CACHE_TTL_MS) {
    return cached.data;
  }
  const json = await callApi(`/sport/football/scheduled-events/${date}`);
  const data = json.events || [];
  fixturesCache.set(date, { fetchedAt: Date.now(), data });
  return data;
}

async function getCurrentSeasonId(tournamentId) {
  const cached = seasonCache.get(tournamentId);
  if (cached && Date.now() - cached.fetchedAt < SEASON_CACHE_TTL_MS) {
    return cached.seasonId;
  }
  const json = await callApi(`/unique-tournament/${tournamentId}/seasons`);
  const seasonId = json.seasons?.[0]?.id;
  if (!seasonId) {
    throw new Error(`Nenhuma temporada encontrada para o torneio ${tournamentId}`);
  }
  seasonCache.set(tournamentId, { fetchedAt: Date.now(), seasonId });
  return seasonId;
}

async function getTeamsByTournament(tournamentId) {
  const seasonId = await getCurrentSeasonId(tournamentId);
  const json = await callApi(`/unique-tournament/${tournamentId}/season/${seasonId}/standings/total`);
  const rows = json.standings?.[0]?.rows || [];
  return rows.map((row) => row.team);
}

module.exports = { getFixturesByDate, getTeamsByTournament };
