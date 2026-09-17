const config = require('./config');
const { LEAGUE_CATALOG } = require('./leagues');

const BASE_URL = 'https://api-football-v1.p.rapidapi.com/v3';
const API_HOST = 'api-football-v1.p.rapidapi.com';
const FIXTURES_CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutos
const LEAGUE_CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24h — id/temporada da liga quase nunca mudam

const fixturesCache = new Map(); // date -> { fetchedAt, data }
const leagueCache = new Map(); // slug -> { fetchedAt, leagueId, season }

async function callApi(path, params = {}) {
  const url = new URL(`${BASE_URL}${path}`);
  Object.entries(params).forEach(([key, value]) => url.searchParams.set(key, String(value)));

  const response = await fetch(url, {
    headers: {
      'X-RapidAPI-Key': config.rapidApiKey,
      'X-RapidAPI-Host': API_HOST,
    },
  });
  if (!response.ok) {
    throw new Error(`API-Football respondeu ${response.status} para ${path}`);
  }
  return response.json();
}

async function getFixturesByDate(date) {
  const cached = fixturesCache.get(date);
  if (cached && Date.now() - cached.fetchedAt < FIXTURES_CACHE_TTL_MS) {
    return cached.data;
  }
  const json = await callApi('/fixtures', { date });
  const data = json.response || [];
  fixturesCache.set(date, { fetchedAt: Date.now(), data });
  return data;
}

/**
 * Resolve o ID numérico e a temporada atual de uma liga do catálogo (ver
 * leagues.js) pesquisando por nome/país na própria API, em vez de depender
 * de um ID hardcodado que a gente não teve como conferir ao vivo. Cacheado
 * por 24h — na prática, resolvido uma vez só por liga.
 */
async function resolveLeague(slug) {
  const cached = leagueCache.get(slug);
  if (cached && Date.now() - cached.fetchedAt < LEAGUE_CACHE_TTL_MS) {
    return cached;
  }

  const catalogEntry = LEAGUE_CATALOG[slug];
  if (!catalogEntry) {
    throw new Error(`Liga desconhecida: ${slug}`);
  }

  const params = { search: catalogEntry.name };
  const json = await callApi('/leagues', params);
  const results = json.response || [];
  const match = catalogEntry.country
    ? results.find((r) => r.country?.name === catalogEntry.country) || results[0]
    : results[0];

  if (!match) {
    throw new Error(`Nenhuma liga encontrada na API para "${catalogEntry.name}" (slug: ${slug})`);
  }

  const currentSeason = match.seasons?.find((s) => s.current) || match.seasons?.at(-1);
  if (!currentSeason) {
    throw new Error(`Liga "${match.league.name}" não tem temporada atual (slug: ${slug})`);
  }

  const resolved = {
    fetchedAt: Date.now(),
    leagueId: match.league.id,
    leagueName: match.league.name,
    season: currentSeason.year,
  };
  leagueCache.set(slug, resolved);
  return resolved;
}

async function getTeamsByLeague(slug) {
  const { leagueId, season } = await resolveLeague(slug);
  const json = await callApi('/teams', { league: leagueId, season });
  return (json.response || []).map((entry) => entry.team);
}

module.exports = { getFixturesByDate, getTeamsByLeague, resolveLeague };
