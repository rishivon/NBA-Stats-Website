import React from "react";

interface StandingsTableProps {
  teams: string[];
  loading: boolean;
  error: string;
}

const StandingsTable: React.FC<StandingsTableProps> = ({ teams, loading, error }) => (
  <section>
    <h2 className="text-3xl font-bold text-zinc-900 dark:text-zinc-100 mb-6">NBA Standings</h2>
    {loading && <p>Loading...</p>}
    {error && <p className="text-red-500">{error}</p>}
    {!loading && !error && (
      <table className="min-w-full border border-zinc-700 rounded">
        <thead>
          <tr>
            <th className="border-b border-zinc-700 px-4 py-2 text-left">Team Name</th>
          </tr>
        </thead>
        <tbody>
          {teams.map((team) => (
            <tr key={team}>
              <td className="border-b border-zinc-700 px-4 py-2">{team}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )}
  </section>
);

export default StandingsTable;
