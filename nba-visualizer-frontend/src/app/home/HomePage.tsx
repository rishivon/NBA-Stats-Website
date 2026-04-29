"use client";

import React, { useEffect, useState } from "react";
import StandingsTable from "@/components/StandingsTable";

const HomePage: React.FC = () => {
  const [teams, setTeams] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("http://localhost:8080/api/standings/teams")
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch teams");
        return res.json();
      })
      .then((data) => setTeams(data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-zinc-50 font-sans dark:bg-black">
      <main className="w-full max-w-3xl py-16 px-4 bg-white dark:bg-black">
        <StandingsTable teams={teams} loading={loading} error={error} />
        {/* Future homepage sections/components go here */}
      </main>
    </div>
  );
};

export default HomePage;
