import TeamDashboardPage from "@/components/team/TeamDashboardPage";
import { API_BASE_URL } from "@/lib/api";
import { TeamDashboard } from "@/types/teamDashboard";
import { notFound } from "next/navigation";

interface TeamPageProps {
  params: Promise<{
    teamId: string;
  }>;
}

const getTeamDashboard = async (teamId: string): Promise<TeamDashboard> => {
  const response = await fetch(`${API_BASE_URL}/api/teams/${teamId}/dashboard`, {
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

export default async function TeamPage({ params }: TeamPageProps) {
  const { teamId } = await params;
  const dashboard = await getTeamDashboard(teamId);

  return <TeamDashboardPage dashboard={dashboard} />;
}
