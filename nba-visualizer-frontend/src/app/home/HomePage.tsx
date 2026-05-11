"use client";

import React, { useEffect, useState } from "react";
import StandingsTable, { Standing } from "@/components/StandingsTable";
import SectionCard from "@/components/SectionCard";
import { API_BASE_URL } from "@/lib/api";

const getCurrentSeason = (): number => {
  const now = new Date();
  const month = now.getMonth() + 1;
  const year = now.getFullYear();
  return month >= 10 ? year + 1 : year;
};

const HomePage: React.FC = () => {
  const [standings, setStandings] = useState<Standing[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [season, setSeason] = useState<number>(() => getCurrentSeason());

  useEffect(() => {
    let active = true;

    const loadStandings = async () => {
      setLoading(true);
      setError("");

      try {
        const res = await fetch(`${API_BASE_URL}/api/standings?season=${season}`);
        if (!res.ok) throw new Error("Failed to fetch standings");
        const data = await res.json();
        if (active) setStandings(data);
      } catch (err) {
        if (active) setError(err instanceof Error ? err.message : "Failed to fetch standings");
      } finally {
        if (active) setLoading(false);
      }
    };

    void loadStandings();

    return () => {
      active = false;
    };
  }, [season]);

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-50">
      <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
        <header className="mb-10 rounded-3xl border border-zinc-800 bg-zinc-900/80 p-6 shadow-sm backdrop-blur">
          <p className="text-sm uppercase tracking-[0.4em] text-zinc-500">NBA Dashboard</p>
          <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">NBA Data Visualizer</h1>
          <p className="mt-3 max-w-2xl text-sm text-zinc-400 sm:text-base">
            Live-ready sections for standings, leaders, live games, and more. Use this layout as the foundation for your dashboard cards.
          </p>
        </header>

        <div className="grid gap-6 xl:grid-cols-[1.7fr_1fr]">
          <div className="space-y-4">
            <StandingsTable
              standings={standings}
              loading={loading}
              error={error}
              season={season}
              onSeasonChange={setSeason}
            />
          </div>

          <div className="grid gap-6">
            <SectionCard title="Trending Performance" subtitle="Top players and hot streaks.">
              <div className="space-y-3 text-sm text-zinc-400">
                <p className="text-zinc-100">This section will highlight player trends, recent performances, and storylines.</p>
                <ul className="space-y-2 list-disc pl-5 text-zinc-400">
                  <li>Top scorers</li>
                  <li>Recent stat spikes</li>
                  <li>Trend insights</li>
                </ul>
              </div>
            </SectionCard>
            <SectionCard title="Live Schedule" subtitle="Upcoming games and scores.">
              <div className="space-y-3 text-sm text-zinc-400">
                <p className="text-zinc-100">This area can show today&apos;s games, live scores, and next matchups.</p>
                <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
                  <p className="text-sm text-zinc-400">Add a quick summary card for live events or featured matchups here.</p>
                </div>
              </div>
            </SectionCard>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
