import Link from "next/link";
import { API_BASE_URL } from "@/lib/api";
import SeasonSelect from "@/components/team/SeasonSelect";
import { TeamDashboard, TeamDepthChartPlayer } from "@/types/teamDashboard";

interface RosterPageProps {
  params: Promise<{ teamId: string }>;
  searchParams: Promise<{ season?: string }>;
}

const getRosterDashboard = async (teamId: string, season?: string): Promise<TeamDashboard> => {
  const query = season ? `?season=${season}` : "";
  const response = await fetch(`${API_BASE_URL}/api/teams/${teamId}/roster${query}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to load roster");
  return response.json();
};

export default async function TeamRosterPage({ params, searchParams }: RosterPageProps) {
  const { teamId } = await params;
  const { season } = await searchParams;
  const dashboard = await getRosterDashboard(teamId, season);

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        <Link href={`/teams/${teamId}${season ? `?season=${season}` : ""}`} className="mb-8 inline-flex text-sm font-semibold text-zinc-500 transition hover:text-blue-400">
          Back to team overview
        </Link>
        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.3em] text-blue-400">Roster</p>
            <h1 className="mt-3 text-4xl font-black uppercase tracking-normal text-white">{dashboard.summary.fullName}</h1>
          </div>
          <SeasonSelect seasons={dashboard.availableSeasons} selectedSeason={dashboard.summary.selectedSeason} />
        </header>

        <DepthChartTable dashboard={dashboard} />

        <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
          <div className="border-b border-zinc-800 bg-zinc-800/40 px-6 py-4">
            <h2 className="text-sm font-black uppercase tracking-[0.24em] text-zinc-400">Full Team</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-[0.2em] text-zinc-600">
                <tr>
                  <th className="px-6 py-4">#</th>
                  <th className="px-6 py-4">Player</th>
                  <th className="px-6 py-4">Pos</th>
                  <th className="px-6 py-4">Height</th>
                  <th className="px-6 py-4">Weight</th>
                  <th className="px-6 py-4 text-right">Salary</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60">
                {dashboard.roster.map((player) => (
                  <tr key={player.playerId}>
                    <td className="px-6 py-4 font-bold text-zinc-500">{player.jersey || "-"}</td>
                    <td className="px-6 py-4 font-bold text-white">{player.fullName}</td>
                    <td className="px-6 py-4 text-zinc-400">{player.position || "-"}</td>
                    <td className="px-6 py-4 text-zinc-400">{player.height || "-"}</td>
                    <td className="px-6 py-4 text-zinc-400">{player.weight || "-"}</td>
                    <td className="px-6 py-4 text-right font-semibold text-zinc-300">{player.salary || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </main>
  );
}

const depthColumns = ["Starter", "2nd", "3rd", "4th", "5th"];

const DepthChartTable: React.FC<{ dashboard: TeamDashboard }> = ({ dashboard }) => (
  <section className="mb-10 overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
    <div className="border-b border-zinc-800 bg-zinc-800/40 px-6 py-4">
      <h2 className="text-sm font-black uppercase tracking-[0.24em] text-zinc-400">Depth Chart</h2>
    </div>
    {dashboard.depthChart.length === 0 ? (
      <p className="px-6 py-8 text-sm font-semibold text-zinc-500">No depth chart available yet.</p>
    ) : (
      <div className="overflow-x-auto">
        <table className="w-full min-w-[760px] border-collapse text-left text-sm">
          <thead className="text-xs uppercase tracking-[0.18em] text-zinc-500">
            <tr className="border-b border-zinc-800">
              <th className="w-16 px-5 py-3">Pos</th>
              {depthColumns.map((column) => (
                <th key={column} className="px-5 py-3">{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {dashboard.depthChart.map((group) => (
              <tr key={group.position} className="border-b border-zinc-800/70 last:border-b-0 odd:bg-zinc-950/30">
                <td className="px-5 py-4 font-black text-zinc-500">{group.position}</td>
                {depthColumns.map((column, index) => (
                  <td key={`${group.position}-${column}`} className="px-5 py-4">
                    <DepthPlayer player={group.players.find((player) => player.depthOrder === index + 1)} />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </section>
);

const DepthPlayer: React.FC<{ player?: TeamDepthChartPlayer }> = ({ player }) => {
  if (!player) return <span className="text-zinc-700">-</span>;
  return (
    <span className="font-bold text-blue-400">
      {player.playerName}
      {player.status && <span className="ml-1 text-red-400">{player.status}</span>}
    </span>
  );
};
