import { useContext, useEffect, useState } from "react";
import { coerce, compare } from "semver";
import { getRandomProjects, type ModrinthProject } from "@/lib/plugin-store/modrinth";
import { ProjectCard } from "./project-card";
import { VersionContext } from "@/contexts/api-context";
import { sendPostRequest } from "@/lib/api";

export function ModrinthStore() {
  const versionCtx = useContext(VersionContext);
  const [projects, setProjects] = useState<ModrinthProject[]>([]);

  const fetchProjectList = async () => {
    if(!versionCtx) return;

    const fetchedList = await getRandomProjects(100);
    const filteredList = fetchedList.filter((project) => {
      const currentVersion = coerce(versionCtx.version);
      const projectLowestVersion = coerce(project.game_versions[0]);

      return (
        project.project_type === "mod"
        && (currentVersion && projectLowestVersion)
        && compare(currentVersion, projectLowestVersion) >= 0
        && project.loaders.includes(versionCtx.serverType.toLowerCase() as any)
      );
    });

    setProjects(filteredList);
  };

  const handleInstall = async (projectId: string) => {
    try {
      await sendPostRequest(`/api/plugins/install?source=modrinth&id=${projectId}`);
    } catch (e: any) {
      
    }
  };

  useEffect(() => {
    if(!versionCtx) return;

    fetchProjectList();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [versionCtx]);

  if(!versionCtx) return <></>;

  return (
    <>
      {projects.map((project, i) => (
        <ProjectCard
          id={project.id}
          title={project.title}
          description={project.description}
          detailedUrl={`https://modrinth.com/project/${project.slug}`}
          sourceUrl={project.source_url}
          docsUrl={project.wiki_url}
          donationUrl={project.donation_urls.length > 0 ? project.donation_urls[0].url : undefined}
          iconUrl={project.icon_url}
          loaders={project.loaders}
          downloads={project.downloads}
          readme={project.body}
          onInstall={() => handleInstall(project.id)}
          key={i}/>
      ))}
    </>
  );
}
