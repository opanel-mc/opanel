package net.opanel.bukkit_1_21_5;

import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelSave;

import java.io.IOException;
import java.nio.file.Path;

/** @todo */
public class BukkitSave implements OPanelSave {
    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public void setDisplayName(String displayName) throws IOException {

    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public boolean isCurrent() {
        return false;
    }

    @Override
    public OPanelGameMode getDefaultGameMode() {
        return null;
    }

    @Override
    public void setDefaultGameMode(OPanelGameMode gamemode) throws IOException {

    }

    @Override
    public void delete() throws IOException {

    }
}
