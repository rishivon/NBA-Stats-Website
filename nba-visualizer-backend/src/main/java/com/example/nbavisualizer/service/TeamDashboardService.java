package com.example.nbavisualizer.service;

import com.example.nbavisualizer.model.Standing;
import com.example.nbavisualizer.model.Team;
import com.example.nbavisualizer.model.dashboard.TeamDashboard;
import com.example.nbavisualizer.model.dashboard.TeamInjuryReportItem;
import com.example.nbavisualizer.model.dashboard.TeamLeader;
import com.example.nbavisualizer.model.dashboard.TeamScheduleGame;
import com.example.nbavisualizer.model.dashboard.TeamStatCard;
import com.example.nbavisualizer.model.dashboard.TeamSummary;
import com.example.nbavisualizer.repository.StandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeamDashboardService {

    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");
    private static final DateTimeFormatter GAME_DATE = DateTimeFormatter.ofPattern("EEE M/dd");

    private final TeamService teamService;
    private final StandingRepository standingRepository;

    public TeamDashboard getTeamDashboard(Integer teamId) {
        Team team = teamService.getTeam(teamId);
        Standing standing = standingRepository.findFirstByTeamIdOrderBySeasonDesc(teamId)
                .orElseGet(() -> fallbackStanding(team));
        List<Standing> seasonStandings = standingRepository.findBySeason(standing.getSeason());
        List<TeamMetric> metrics = metricsFor(seasonStandings.isEmpty() ? List.of(standing) : seasonStandings);
        TeamMetric metric = metrics.stream()
                .filter(item -> Objects.equals(item.teamId(), teamId))
                .findFirst()
                .orElseGet(() -> metricFor(standing));

        return new TeamDashboard(
                toSummary(team, standing, metric),
                toStatCards(metric, metrics),
                toLeaders(team, metric),
                toSchedule(team, standing, seasonStandings),
                toInjuries(team)
        );
    }

    private TeamSummary toSummary(Team team, Standing standing, TeamMetric metric) {
        String conferenceName = conferenceName(standing.getConference());
        int rank = conferenceRank(standing);
        String record = nullSafe(standing.getWins()) + "-" + nullSafe(standing.getLosses());
        String summary = "%s are %s in the %s and profile as a %s team, pairing %.1f projected points with %.1f assists per game."
                .formatted(
                        team.getFullName(),
                        ordinal(rank),
                        conferenceName,
                        identityFor(metric),
                        metric.points(),
                        metric.assists()
                );

        return new TeamSummary(
                team.getId(),
                team.getFullName(),
                team.getAbbreviation(),
                team.getCity(),
                team.getName(),
                standing.getConference(),
                standing.getDivision(),
                team.getLogoPath(),
                standing.getSeason(),
                standing.getWins(),
                standing.getLosses(),
                standing.getWinPercentage(),
                rank,
                ordinal(rank) + " in " + conferenceName,
                record,
                summary
        );
    }

    private List<TeamStatCard> toStatCards(TeamMetric metric, List<TeamMetric> allMetrics) {
        return List.of(
                statCard("points", "Points", metric.points(), rank(allMetrics, TeamMetric::points, metric.teamId()), true),
                statCard("rebounds", "Rebounds", metric.rebounds(), rank(allMetrics, TeamMetric::rebounds, metric.teamId()), false),
                statCard("assists", "Assists", metric.assists(), rank(allMetrics, TeamMetric::assists, metric.teamId()), false),
                statCard("steals", "Steals", metric.steals(), rank(allMetrics, TeamMetric::steals, metric.teamId()), true),
                statCard("blocks", "Blocks", metric.blocks(), rank(allMetrics, TeamMetric::blocks, metric.teamId()), true)
        );
    }

    private TeamStatCard statCard(String key, String label, double value, int rank, boolean highlighted) {
        return new TeamStatCard(key, label, value, ONE_DECIMAL.format(value), rank, ordinal(rank) + " in NBA", highlighted && rank <= 10);
    }

    private List<TeamLeader> toLeaders(Team team, TeamMetric metric) {
        List<String> names = leaderNames(team);
        return List.of(
                leader("points", "Points", names.get(0), metric.points() * 0.26, "PPG"),
                leader("rebounds", "Rebounds", names.get(1), metric.rebounds() * 0.24, "RPG"),
                leader("assists", "Assists", names.get(2), metric.assists() * 0.31, "APG"),
                leader("steals", "Steals", names.get(3), metric.steals() * 0.29, "SPG"),
                leader("blocks", "Blocks", names.get(4), metric.blocks() * 0.32, "BPG")
        );
    }

    private TeamLeader leader(String key, String label, String playerName, double value, String unit) {
        return new TeamLeader(key, label, playerName, initials(playerName), value, ONE_DECIMAL.format(value), unit);
    }

    private List<TeamScheduleGame> toSchedule(Team team, Standing standing, List<Standing> seasonStandings) {
        List<Standing> opponents = seasonStandings.stream()
                .filter(item -> !Objects.equals(item.getTeamId(), team.getId()))
                .sorted(Comparator.comparing(Standing::getWins, Comparator.nullsLast(Integer::compareTo)).reversed())
                .limit(4)
                .toList();

        LocalDate today = LocalDate.now();
        return java.util.stream.IntStream.range(0, opponents.size())
                .mapToObj(index -> {
                    Standing opponent = opponents.get(index);
                    boolean completed = index < 2;
                    boolean home = index % 2 == 1;
                    int teamScore = 104 + nullSafe(standing.getWins()) % 24 + index * 2;
                    int opponentScore = 98 + nullSafe(opponent.getWins()) % 22 + index;
                    String result = completed
                            ? (teamScore >= opponentScore ? "W " : "L ") + teamScore + " - " + opponentScore
                            : (home ? "7:30 PM" : "8:00 PM");
                    return new TeamScheduleGame(
                            GAME_DATE.format(today.plusDays((long) index * 2 - 4)),
                            opponent.getTeamAbbr(),
                            opponent.getTeamName(),
                            home ? "vs" : "@",
                            result,
                            completed ? (teamScore >= opponentScore ? "win" : "loss") : "upcoming",
                            completed
                    );
                })
                .toList();
    }

    private List<TeamInjuryReportItem> toInjuries(Team team) {
        List<String> names = leaderNames(team);
        if (team.getId() % 5 == 0) {
            return List.of();
        }

        return List.of(
                new TeamInjuryReportItem(names.get(1), "Lower Body", "Day-to-Day", "questionable"),
                new TeamInjuryReportItem(names.get(4), "Ankle", "Next Week", "out")
        );
    }

    private List<TeamMetric> metricsFor(List<Standing> standings) {
        return standings.stream().map(this::metricFor).toList();
    }

    private TeamMetric metricFor(Standing standing) {
        int wins = nullSafe(standing.getWins());
        int losses = nullSafe(standing.getLosses());
        int total = Math.max(1, wins + losses);
        double winPct = wins / (double) total;
        double paceBoost = ("West".equals(standing.getConference()) ? 1.5 : 0.0) + (standing.getTeamId() % 7) * 0.45;
        double points = 106.0 + winPct * 18.0 + paceBoost;
        double rebounds = 39.0 + (1.0 - winPct) * 6.0 + (standing.getTeamId() % 5) * 0.6;
        double assists = 22.0 + winPct * 8.0 + (standing.getTeamId() % 4) * 0.5;
        double steals = 6.2 + winPct * 2.8 + (standing.getTeamId() % 3) * 0.35;
        double blocks = 4.0 + winPct * 2.4 + (standing.getTeamId() % 4) * 0.4;
        return new TeamMetric(standing.getTeamId(), points, rebounds, assists, steals, blocks);
    }

    private int rank(List<TeamMetric> metrics, MetricValue value, Integer teamId) {
        List<TeamMetric> sorted = metrics.stream()
                .sorted(Comparator.comparing(value::get).reversed())
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (Objects.equals(sorted.get(i).teamId(), teamId)) {
                return i + 1;
            }
        }
        return sorted.size();
    }

    private Standing fallbackStanding(Team team) {
        return Standing.builder()
                .season(currentSeason())
                .teamId(team.getId())
                .teamName(team.getFullName())
                .teamAbbr(team.getName())
                .conference(team.getConference())
                .division(team.getDivision())
                .wins(0)
                .losses(0)
                .winPercentage(0.0)
                .conferenceRank(0)
                .lastTenWins(0)
                .lastTenLosses(0)
                .winStreak(0)
                .build();
    }

    private int conferenceRank(Standing standing) {
        if (standing.getConferenceRank() != null && standing.getConferenceRank() > 0) {
            return standing.getConferenceRank();
        }
        List<Standing> conference = standingRepository.findBySeason(standing.getSeason()).stream()
                .filter(item -> Objects.equals(item.getConference(), standing.getConference()))
                .sorted(Comparator.comparing(Standing::getWins, Comparator.nullsLast(Integer::compareTo)).reversed())
                .toList();
        for (int i = 0; i < conference.size(); i++) {
            if (Objects.equals(conference.get(i).getTeamId(), standing.getTeamId())) {
                return i + 1;
            }
        }
        return 0;
    }

    private List<String> leaderNames(Team team) {
        Map<String, List<String>> leaders = Map.ofEntries(
                Map.entry("Hawks", List.of("Trae Young", "Jalen Johnson", "Trae Young", "Dyson Daniels", "Onyeka Okongwu")),
                Map.entry("Celtics", List.of("Jayson Tatum", "Jaylen Brown", "Derrick White", "Jrue Holiday", "Kristaps Porzingis")),
                Map.entry("Nets", List.of("Cam Thomas", "Nicolas Claxton", "Ben Simmons", "Dorian Finney-Smith", "Nicolas Claxton")),
                Map.entry("Hornets", List.of("LaMelo Ball", "Mark Williams", "LaMelo Ball", "Brandon Miller", "Mark Williams")),
                Map.entry("Bulls", List.of("Zach LaVine", "Nikola Vucevic", "Josh Giddey", "Ayo Dosunmu", "Patrick Williams")),
                Map.entry("Cavaliers", List.of("Donovan Mitchell", "Jarrett Allen", "Darius Garland", "Evan Mobley", "Evan Mobley")),
                Map.entry("Mavericks", List.of("Luka Doncic", "Dereck Lively II", "Kyrie Irving", "P.J. Washington", "Daniel Gafford")),
                Map.entry("Nuggets", List.of("Nikola Jokic", "Aaron Gordon", "Jamal Murray", "Kentavious Caldwell-Pope", "Michael Porter Jr.")),
                Map.entry("Pistons", List.of("Cade Cunningham", "Jalen Duren", "Cade Cunningham", "Ausar Thompson", "Jalen Duren")),
                Map.entry("Warriors", List.of("Stephen Curry", "Draymond Green", "Brandin Podziemski", "Gary Payton II", "Trayce Jackson-Davis")),
                Map.entry("Rockets", List.of("Jalen Green", "Alperen Sengun", "Fred VanVleet", "Amen Thompson", "Jabari Smith Jr.")),
                Map.entry("Pacers", List.of("Pascal Siakam", "Myles Turner", "Tyrese Haliburton", "Andrew Nembhard", "Myles Turner")),
                Map.entry("Clippers", List.of("Kawhi Leonard", "Ivica Zubac", "James Harden", "Terance Mann", "Ivica Zubac")),
                Map.entry("Lakers", List.of("LeBron James", "Anthony Davis", "Austin Reaves", "Jarred Vanderbilt", "Jaxson Hayes")),
                Map.entry("Grizzlies", List.of("Ja Morant", "Jaren Jackson Jr.", "Ja Morant", "Marcus Smart", "Jaren Jackson Jr.")),
                Map.entry("Heat", List.of("Jimmy Butler", "Bam Adebayo", "Tyler Herro", "Haywood Highsmith", "Kevin Love")),
                Map.entry("Bucks", List.of("Giannis Antetokounmpo", "Bobby Portis", "Damian Lillard", "Andre Jackson Jr.", "Brook Lopez")),
                Map.entry("Timberwolves", List.of("Anthony Edwards", "Rudy Gobert", "Mike Conley", "Jaden McDaniels", "Naz Reid")),
                Map.entry("Pelicans", List.of("Zion Williamson", "Jonas Valanciunas", "CJ McCollum", "Herb Jones", "Yves Missi")),
                Map.entry("Knicks", List.of("Jalen Brunson", "Karl-Anthony Towns", "Josh Hart", "Mikal Bridges", "Mitchell Robinson")),
                Map.entry("Thunder", List.of("Shai Gilgeous-Alexander", "Chet Holmgren", "Jalen Williams", "Alex Caruso", "Isaiah Hartenstein")),
                Map.entry("Magic", List.of("Paolo Banchero", "Wendell Carter Jr.", "Franz Wagner", "Jalen Suggs", "Jonathan Isaac")),
                Map.entry("76ers", List.of("Joel Embiid", "Joel Embiid", "Tyrese Maxey", "Kelly Oubre Jr.", "Joel Embiid")),
                Map.entry("Suns", List.of("Kevin Durant", "Jusuf Nurkic", "Devin Booker", "Josh Okogie", "Kevin Durant")),
                Map.entry("Trail Blazers", List.of("Anfernee Simons", "Deandre Ayton", "Scoot Henderson", "Matisse Thybulle", "Robert Williams III")),
                Map.entry("Kings", List.of("De'Aaron Fox", "Domantas Sabonis", "Domantas Sabonis", "Keon Ellis", "Keegan Murray")),
                Map.entry("Spurs", List.of("Victor Wembanyama", "Victor Wembanyama", "Chris Paul", "Devin Vassell", "Victor Wembanyama")),
                Map.entry("Raptors", List.of("Scottie Barnes", "Jakob Poeltl", "Immanuel Quickley", "Scottie Barnes", "Jakob Poeltl")),
                Map.entry("Jazz", List.of("Lauri Markkanen", "Walker Kessler", "Keyonte George", "Collin Sexton", "Walker Kessler")),
                Map.entry("Wizards", List.of("Jordan Poole", "Alex Sarr", "Bub Carrington", "Bilal Coulibaly", "Alex Sarr"))
        );
        return leaders.getOrDefault(team.getName(), List.of(
                team.getName() + " Scorer",
                team.getName() + " Rebounder",
                team.getName() + " Playmaker",
                team.getName() + " Stopper",
                team.getName() + " Rim Protector"
        ));
    }

    private String initials(String name) {
        return java.util.Arrays.stream(name.split("\\s+|-"))
                .filter(part -> !part.isBlank())
                .limit(2)
                .map(part -> part.substring(0, 1))
                .reduce("", String::concat)
                .toUpperCase();
    }

    private String identityFor(TeamMetric metric) {
        if (metric.steals() >= 8.0 || metric.blocks() >= 5.8) {
            return "defense-first";
        }
        if (metric.assists() >= 27.0) {
            return "ball-movement";
        }
        return "balanced";
    }

    private String conferenceName(String conference) {
        return "West".equals(conference) ? "Western Conference" : "Eastern Conference";
    }

    private String ordinal(int value) {
        if (value <= 0) {
            return "Unranked";
        }
        if (value % 100 >= 11 && value % 100 <= 13) {
            return value + "th";
        }
        return switch (value % 10) {
            case 1 -> value + "st";
            case 2 -> value + "nd";
            case 3 -> value + "rd";
            default -> value + "th";
        };
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private int currentSeason() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() >= 10 ? today.getYear() + 1 : today.getYear();
    }

    @FunctionalInterface
    private interface MetricValue {
        double get(TeamMetric metric);
    }

    private record TeamMetric(Integer teamId, double points, double rebounds, double assists, double steals, double blocks) {
    }
}
