import Link from "next/link";
import SeasonSelect from "@/components/team/SeasonSelect";
import { API_BASE_URL } from "@/lib/api";
import { TeamDashboard, TeamLeader, TeamStatCard } from "@/types/teamDashboard";

interface StatsPageProps {
  params: Promise<{ teamId: string }>;
  searchParams: Promise<{ season?: string }>;
}

const getDashboard = async (teamId: string, season?: string): Promise<TeamDashboard> => {
  const query = season ? `?season=${season}` : "";
  const response = await fetch(`${API_BASE_URL}/api/teams/${teamId}/dashboard${query}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to load team stats");
  return response.json();
};

const statTone = (stat: TeamStatCard) => {
  if (stat.highlighted) return "text-blue-400";
  if (stat.rank > 20) return "text-zinc-500";
  return "text-white";
};

export default async function TeamStatsPage({ params, searchParams }: StatsPageProps) {
  const { teamId } = await params;
  const { season } = await searchParams;
  const dashboard = await getDashboard(teamId, season);

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        <nav className="mb-10 flex items-center gap-7 border-b border-zinc-800 pb-1 text-sm font-bold uppercase tracking-[0.22em]">
          <Link href={`/teams/${teamId}?season=${dashboard.summary.selectedSeason}`} className="pb-3 text-zinc-500">Overview</Link>
          <Link href={`/teams/${teamId}/stats?season=${dashboard.summary.selectedSeason}`} className="border-b-2 border-blue-400 pb-3 text-blue-400">Stats</Link>
          <Link href={`/teams/${teamId}/roster?season=${dashboard.summary.selectedSeason}`} className="pb-3 text-zinc-500">Roster</Link>
          <Link href={`/teams/${teamId}?season=${dashboard.summary.selectedSeason}`} className="pb-3 text-zinc-500">History</Link>
        </nav>

        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.3em] text-blue-400">Team Stats</p>
            <h1 className="mt-3 text-4xl font-black uppercase tracking-normal text-white">{dashboard.summary.fullName}</h1>
          </div>
          <SeasonSelect seasons={dashboard.availableSeasons} selectedSeason={dashboard.summary.selectedSeason} />
        </header>

        <section className="mb-10 grid grid-cols-2 gap-4 md:grid-cols-5">
          {dashboard.stats.map((stat) => (
            <article key={stat.key} className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5">
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-zinc-500">{stat.label}</p>
              <p className={`text-3xl font-black italic ${statTone(stat)}`}>{stat.formattedValue}</p>
              <p className={`mt-3 text-xs font-bold ${stat.highlighted ? "text-blue-400" : "text-zinc-500"}`}>{stat.rankDisplay}</p>
            </article>
          ))}
        </section>

        <section>
          <h2 className="mb-6 text-xs font-black uppercase tracking-[0.28em] text-zinc-600">Team Leaders</h2>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {dashboard.leaders.map((leader) => (
              <LeaderCard key={leader.statKey} leader={leader} />
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}

const LeaderCard: React.FC<{ leader: TeamLeader }> = ({ leader }) => (
  <article className="grid min-h-40 grid-cols-[64px_1fr] items-center gap-5 rounded-2xl border border-zinc-800 bg-gradient-to-br from-zinc-800/40 to-zinc-950 p-6">
    <div className="flex size-16 shrink-0 items-center justify-center rounded-full border-2 border-blue-500/20 bg-zinc-100 text-lg font-black text-zinc-400">
      {leader.initials}
    </div>
    <div className="min-w-0">
      <p className="text-xs font-black uppercase tracking-widest text-blue-400">{leader.statLabel}</p>
      <p className="mt-1 text-xl font-bold leading-tight text-white">{leader.playerName}</p>
      <p className="mt-3 text-3xl font-black italic leading-none text-white">
        {leader.formattedValue}
        <span className="ml-2 text-xs font-bold uppercase not-italic text-zinc-500">{leader.unit}</span>
      </p>
    </div>
  </article>
);
