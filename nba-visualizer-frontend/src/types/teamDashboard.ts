export interface TeamSummary {
  id: number;
  fullName: string;
  abbreviation: string;
  city: string;
  name: string;
  conference: string;
  division: string;
  logoPath: string;
  season: number;
  wins: number;
  losses: number;
  winPercentage: number;
  conferenceRank: number;
  conferenceRankDisplay: string;
  recordDisplay: string;
  summary: string;
  selectedSeason: number;
}

export interface TeamStatCard {
  key: string;
  label: string;
  value: number;
  formattedValue: string;
  rank: number;
  rankDisplay: string;
  highlighted: boolean;
}

export interface TeamLeader {
  statKey: string;
  statLabel: string;
  playerName: string;
  initials: string;
  value: number;
  formattedValue: string;
  unit: string;
}

export interface TeamScheduleGame {
  date: string;
  gameDate: string;
  opponentAbbreviation: string;
  opponentName: string;
  location: "@" | "vs";
  result: string;
  resultType: "win" | "loss" | "upcoming";
  completed: boolean;
  record: string;
}

export interface TeamInjuryReportItem {
  playerName: string;
  position: string | null;
  injury: string;
  expectedReturn: string;
  status: string;
}

export interface TeamDashboard {
  summary: TeamSummary;
  stats: TeamStatCard[];
  leaders: TeamLeader[];
  schedule: TeamScheduleGame[];
  injuries: TeamInjuryReportItem[];
  roster: TeamRosterPlayer[];
  depthChart: TeamDepthChartGroup[];
  availableSeasons: number[];
}

export interface TeamRosterPlayer {
  playerId: number;
  fullName: string;
  position: string;
  jersey: string;
  height: string;
  weight: string;
  salary: string | null;
}

export interface TeamDepthChartPlayer {
  playerId: number;
  playerName: string;
  depthOrder: number;
  status: string | null;
}

export interface TeamDepthChartGroup {
  position: string;
  players: TeamDepthChartPlayer[];
}
