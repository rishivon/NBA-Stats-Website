import React, { useMemo, useState } from "react";

export interface Standing {
  teamId: number;
  teamName: string;
  teamAbbr: string;
  conference: string;
  wins: number;
  losses: number;
  winPercentage: number;
  lastTenWins: number;
  lastTenLosses: number;
  winStreak: number;
}

interface StandingsTableProps {
  standings: Standing[];
  loading: boolean;
  error: string;
  season: number;
  onSeasonChange: (season: number) => void;
}

const StandingsTable: React.FC<StandingsTableProps> = ({ standings, loading, error, season, onSeasonChange }) => {
  const [activeConference, setActiveConference] = useState<"East" | "West">("East");
  const currentSeason = (() => {
    const now = new Date();
    const month = now.getMonth() + 1;
    const year = now.getFullYear();
    return month >= 10 ? year + 1 : year;
  })();
  const seasons = Array.from({ length: currentSeason - 2015 + 1 }, (_, index) => currentSeason - index);

  const getTeamLogoUrl = (teamAbbr: string) => {
    return `/logos/${teamAbbr.toLowerCase()}.svg`;
  };

  const filteredStandings = useMemo(
    () => standings.filter((standing) => standing.conference === activeConference),
    [standings, activeConference]
  );

  const getStreakColor = (streak: number) => {
    if (streak > 0) return "text-green-500";
    if (streak < 0) return "text-red-500";
    return "text-zinc-400";
  };

  const getStreakDisplay = (streak: number) => {
    const prefix = streak >= 0 ? "W" : "L";
    return `${prefix}${Math.abs(streak)}`;
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-2xl font-semibold text-white">NBA Standings</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-6">
          <div className="flex flex-col gap-1">
            <span className="text-xs uppercase tracking-[0.3em] text-zinc-500 dark:text-zinc-400">Season</span>
            <select
              value={season}
              onChange={(e) => onSeasonChange(Number(e.target.value))}
              className="h-11 px-4 rounded-lg bg-zinc-200 text-zinc-900 dark:bg-zinc-800 dark:text-white text-sm font-medium border border-zinc-300 dark:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {seasons.map((s) => (
                <option key={s} value={s}>
                  {s - 1}-{s.toString().slice(-2)}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-xs uppercase tracking-[0.3em] text-zinc-500 dark:text-zinc-400">Conference</span>
            <div className="inline-flex h-11 rounded-full bg-zinc-200 p-1 dark:bg-zinc-800">
              {(["East", "West"] as const).map((conference) => (
                <button
                  key={conference}
                  className={`inline-flex h-9 items-center rounded-full px-4 text-sm font-medium transition ${
                    activeConference === conference
                      ? "bg-black text-white dark:bg-white dark:text-black"
                      : "text-zinc-600 hover:bg-zinc-300 dark:text-zinc-300 dark:hover:bg-zinc-700"
                  }`}
                  onClick={() => setActiveConference(conference)}
                >
                  {conference}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {loading && <p>Loading standings...</p>}
      {error && <p className="text-red-500">{error}</p>}

      {!loading && !error && (
        <div className="overflow-x-auto rounded-3xl border border-zinc-200 bg-white shadow-sm dark:border-zinc-700 dark:bg-zinc-950">
          <table className="w-full border-collapse">
            <thead className="bg-zinc-100 text-left text-xs uppercase tracking-[0.2em] text-zinc-600 dark:bg-zinc-900 dark:text-zinc-400">
              <tr>
                <th className="px-4 py-3">Team</th>
                <th className="px-4 py-3 text-right">W</th>
                <th className="px-4 py-3 text-right">L</th>
                <th className="px-4 py-3 text-right">Pct</th>
                <th className="px-4 py-3 text-right">L10</th>
                <th className="px-4 py-3 text-right">Strk</th>
              </tr>
            </thead>
            <tbody>
              {filteredStandings.map((standing) => (
                <tr key={standing.teamId} className="border-t border-zinc-200 dark:border-zinc-800">
                  <td className="px-4 py-4 text-sm font-medium text-zinc-900 dark:text-zinc-100">
                    <div className="flex items-center gap-3">
                      <img
                        src={getTeamLogoUrl(standing.teamAbbr)}
                        alt={standing.teamName}
                        className="w-6 h-6"
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = 'none';
                        }}
                      />
                      {standing.teamName}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-right text-sm text-zinc-700 dark:text-zinc-300">
                    {standing.wins}
                  </td>
                  <td className="px-4 py-4 text-right text-sm text-zinc-700 dark:text-zinc-300">
                    {standing.losses}
                  </td>
                  <td className="px-4 py-4 text-right text-sm text-zinc-700 dark:text-zinc-300">
                    .{Math.round(standing.winPercentage * 1000)}
                  </td>
                  <td className="px-4 py-4 text-right text-sm text-zinc-700 dark:text-zinc-300">
                    {standing.lastTenWins}-{standing.lastTenLosses}
                  </td>
                  <td className={`px-4 py-4 text-right text-sm font-medium ${getStreakColor(standing.winStreak)}`}>
                    {getStreakDisplay(standing.winStreak)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default StandingsTable;
