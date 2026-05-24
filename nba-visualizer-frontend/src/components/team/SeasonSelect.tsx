"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";

interface SeasonSelectProps {
  seasons: number[];
  selectedSeason: number;
}

const SeasonSelect: React.FC<SeasonSelectProps> = ({ seasons, selectedSeason }) => {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [pending, startTransition] = useTransition();

  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-black uppercase tracking-[0.24em] text-zinc-600">Season</span>
      <select
        key={selectedSeason}
        defaultValue={selectedSeason}
        disabled={pending}
        onChange={(event) => {
          const params = new URLSearchParams(searchParams.toString());
          params.set("season", event.target.value);
          startTransition(() => {
            router.replace(`${pathname}?${params.toString()}`);
            router.refresh();
          });
        }}
        className="h-11 rounded-xl border border-zinc-800 bg-zinc-900 px-4 text-sm font-bold text-zinc-100 outline-none transition focus:border-blue-500"
      >
        {seasons.map((season) => (
          <option key={season} value={season}>
            {season - 1}-{String(season).slice(-2)}
          </option>
        ))}
      </select>
    </label>
  );
};

export default SeasonSelect;
