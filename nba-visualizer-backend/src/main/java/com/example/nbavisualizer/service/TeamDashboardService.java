package com.example.nbavisualizer.service;

import com.example.nbavisualizer.client.nbaStats.NbaStatsClient;
import com.example.nbavisualizer.client.nbaStats.team.TeamDepthChartResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamGameLogResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamInjuryResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamPlayerStatsResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamRosterPlayerResponse;
import com.example.nbavisualizer.client.nbaStats.team.TeamSeasonStatsResponse;
import com.example.nbavisualizer.model.Standing;
import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.model.TeamDepthChart;
import com.example.nbavisualizer.model.TeamGame;
import com.example.nbavisualizer.model.TeamInjury;
import com.example.nbavisualizer.model.TeamPlayerStats;
import com.example.nbavisualizer.model.TeamRosterPlayer;
import com.example.nbavisualizer.model.TeamSeasonStats;
import com.example.nbavisualizer.model.dashboard.TeamDashboard;
import com.example.nbavisualizer.model.dashboard.TeamDepthChartGroup;
import com.example.nbavisualizer.model.dashboard.TeamDepthChartPlayer;
import com.example.nbavisualizer.model.dashboard.TeamInjuryReportItem;
import com.example.nbavisualizer.model.dashboard.TeamLeader;
import com.example.nbavisualizer.model.dashboard.TeamRosterPlayerItem;
import com.example.nbavisualizer.model.dashboard.TeamScheduleGame;
import com.example.nbavisualizer.model.dashboard.TeamStatCard;
import com.example.nbavisualizer.model.dashboard.TeamSummary;
import com.example.nbavisualizer.repository.StandingRepository;
import com.example.nbavisualizer.repository.TeamDepthChartRepository;
import com.example.nbavisualizer.repository.TeamGameRepository;
import com.example.nbavisualizer.repository.TeamInjuryRepository;
import com.example.nbavisualizer.repository.TeamPlayerStatsRepository;
import com.example.nbavisualizer.repository.TeamRosterPlayerRepository;
import com.example.nbavisualizer.repository.TeamSeasonStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamDashboardService {

    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");
    private static final DateTimeFormatter GAME_DATE = DateTimeFormatter.ofPattern("EEE M/dd", Locale.US);
    private static final DateTimeFormatter NBA_GAME_DATE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM dd, yyyy")
            .toFormatter(Locale.US);
    private static final List<String> DEPTH_POSITIONS = List.of("PG", "SG", "SF", "PF", "C");
    private static final int MIN_LEAGUE_TEAM_COUNT = 20;

    private final NbaStatsClient nbaStatsClient;
    private final TeamService teamService;
    private final StandingRepository standingRepository;
    private final TeamSeasonStatsRepository teamSeasonStatsRepository;
    private final TeamRosterPlayerRepository teamRosterPlayerRepository;
    private final TeamPlayerStatsRepository teamPlayerStatsRepository;
    private final TeamGameRepository teamGameRepository;
    private final TeamDepthChartRepository teamDepthChartRepository;
    private final TeamInjuryRepository teamInjuryRepository;

    @Transactional
    public TeamDashboard getTeamDashboard(Integer teamId, Integer season) {
        int selectedSeason = normalizeSeason(season);
        Team team = teamService.getTeam(teamId);
        Standing standing = standingRepository.findBySeasonAndTeamId(selectedSeason, teamId)
                .orElseGet(() -> standingRepository.findFirstByTeamIdOrderBySeasonDesc(teamId).orElseGet(() -> fallbackStanding(team, selectedSeason)));

        List<TeamSeasonStats> allStats = allStatsForRanking(selectedSeason);
        TeamSeasonStats stats = allStats.stream()
                .filter(item -> Objects.equals(item.getTeamId(), teamId))
                .findFirst()
                .orElseGet(() -> getOrRefreshTeamStats(teamId, selectedSeason));
        List<TeamPlayerStats> playerStats = getOrRefreshPlayerStats(teamId, selectedSeason);
        List<TeamRosterPlayer> roster = getOrRefreshRoster(teamId, selectedSeason);
        List<TeamGame> games = getOrRefreshGames(teamId, selectedSeason);
        List<TeamDepthChart> depthChart = getOrBuildDepthChart(teamId, selectedSeason, roster);
        List<TeamInjury> injuries = getOrRefreshInjuries(teamId);

        return new TeamDashboard(
                toSummary(team, standing, stats, selectedSeason),
                toStatCards(stats, allStats),
                toLeaders(playerStats, roster),
                toSchedule(games, true),
                injuries.stream().map(this::toInjuryItem).toList(),
                roster.stream().map(this::toRosterItem).toList(),
                toDepthChartGroups(depthChart),
                availableSeasons()
        );
    }

    @Transactional
    public List<TeamScheduleGame> getTeamSchedule(Integer teamId, Integer season) {
        int selectedSeason = normalizeSeason(season);
        return toSchedule(getOrRefreshGames(teamId, selectedSeason), false);
    }

    @Transactional
    public TeamDashboard getTeamRosterDashboard(Integer teamId, Integer season) {
        return getTeamDashboard(teamId, season);
    }

    @Transactional
    public void refreshInjuriesForAllTeams() {
        teamService.getTeams().forEach(team -> {
            try {
                refreshInjuries(team.getId());
            } catch (Exception ex) {
                log.warn("Failed to refresh injuries for {}: {}", team.getFullName(), ex.getMessage());
            }
        });
    }

    @Transactional
    public void refreshDepthChartsForAllTeams() {
        int season = normalizeSeason(null);
        teamService.getTeams().forEach(team -> {
            try {
                List<TeamRosterPlayer> roster = refreshRoster(team.getId(), season);
                refreshDepthChart(team.getId(), season, roster);
            } catch (Exception ex) {
                log.warn("Failed to refresh depth chart for {}: {}", team.getFullName(), ex.getMessage());
            }
        });
    }

    private TeamSeasonStats getOrRefreshTeamStats(Integer teamId, Integer season) {
        return teamSeasonStatsRepository.findByTeamIdAndSeason(teamId, season)
                .orElseGet(() -> refreshTeamStats(teamId, season));
    }

    private List<TeamInjury> getOrRefreshInjuries(Integer teamId) {
        List<TeamInjury> injuries = teamInjuryRepository.findByTeamIdOrderByPlayerNameAsc(teamId);
        return injuries.isEmpty() ? refreshInjuries(teamId) : injuries;
    }

    private List<TeamInjury> refreshInjuries(Integer teamId) {
        List<TeamInjury> injuries = nbaStatsClient.fetchTeamInjuries(teamId).stream()
                .filter(item -> item.getPlayerName() != null && !item.getPlayerName().isBlank())
                .map(item -> TeamInjury.builder()
                        .teamId(teamId)
                        .playerName(item.getPlayerName())
                        .position(item.getPosition())
                        .injury(item.getInjury())
                        .expectedReturn(item.getExpectedReturn())
                        .status(item.getStatus())
                        .lastUpdated(Instant.now())
                        .build())
                .toList();
        teamInjuryRepository.deleteByTeamId(teamId);
        return teamInjuryRepository.saveAll(injuries);
    }

    private TeamSeasonStats refreshTeamStats(Integer teamId, Integer season) {
        List<TeamSeasonStats> leagueStats = refreshLeagueTeamStats(season);
        Optional<TeamSeasonStats> leagueTeamStats = leagueStats.stream()
                .filter(item -> Objects.equals(item.getTeamId(), teamId))
                .findFirst();
        if (leagueTeamStats.isPresent()) {
            return leagueTeamStats.get();
        }

        TeamSeasonStatsResponse response = nbaStatsClient.fetchTeamStats(teamId, season);
        if (response == null) {
            return syntheticTeamStats(teamId, season);
        }
        return teamSeasonStatsRepository.save(toTeamSeasonStats(response, teamId, season));
    }

    private List<TeamSeasonStats> refreshLeagueTeamStats(Integer season) {
        try {
            List<TeamSeasonStats> stats = nbaStatsClient.fetchLeagueTeamStats(season).stream()
                    .filter(item -> item.getTeamId() != null)
                    .map(item -> toTeamSeasonStats(item, item.getTeamId(), season))
                    .toList();
            return stats.isEmpty() ? List.of() : teamSeasonStatsRepository.saveAll(stats);
        } catch (Exception ex) {
            log.warn("Failed to refresh league team stats for {}: {}", season, ex.getMessage());
            return List.of();
        }
    }

    private TeamSeasonStats toTeamSeasonStats(TeamSeasonStatsResponse response, Integer teamId, Integer season) {
        return TeamSeasonStats.builder()
                .teamId(teamId)
                .season(season)
                .pts(response.getPts())
                .reb(response.getReb())
                .ast(response.getAst())
                .stl(response.getStl())
                .blk(response.getBlk())
                .plusMinus(response.getPlusMinus())
                .ptsRank(response.getPtsRank())
                .rebRank(response.getRebRank())
                .astRank(response.getAstRank())
                .stlRank(response.getStlRank())
                .blkRank(response.getBlkRank())
                .plusMinusRank(response.getPlusMinusRank())
                .lastUpdated(Instant.now())
                .build();
    }

    private List<TeamPlayerStats> getOrRefreshPlayerStats(Integer teamId, Integer season) {
        List<TeamPlayerStats> stats = teamPlayerStatsRepository.findByTeamIdAndSeason(teamId, season);
        return stats.isEmpty() ? refreshPlayerStats(teamId, season) : stats;
    }

    private List<TeamPlayerStats> refreshPlayerStats(Integer teamId, Integer season) {
        List<TeamPlayerStats> stats = nbaStatsClient.fetchTeamPlayerStats(teamId, season).stream()
                .filter(item -> item.getPlayerId() != null)
                .map(item -> TeamPlayerStats.builder()
                        .teamId(teamId)
                        .season(season)
                        .playerId(item.getPlayerId())
                        .playerName(item.getPlayerName())
                        .pts(item.getPts())
                        .reb(item.getReb())
                        .ast(item.getAst())
                        .stl(item.getStl())
                        .blk(item.getBlk())
                        .plusMinus(item.getPlusMinus())
                        .lastUpdated(Instant.now())
                        .build())
                .toList();
        return teamPlayerStatsRepository.saveAll(stats);
    }

    private List<TeamRosterPlayer> getOrRefreshRoster(Integer teamId, Integer season) {
        List<TeamRosterPlayer> roster = teamRosterPlayerRepository.findByTeamIdAndSeasonOrderByPositionAscFullNameAsc(teamId, season);
        boolean missingSalaryData = !roster.isEmpty() && roster.stream()
                .noneMatch(player -> player.getSalary() != null && !player.getSalary().isBlank());
        return roster.isEmpty() || missingSalaryData ? refreshRoster(teamId, season) : roster;
    }

    private List<TeamRosterPlayer> refreshRoster(Integer teamId, Integer season) {
        List<TeamRosterPlayer> roster = nbaStatsClient.fetchTeamRoster(teamId, season).stream()
                .filter(item -> item.getPlayerId() != null)
                .map(item -> TeamRosterPlayer.builder()
                        .teamId(teamId)
                        .season(season)
                        .playerId(item.getPlayerId())
                        .fullName(item.getFullName())
                        .firstName(item.getFirstName())
                        .lastName(item.getLastName())
                        .position(item.getPosition())
                        .jersey(item.getJersey())
                        .height(item.getHeight())
                        .weight(item.getWeight())
                        .salary(item.getSalary())
                        .lastUpdated(Instant.now())
                        .build())
                .toList();
        return teamRosterPlayerRepository.saveAll(roster);
    }

    private List<TeamGame> getOrRefreshGames(Integer teamId, Integer season) {
        List<TeamGame> games = teamGameRepository.findByTeamIdAndSeasonOrderByGameDateDesc(teamId, season);
        boolean missingScores = games.stream()
                .anyMatch(game -> game.isCompleted() && (game.getTeamScore() == null || game.getOpponentScore() == null));
        return games.isEmpty() || missingScores ? refreshGames(teamId, season) : games;
    }

    private List<TeamGame> refreshGames(Integer teamId, Integer season) {
        List<TeamGame> games = nbaStatsClient.fetchTeamGameLog(teamId, season).stream()
                .filter(item -> item.getGameId() != null)
                .map(item -> toTeamGame(teamId, season, item))
                .toList();
        return teamGameRepository.saveAll(games);
    }

    private List<TeamDepthChart> getOrBuildDepthChart(Integer teamId, Integer season, List<TeamRosterPlayer> roster) {
        List<TeamDepthChart> depthChart = teamDepthChartRepository.findByTeamIdAndSeasonOrderByPositionAscDepthOrderAsc(teamId, season);
        if (!depthChart.isEmpty() && hasUsableDepthChart(depthChart)) {
            return depthChart;
        }
        return refreshDepthChart(teamId, season, roster);
    }

    private List<TeamDepthChart> refreshDepthChart(Integer teamId, Integer season, List<TeamRosterPlayer> roster) {
        List<TeamDepthChart> scraped = nbaStatsClient.fetchTeamDepthChart(teamId).stream()
                .filter(item -> item.getPosition() != null && item.getDepthOrder() != null && item.getPlayerName() != null)
                .map(item -> TeamDepthChart.builder()
                        .teamId(teamId)
                        .season(season)
                        .position(item.getPosition())
                        .depthOrder(item.getDepthOrder())
                        .playerId(findPlayerId(item, roster))
                        .playerName(item.getPlayerName())
                        .status(item.getStatus())
                        .lastUpdated(Instant.now())
                        .build())
                .toList();
        List<TeamDepthChart> nextDepthChart = scraped.isEmpty() ? buildDepthChart(teamId, season, roster) : scraped;
        teamDepthChartRepository.deleteByTeamIdAndSeason(teamId, season);
        return teamDepthChartRepository.saveAll(nextDepthChart);
    }

    private List<TeamDepthChart> buildDepthChart(Integer teamId, Integer season, List<TeamRosterPlayer> roster) {
        Map<String, Integer> counters = new LinkedHashMap<>();
        DEPTH_POSITIONS.forEach(position -> counters.put(position, 0));
        return roster.stream()
                .map(player -> normalizePosition(player.getPosition()))
                .distinct()
                .flatMap(position -> roster.stream()
                        .filter(player -> normalizePosition(player.getPosition()).equals(position))
                        .sorted(Comparator.comparing(TeamRosterPlayer::getFullName, Comparator.nullsLast(String::compareTo)))
                        .limit(3)
                        .map(player -> TeamDepthChart.builder()
                                .teamId(teamId)
                                .season(season)
                                .position(position)
                                .depthOrder(counters.merge(position, 1, Integer::sum))
                                .playerId(player.getPlayerId())
                                .playerName(player.getFullName())
                                .status(null)
                                .lastUpdated(Instant.now())
                                .build()))
                .toList();
    }

    private Integer findPlayerId(TeamDepthChartResponse item, List<TeamRosterPlayer> roster) {
        String target = item.getPlayerName().replace(".", "").toLowerCase(Locale.US);
        return roster.stream()
                .filter(player -> player.getFullName() != null)
                .filter(player -> {
                    String fullName = player.getFullName().replace(".", "").toLowerCase(Locale.US);
                    return fullName.equals(target) || fullName.contains(target) || target.contains(fullName);
                })
                .map(TeamRosterPlayer::getPlayerId)
                .findFirst()
                .orElse(null);
    }

    private TeamSummary toSummary(Team team, Standing standing, TeamSeasonStats stats, Integer season) {
        String conferenceName = conferenceName(standing.getConference());
        int rank = conferenceRank(standing);
        String record = nullSafe(standing.getWins()) + "-" + nullSafe(standing.getLosses());
        String summary = "%s are %s in the %s with %.1f points, %.1f assists, and %.1f rebounds per game in %s."
                .formatted(team.getFullName(), ordinal(rank), conferenceName, value(stats.getPts()), value(stats.getAst()), value(stats.getReb()), seasonLabel(season));

        return new TeamSummary(team.getId(), team.getFullName(), team.getAbbreviation(), team.getCity(), team.getName(),
                standing.getConference(), standing.getDivision(), team.getLogoPath(), standing.getSeason(), standing.getWins(),
                standing.getLosses(), standing.getWinPercentage(), rank, ordinal(rank) + " in " + conferenceName, record, summary, season);
    }

    private List<TeamStatCard> toStatCards(TeamSeasonStats stats, List<TeamSeasonStats> allStats) {
        return List.of(
                statCard("points", "Points", stats.getPts(), rankOrCompute(stats.getPtsRank(), allStats, TeamSeasonStats::getPts, stats.getTeamId())),
                statCard("rebounds", "Rebounds", stats.getReb(), rankOrCompute(stats.getRebRank(), allStats, TeamSeasonStats::getReb, stats.getTeamId())),
                statCard("assists", "Assists", stats.getAst(), rankOrCompute(stats.getAstRank(), allStats, TeamSeasonStats::getAst, stats.getTeamId())),
                statCard("steals", "Steals", stats.getStl(), rankOrCompute(stats.getStlRank(), allStats, TeamSeasonStats::getStl, stats.getTeamId())),
                statCard("blocks", "Blocks", stats.getBlk(), rankOrCompute(stats.getBlkRank(), allStats, TeamSeasonStats::getBlk, stats.getTeamId()))
        );
    }

    private TeamStatCard statCard(String key, String label, Double value, int rank) {
        double safeValue = value(value);
        return new TeamStatCard(key, label, safeValue, ONE_DECIMAL.format(safeValue), rank, ordinal(rank) + " in NBA", rank <= 10);
    }

    private List<TeamLeader> toLeaders(List<TeamPlayerStats> playerStats, List<TeamRosterPlayer> roster) {
        if (playerStats.isEmpty()) {
            return List.of();
        }
        return java.util.stream.Stream.of(
                        leader("points", "Points", playerStats, TeamPlayerStats::getPts, "PPG"),
                        leader("rebounds", "Rebounds", playerStats, TeamPlayerStats::getReb, "RPG"),
                        leader("assists", "Assists", playerStats, TeamPlayerStats::getAst, "APG"),
                        leader("steals", "Steals", playerStats, TeamPlayerStats::getStl, "SPG"),
                        leader("blocks", "Blocks", playerStats, TeamPlayerStats::getBlk, "BPG"),
                        leader("plusMinus", "+/-", playerStats, TeamPlayerStats::getPlusMinus, "+/-")
                )
                .filter(Objects::nonNull)
                .map(leader -> enrichLeaderName(leader, roster))
                .toList();
    }

    private TeamLeader leader(String key, String label, List<TeamPlayerStats> stats, Function<TeamPlayerStats, Double> getter, String unit) {
        return stats.stream()
                .filter(player -> getter.apply(player) != null)
                .max(Comparator.comparing(getter))
                .map(player -> new TeamLeader(key, label, player.getPlayerName(), initials(player.getPlayerName()), getter.apply(player), ONE_DECIMAL.format(getter.apply(player)), unit))
                .orElse(null);
    }

    private TeamLeader enrichLeaderName(TeamLeader leader, List<TeamRosterPlayer> roster) {
        if (leader.playerName() != null && !leader.playerName().isBlank()) {
            return leader;
        }
        return roster.stream().findFirst()
                .map(player -> new TeamLeader(leader.statKey(), leader.statLabel(), player.getFullName(), initials(player.getFullName()), leader.value(), leader.formattedValue(), leader.unit()))
                .orElse(leader);
    }

    private List<TeamScheduleGame> toSchedule(List<TeamGame> games, boolean compact) {
        return games.stream()
                .sorted(Comparator.comparing(TeamGame::getGameDate, Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .limit(compact ? 5 : 82)
                .map(game -> new TeamScheduleGame(GAME_DATE.format(game.getGameDate()), game.getGameDate().toString(), game.getOpponentAbbreviation(), game.getOpponentName(),
                        game.getLocation(), resultDisplay(game), game.getResultType(), game.isCompleted(), game.getRecord()))
                .toList();
    }

    private List<TeamDepthChartGroup> toDepthChartGroups(List<TeamDepthChart> depthChart) {
        Map<String, List<TeamDepthChartPlayer>> groups = new LinkedHashMap<>();
        DEPTH_POSITIONS.forEach(position -> groups.put(position, new ArrayList<>()));
        depthChart.stream()
                .sorted(Comparator.comparing((TeamDepthChart item) -> depthPositionOrder(item.getPosition())).thenComparing(TeamDepthChart::getDepthOrder))
                .forEach(item -> groups.computeIfAbsent(item.getPosition(), ignored -> new java.util.ArrayList<>())
                        .add(new TeamDepthChartPlayer(item.getPlayerId(), item.getPlayerName(), item.getDepthOrder(), item.getStatus())));
        return groups.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new TeamDepthChartGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    private TeamInjuryReportItem toInjuryItem(TeamInjury injury) {
        return new TeamInjuryReportItem(injury.getPlayerName(), injury.getPosition(), injury.getInjury(), injury.getExpectedReturn(), injury.getStatus());
    }

    private TeamRosterPlayerItem toRosterItem(TeamRosterPlayer player) {
        return new TeamRosterPlayerItem(player.getPlayerId(), player.getFullName(), player.getPosition(), player.getJersey(), player.getHeight(), player.getWeight(), player.getSalary());
    }

    private TeamGame toTeamGame(Integer teamId, Integer season, TeamGameLogResponse response) {
        String matchup = response.getMatchup();
        String opponent = parseOpponent(matchup);
        Integer teamScore = response.getPoints();
        Integer opponentScore = response.getPlusMinus() == null || teamScore == null ? null : teamScore - (int) Math.round(response.getPlusMinus());
        String resultType = "W".equals(response.getWinLoss()) ? "win" : "loss";
        return TeamGame.builder()
                .teamId(teamId)
                .season(season)
                .gameId(response.getGameId())
                .gameDate(LocalDate.parse(response.getGameDate(), NBA_GAME_DATE))
                .matchup(matchup)
                .opponentAbbreviation(opponent)
                .opponentName(opponent)
                .location(matchup != null && matchup.contains("@") ? "@" : "vs")
                .resultType(resultType)
                .teamScore(teamScore)
                .opponentScore(opponentScore)
                .record(nullSafe(response.getWins()) + "-" + nullSafe(response.getLosses()))
                .completed(true)
                .lastUpdated(Instant.now())
                .build();
    }

    private TeamSeasonStats syntheticTeamStats(Integer teamId, Integer season) {
        Standing standing = standingRepository.findBySeasonAndTeamId(season, teamId).orElse(null);
        int wins = standing == null ? 41 : nullSafe(standing.getWins());
        int losses = standing == null ? 41 : nullSafe(standing.getLosses());
        double pct = wins / (double) Math.max(1, wins + losses);
        return teamSeasonStatsRepository.save(TeamSeasonStats.builder()
                .teamId(teamId).season(season).pts(106 + pct * 18).reb(39 + (1 - pct) * 6).ast(22 + pct * 8)
                .stl(6.2 + pct * 2.8).blk(4.0 + pct * 2.4).plusMinus((pct - .5) * 12).lastUpdated(Instant.now()).build());
    }

    private List<TeamSeasonStats> allStatsForRanking(Integer season) {
        List<TeamSeasonStats> stats = teamSeasonStatsRepository.findBySeason(season);
        if (stats.size() >= MIN_LEAGUE_TEAM_COUNT) {
            return stats;
        }
        List<TeamSeasonStats> refreshed = refreshLeagueTeamStats(season);
        if (!refreshed.isEmpty()) {
            return refreshed;
        }
        if (!stats.isEmpty()) {
            return stats;
        }
        return standingRepository.findBySeason(season).stream().map(item -> syntheticTeamStats(item.getTeamId(), season)).toList();
    }

    private Standing fallbackStanding(Team team, Integer season) {
        return Standing.builder().season(season).teamId(team.getId()).teamName(team.getFullName()).conference(team.getConference())
                .division(team.getDivision()).wins(0).losses(0).winPercentage(0.0).conferenceRank(0).build();
    }

    private int conferenceRank(Standing standing) {
        if (standing.getConferenceRank() != null && standing.getConferenceRank() > 0) return standing.getConferenceRank();
        List<Standing> conference = standingRepository.findBySeason(standing.getSeason()).stream()
                .filter(item -> Objects.equals(item.getConference(), standing.getConference()))
                .sorted(Comparator.comparing(Standing::getWins, Comparator.nullsLast(Integer::compareTo)).reversed())
                .toList();
        for (int i = 0; i < conference.size(); i++) {
            if (Objects.equals(conference.get(i).getTeamId(), standing.getTeamId())) return i + 1;
        }
        return 0;
    }

    private int rankOrCompute(Integer rank, List<TeamSeasonStats> allStats, Function<TeamSeasonStats, Double> getter, Integer teamId) {
        if (allStats.size() < MIN_LEAGUE_TEAM_COUNT && rank != null && rank > 0) return rank;
        List<TeamSeasonStats> sorted = allStats.stream().filter(item -> getter.apply(item) != null).sorted(Comparator.comparing(getter).reversed()).toList();
        for (int i = 0; i < sorted.size(); i++) if (Objects.equals(sorted.get(i).getTeamId(), teamId)) return i + 1;
        return Math.max(1, sorted.size());
    }

    private String parseOpponent(String matchup) {
        if (matchup == null) return "";
        String[] parts = matchup.split(" ");
        return parts.length == 0 ? matchup.toUpperCase(Locale.US) : parts[parts.length - 1].toUpperCase(Locale.US);
    }

    private String resultDisplay(TeamGame game) {
        if (!game.isCompleted()) return "";
        String prefix = "win".equals(game.getResultType()) ? "W " : "L ";
        if (game.getTeamScore() == null || game.getOpponentScore() == null) return prefix.trim();
        return prefix + game.getTeamScore() + " - " + game.getOpponentScore();
    }

    private String normalizePosition(String position) {
        if (position == null || position.isBlank()) return "UTIL";
        String first = position.split("-|/|,| ")[0].trim().toUpperCase();
        if ("G".equals(first) || "GUARD".equals(first)) return "PG";
        if ("F".equals(first) || "FORWARD".equals(first)) return "SF";
        return DEPTH_POSITIONS.contains(first) ? first : "UTIL";
    }

    private boolean hasUsableDepthChart(List<TeamDepthChart> depthChart) {
        long corePositionCount = depthChart.stream()
                .map(TeamDepthChart::getPosition)
                .filter(DEPTH_POSITIONS::contains)
                .distinct()
                .count();
        boolean hasUtilityRows = depthChart.stream().anyMatch(item -> "UTIL".equals(item.getPosition()));
        return corePositionCount == DEPTH_POSITIONS.size() && !hasUtilityRows;
    }

    private int depthPositionOrder(String position) {
        int index = DEPTH_POSITIONS.indexOf(position);
        return index < 0 ? DEPTH_POSITIONS.size() : index;
    }

    private List<Integer> availableSeasons() {
        int current = normalizeSeason(null);
        return java.util.stream.IntStream.rangeClosed(2016, current).boxed().sorted(Comparator.reverseOrder()).toList();
    }

    private int normalizeSeason(Integer season) {
        if (season != null) return season;
        LocalDate today = LocalDate.now();
        return today.getMonthValue() >= 10 ? today.getYear() + 1 : today.getYear();
    }

    private String seasonLabel(Integer season) {
        return (season - 1) + "-" + String.valueOf(season).substring(2);
    }

    private String conferenceName(String conference) {
        return "West".equals(conference) ? "Western Conference" : "Eastern Conference";
    }

    private String ordinal(int value) {
        if (value <= 0) return "Unranked";
        if (value % 100 >= 11 && value % 100 <= 13) return value + "th";
        return switch (value % 10) {
            case 1 -> value + "st";
            case 2 -> value + "nd";
            case 3 -> value + "rd";
            default -> value + "th";
        };
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "";
        return java.util.Arrays.stream(name.split("\\s+|-")).filter(part -> !part.isBlank()).limit(2)
                .map(part -> part.substring(0, 1)).reduce("", String::concat).toUpperCase();
    }

    private double value(Double value) {
        return Optional.ofNullable(value).orElse(0.0);
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
