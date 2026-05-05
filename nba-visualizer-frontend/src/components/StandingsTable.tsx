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
}

const StandingsTable: React.FC<StandingsTableProps> = ({ standings, loading, error }) => {
  const [activeConference, setActiveConference] = useState<"East" | "West">("East");

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
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-zinc-500 dark:text-zinc-400">
            2025-26 {activeConference} Conference
          </p>
          <p className="text-sm text-zinc-600 dark:text-zinc-400">
            Regular season standings from BallDontLie.
          </p>
        </div>
        <div className="inline-flex rounded-full bg-zinc-200 p-1 dark:bg-zinc-800">
          {(["East", "West"] as const).map((conference) => (
            <button
              key={conference}
              className={`rounded-full px-4 py-2 text-sm font-medium transition ${
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
                    {standing.teamName}
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
