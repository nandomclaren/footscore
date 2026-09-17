// Catálogo das competições que aparecem como chips no app. Em vez de
// hardcodar IDs numéricos da API-Football (arriscado — já erramos IDs
// chutados de outra API nesse projeto), guardamos aqui só o nome/país pra
// buscar, e o backend resolve o ID de verdade na primeira vez que usa cada
// liga (ver footballApi.resolveLeagueId), com cache.
const LEAGUE_CATALOG = {
  brasileirao: { name: 'Serie A', country: 'Brazil', label: 'Brasileirão Série A' },
  'copa-do-brasil': { name: 'Copa do Brasil', country: 'Brazil', label: 'Copa do Brasil' },
  libertadores: { name: 'CONMEBOL Libertadores', country: null, label: 'Copa Libertadores' },
  'premier-league': { name: 'Premier League', country: 'England', label: 'Premier League' },
  'la-liga': { name: 'La Liga', country: 'Spain', label: 'La Liga' },
  'champions-league': { name: 'UEFA Champions League', country: null, label: 'Champions League' },
  'serie-a-italia': { name: 'Serie A', country: 'Italy', label: 'Serie A (Itália)' },
  bundesliga: { name: 'Bundesliga', country: 'Germany', label: 'Bundesliga' },
  'ligue-1': { name: 'Ligue 1', country: 'France', label: 'Ligue 1' },
};

module.exports = { LEAGUE_CATALOG };
