package net.opanel.common;

import java.io.IOException;
import java.nio.file.Path;

public interface OPanelSave {
    String getName();
    String getDisplayName();
    Path getPath();
    boolean isCurrent();
    OPanelGameMode getDefaultGameMode();
    void delete() throws IOException;
}
