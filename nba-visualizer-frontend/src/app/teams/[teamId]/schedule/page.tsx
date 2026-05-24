import Link from "next/link";
import SeasonSelect from "@/components/team/SeasonSelect";
import { API_BASE_URL } from "@/lib/api";
import { TeamScheduleGame } from "@/types/teamDashboard";

interface SchedulePageProps {
  params: Promise<{ teamId: string }>;
  searchParams: Promise<{ season?: string }>;
}

const resultTone = (type: TeamScheduleGame["resultType"]) => {
  if (type === "win") return "text-green-400";
  if (type === "loss") return "text-red-400";
  return "text-zinc-100";
};

const getSchedule = async (teamId: string, season?: string): Promise<TeamScheduleGame[]> => {
  const query = season ? `?season=${season}` : "";
  const response = await fetch(`${API_BASE_URL}/api/teams/${teamId}/schedule${query}`, { cache: "no-store" });
  if (!response.ok) throw new Error("Failed to load schedule");
  return response.json();
};

const currentSeason = () => {
  const today = new Date();
  return today.getMonth() >= 9 ? today.getFullYear() + 1 : today.getFullYear();
};

const availableSeasons = (selectedSeason: number) => {
  const current = Math.max(currentSeason(), selectedSeason);
  return Array.from({ length: current - 2016 + 1 }, (_, index) => current - index);
};

export default async function TeamSchedulePage({ params, searchParams }: SchedulePageProps) {
  const { teamId } = await params;
  const { season } = await searchParams;
  const selectedSeason = season ? Number(season) : currentSeason();
  const schedule = await getSchedule(teamId, season);
  const upcoming = schedule.filter((game) => !game.completed);
  const recent = schedule.filter((game) => game.completed);

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-10">
        <Link href={`/teams/${teamId}${season ? `?season=${season}` : ""}`} className="mb-8 inline-flex text-sm font-semibold text-zinc-500 transition hover:text-blue-400">
          Back to team overview
        </Link>
        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.3em] text-blue-400">Schedule</p>
            <h1 className="mt-3 text-4xl font-black uppercase tracking-normal text-white">Full Schedule</h1>
          </div>
          <SeasonSelect seasons={availableSeasons(selectedSeason)} selectedSeason={selectedSeason} />
        </header>

        <ScheduleSection title="Upcoming Games" games={upcoming} />
        <ScheduleSection title="Recent Results" games={recent} />
      </div>
    </main>
  );
}

const ScheduleSection: React.FC<{ title: string; games: TeamScheduleGame[] }> = ({ title, games }) => (
  <section className="mb-10 overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
    <div className="border-b border-zinc-800 bg-zinc-800/40 px-6 py-4">
      <h2 className="text-sm font-black uppercase tracking-[0.24em] text-zinc-400">{title}</h2>
    </div>
    {games.length === 0 ? (
      <p className="px-6 py-8 text-sm font-semibold text-zinc-500">No games available.</p>
    ) : (
      <div className="divide-y divide-zinc-800/60">
        {games.map((game) => (
          <div key={`${game.gameDate}-${game.opponentAbbreviation}-${game.location}`} className="grid gap-4 px-6 py-5 text-base sm:grid-cols-[120px_70px_1fr_160px_90px] sm:items-center">
            <span className="font-black text-blue-400">{game.date}</span>
            <span className="text-center font-black text-white">{game.location}</span>
            <span className="font-black uppercase text-blue-400">
              {game.opponentAbbreviation || game.opponentName}
            </span>
            <span className={`font-black ${resultTone(game.resultType)}`}>{game.result || "TBD"}</span>
            <span className="font-semibold text-zinc-500">{game.record || "-"}</span>
          </div>
        ))}
      </div>
    )}
  </section>
);
