package net.opanel.common;

import java.io.IOException;
import java.nio.file.Path;

public interface OPanelSave {
    String getName();
    String getDisplayName();
    void setDisplayName(String displayName) throws IOException;
    Path getPath();
    boolean isCurrent();
    OPanelGameMode getDefaultGameMode();
    void setDefaultGameMode(OPanelGameMode gamemode) throws IOException;
    void delete() throws IOException;
}
