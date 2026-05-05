import React from "react";

interface SectionCardProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}

const SectionCard: React.FC<SectionCardProps> = ({ title, subtitle, children }) => (
  <section className="rounded-3xl border border-zinc-200 bg-white p-6 shadow-sm transition-shadow duration-200 hover:shadow-md dark:border-zinc-800 dark:bg-zinc-950">
    <div className="mb-6 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-xs uppercase tracking-[0.4em] text-zinc-500 dark:text-zinc-400">
          {title}
        </p>
        {subtitle && <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-300">{subtitle}</p>}
      </div>
    </div>
    {children}
  </section>
);

export default SectionCard;
