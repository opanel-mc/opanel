package net.opanel.event;

import cn.opanel.api.event.PlayerGameModeChangeEvent;
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
        return new PlayerGameModeChangeEvent(
            api.createPlayerHandle(player.getUUID()),
            gamemode.toAPITyped()
        );
    }
}
