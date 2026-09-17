const config = require('./config');

const BASE_URL = 'https://api-football-v1.p.rapidapi.com/v3';
const API_HOST = 'api-football-v1.p.rapidapi.com';
const CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutos

// Cache simples em memória por data, para não repetir a mesma chamada de
// /fixtures a cada assinante checado no mesmo minuto.
const fixturesCache = new Map();

async function callApi(path, params) {
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
  if (cached && Date.now() - cached.fetchedAt < CACHE_TTL_MS) {
    return cached.data;
  }
  const json = await callApi('/fixtures', { date });
  const data = json.response || [];
  fixturesCache.set(date, { fetchedAt: Date.now(), data });
  return data;
}

async function getTeamsByLeague(leagueId, season) {
  const json = await callApi('/teams', { league: leagueId, season });
  return (json.response || []).map((entry) => entry.team);
}

module.exports = { getFixturesByDate, getTeamsByLeague };
