package net.opanel.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public interface OPanelSave {
    String getName();
    String getDisplayName();
    void setDisplayName(String displayName) throws IOException;
    Path getPath();
    long getSize() throws IOException;
    boolean isRunning();
    boolean isCurrent() throws IOException;
    void setToCurrent() throws IOException;
    OPanelGameMode getDefaultGameMode();
    void setDefaultGameMode(OPanelGameMode gamemode) throws IOException;
    OPanelDifficulty getDifficulty() throws IOException;
    void setDifficulty(OPanelDifficulty difficulty) throws IOException;
    boolean isDifficultyLocked() throws IOException;
    void setDifficultyLocked(boolean locked) throws IOException;
    boolean isHardcore() throws IOException;
    void setHardcoreEnabled(boolean enabled) throws IOException;
    HashMap<String, Boolean> getDatapacks();
    void toggleDatapack(String id, boolean enabled) throws IOException;
    void delete() throws IOException;
    List<OPanelWorldRegion> getRegions();
    default List<OPanelWorldRegion> getRegions(Path regionFolder) { return List.of(); }
}
