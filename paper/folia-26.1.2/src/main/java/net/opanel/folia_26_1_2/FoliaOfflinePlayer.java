package net.opanel.folia_26_1_2;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import net.opanel.annotation.Rewrite;
import net.opanel.paper_helper.BasePaperOfflinePlayer;
import net.opanel.common.OPanelPlayer;
import org.bukkit.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class FoliaOfflinePlayer extends BasePaperOfflinePlayer implements OPanelPlayer {
    private final PlayerProfile profile;

    public FoliaOfflinePlayer(Main plugin, Server server, OfflinePlayer player) {
        super(plugin, server, player);

        profile = player.getPlayerProfile();
    }

    @Rewrite
    @Override
    protected Path getPlayerDataPath() {
        String uuid = player.getUniqueId().toString();
        Path path = server.getWorlds().getFirst().getWorldFolder().toPath().resolve("../../../players/data/"+ uuid +".dat");
        if(!Files.exists(path)) {
            throw new NullPointerException("Player data file for UUID "+ uuid +" unavailable.");
        }
        return path;
    }

    @Override
    public void ban(String reason) {
        if(isBanned()) return;
        runner.runTask(() -> player.ban(reason, (Date) null, null));
    }

    @Override
    public String getBanReason() {
        if(!isBanned()) return null;
        BanList<PlayerProfile> banList = server.getBanList(BanListType.PROFILE);
        BanEntry<PlayerProfile> banEntry = banList.getBanEntry(profile);
        if(banEntry == null) return null;
        return banEntry.getReason();
    }

    @Override
    public void pardon() {
        if(!isBanned()) return;
        BanList<PlayerProfile> banList = server.getBanList(BanListType.PROFILE);
        runner.runTask(() -> banList.pardon(profile));
    }

    @Override
    public FoliaOfflineInventory getInventory() {
        return new FoliaOfflineInventory(playerDataPath);
    }
}