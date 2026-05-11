import Link from "next/link";
import { TeamDashboard, TeamInjuryReportItem, TeamLeader, TeamScheduleGame, TeamStatCard } from "@/types/teamDashboard";

interface TeamDashboardPageProps {
  dashboard: TeamDashboard;
}

const tabs = ["Overview", "Stats", "Schedule", "History"];

const statTone = (stat: TeamStatCard) => {
  if (stat.highlighted) return "text-blue-400";
  if (stat.rank > 20) return "text-zinc-500";
  return "text-white";
};

const resultTone = (resultType: TeamScheduleGame["resultType"]) => {
  if (resultType === "win") return "text-green-400";
  if (resultType === "loss") return "text-red-400";
  return "text-zinc-100";
};

const TeamDashboardPage: React.FC<TeamDashboardPageProps> = ({ dashboard }) => {
  const { summary, stats, leaders, schedule, injuries } = dashboard;

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        <nav className="mb-10 flex items-center gap-7 overflow-x-auto border-b border-zinc-800 pb-1 text-sm font-bold uppercase tracking-[0.22em]">
          {tabs.map((tab, index) => (
            <span
              key={tab}
              className={`shrink-0 pb-3 ${index === 0 ? "border-b-2 border-blue-400 text-blue-400" : "text-zinc-500"}`}
            >
              {tab}
            </span>
          ))}
        </nav>

        <Link href="/" className="mb-8 inline-flex text-sm font-semibold text-zinc-500 transition hover:text-blue-400">
          Back to standings
        </Link>

        <section className="grid gap-8 lg:grid-cols-[1fr_360px]">
          <div className="min-w-0">
            <header className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-center">
              <div className="flex size-32 shrink-0 items-center justify-center rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5">
                <img
                  src={summary.logoPath}
                  alt={summary.fullName}
                  className="h-full w-full object-contain"
                />
              </div>
              <div>
                <p className="mb-2 text-xs font-black uppercase tracking-[0.3em] text-zinc-600">{summary.division}</p>
                <h1 className="max-w-4xl text-4xl font-black uppercase leading-none tracking-normal text-white sm:text-5xl lg:text-6xl">
                  {summary.fullName}
                </h1>
                <p className="mt-3 text-xl font-semibold italic text-zinc-500">
                  {summary.recordDisplay} • {summary.conferenceRankDisplay}
                </p>
              </div>
            </header>

            <p className="mb-10 max-w-4xl text-lg leading-8 text-zinc-400">
              {summary.summary}
            </p>

            <section className="mb-12 grid grid-cols-2 gap-4 md:grid-cols-5">
              {stats.map((stat) => (
                <article
                  key={stat.key}
                  className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5 transition hover:border-zinc-700"
                >
                  <p className="mb-2 text-xs font-black uppercase tracking-widest text-zinc-500">{stat.label}</p>
                  <p className={`text-3xl font-black italic ${statTone(stat)}`}>{stat.formattedValue}</p>
                  <p className={`mt-3 text-xs font-bold ${stat.highlighted ? "text-blue-400" : "text-zinc-500"}`}>
                    {stat.rankDisplay}
                  </p>
                </article>
              ))}
            </section>

            <section>
              <h2 className="mb-6 text-xs font-black uppercase tracking-[0.28em] text-zinc-600">Team Leaders</h2>
              <div className="flex gap-4 overflow-x-auto pb-4">
                {leaders.map((leader) => (
                  <LeaderCard key={leader.statKey} leader={leader} />
                ))}
              </div>
            </section>
          </div>

          <aside className="space-y-6">
            <SchedulePanel schedule={schedule} />
            <InjuryPanel injuries={injuries} />
          </aside>
        </section>
      </div>
    </main>
  );
};

const LeaderCard: React.FC<{ leader: TeamLeader }> = ({ leader }) => (
  <article className="flex w-72 shrink-0 items-center gap-5 rounded-2xl border border-zinc-800 bg-gradient-to-br from-zinc-800/40 to-zinc-950 p-6 transition hover:border-blue-500/30">
    <div className="flex size-16 shrink-0 items-center justify-center rounded-full border-2 border-blue-500/20 bg-zinc-100 text-lg font-black text-zinc-400">
      {leader.initials}
    </div>
    <div className="min-w-0">
      <p className="text-xs font-black uppercase tracking-widest text-blue-400">{leader.statLabel}</p>
      <p className="mt-1 line-clamp-2 text-xl font-bold leading-tight text-white">{leader.playerName}</p>
      <p className="mt-3 text-3xl font-black italic leading-none text-white">
        {leader.formattedValue}
        <span className="ml-2 text-xs font-bold uppercase not-italic text-zinc-500">{leader.unit}</span>
      </p>
    </div>
  </article>
);

const SchedulePanel: React.FC<{ schedule: TeamScheduleGame[] }> = ({ schedule }) => (
  <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
    <div className="flex items-center justify-between border-b border-zinc-800 bg-zinc-800/40 px-5 py-4">
      <h2 className="text-xs font-black uppercase tracking-[0.24em] text-zinc-400">Schedule & Results</h2>
      <span className="text-xs font-bold uppercase text-blue-400">Full Schedule</span>
    </div>
    <div className="divide-y divide-zinc-800/60">
      {schedule.map((game) => (
        <div key={`${game.date}-${game.opponentAbbreviation}`} className="grid grid-cols-[80px_1fr_auto] items-center gap-3 px-5 py-4 text-sm">
          <span className="font-semibold text-zinc-500">{game.date}</span>
          <span className="min-w-0 font-bold text-zinc-100">
            <span className="mr-2 text-zinc-600">{game.location}</span>
            {game.opponentAbbreviation}
          </span>
          <span className={`text-right font-black ${resultTone(game.resultType)}`}>{game.result}</span>
        </div>
      ))}
    </div>
  </section>
);

const InjuryPanel: React.FC<{ injuries: TeamInjuryReportItem[] }> = ({ injuries }) => (
  <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
    <div className="border-b border-zinc-800 bg-zinc-800/40 px-5 py-4">
      <h2 className="text-xs font-black uppercase tracking-[0.24em] text-zinc-400">Current Injury Report</h2>
    </div>
    {injuries.length === 0 ? (
      <p className="px-5 py-8 text-sm font-semibold text-zinc-500">No current injuries reported.</p>
    ) : (
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="border-b border-zinc-800/60 text-zinc-600">
            <tr>
              <th className="px-5 py-3 uppercase">Player</th>
              <th className="px-5 py-3 uppercase">Injury</th>
              <th className="px-5 py-3 text-right uppercase">Return</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/40">
            {injuries.map((injury) => (
              <tr key={`${injury.playerName}-${injury.injury}`}>
                <td className="px-5 py-4 font-bold text-white">{injury.playerName}</td>
                <td className="px-5 py-4 text-zinc-400">{injury.injury}</td>
                <td className="px-5 py-4 text-right font-semibold text-orange-400">{injury.expectedReturn}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </section>
);

export default TeamDashboardPage;
