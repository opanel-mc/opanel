package net.opanel.event;

import net.opanel.api.event.PlayerGameModeChangeEvent;
import net.opanel.api.player.PlayerAPI;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import net.opanel.extension.api.ExtensionAPI;

public class OPanelPlayerGameModeChangeEvent extends OPanelEvent {
    private final OPanelPlayer player;
    private final OPanelGameMode gamemode;

    public OPanelPlayerGameModeChangeEvent(OPanelPlayer player, OPanelGameMode gamemode) {
        this.player = player;
        this.gamemode = gamemode;
    }

    public OPanelPlayer getPlayer() {
        return player;
    }

    public OPanelGameMode getGameMode() {
        return gamemode;
    }

    @Override
    public PlayerGameModeChangeEvent toAPIEvent(ExtensionAPI api) {
        PlayerAPI playerHandle = api.createPlayerHandle(player.getUUID());
        return new PlayerGameModeChangeEvent(playerHandle, playerHandle.getGameMode());
    }
}
