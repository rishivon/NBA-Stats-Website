import TeamDashboardPage from "@/components/team/TeamDashboardPage";
import { API_BASE_URL } from "@/lib/api";
import { TeamDashboard } from "@/types/teamDashboard";
import { notFound } from "next/navigation";

interface TeamPageProps {
  params: Promise<{
    teamId: string;
  }>;
  searchParams: Promise<{
    season?: string;
  }>;
}

const getTeamDashboard = async (teamId: string, season?: string): Promise<TeamDashboard> => {
  const query = season ? `?season=${season}` : "";
  const response = await fetch(`${API_BASE_URL}/api/teams/${teamId}/dashboard${query}`, {
    cache: "no-store",
  });

  if (response.status === 404) {
    notFound();
  }

  if (!response.ok) {
    throw new Error("Failed to load team dashboard");
  }

  return response.json();
};

export default async function TeamPage({ params, searchParams }: TeamPageProps) {
  const { teamId } = await params;
  const { season } = await searchParams;
  const dashboard = await getTeamDashboard(teamId, season);

  return <TeamDashboardPage dashboard={dashboard} />;
}
