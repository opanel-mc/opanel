package net.opanel.fabric_1_21_5;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameMode;
import net.opanel.common.OPanelGameMode;
import net.opanel.common.OPanelPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class FabricPlayer implements OPanelPlayer {
    private String name;
    private String uuid;
    private boolean isOnline;
    private boolean isOp;
    private boolean isBanned;
    private OPanelGameMode gamemode;

    private FabricPlayer() {}

    @Nullable
    public static FabricPlayer from(ServerPlayerEntity serverPlayer) {
        if(serverPlayer == null) return null;

        FabricPlayer player = new FabricPlayer();
        player.setName(serverPlayer.getName().getString());
        player.setUUID(serverPlayer.getUuidAsString());
        player.setIsOnline(true);
        player.setIsOp(serverPlayer.hasPermissionLevel(2));
        player.setIsBanned(false);
        GameMode gamemode = serverPlayer.getGameMode();
        switch(gamemode) {
            case ADVENTURE -> player.setGameMode(OPanelGameMode.ADVENTURE);
            case SURVIVAL -> player.setGameMode(OPanelGameMode.SURVIVAL);
            case CREATIVE -> player.setGameMode(OPanelGameMode.CREATIVE);
            case SPECTATOR -> player.setGameMode(OPanelGameMode.SPECTATOR);
        }
        return player;
    }

    @Nullable
    public static FabricPlayer from(MinecraftServer server, Path playerDataPath, UUID uuid) throws IOException {
        final UserCache userCache = server.getUserCache();
        final PlayerManager playerManager = server.getPlayerManager();

        if(userCache == null) return null;

        ServerPlayerEntity serverPlayer = playerManager.getPlayer(uuid);
        if(serverPlayer != null && !serverPlayer.isDisconnected()) {
            return FabricPlayer.from(serverPlayer);
        }

        FabricPlayer player = new FabricPlayer();
        player.setUUID(uuid);

        Optional<GameProfile> profileOpt = userCache.getByUuid(uuid);
        if(profileOpt.isEmpty()) return null;

        GameProfile profile = profileOpt.get();
        player.setName(profile.getName());
        player.setIsOp(playerManager.isOperator(profile));
        player.setIsBanned(playerManager.getUserBanList().contains(profile));

        NbtCompound nbt = NbtIo.readCompressed(playerDataPath, NbtSizeTracker.of(2097152L));
        int gamemodeId = nbt.getInt("playerGameType", 0);
        GameMode gamemode = GameMode.byIndex(gamemodeId);
        switch(gamemode) {
            case ADVENTURE -> player.setGameMode(OPanelGameMode.ADVENTURE);
            case SURVIVAL -> player.setGameMode(OPanelGameMode.SURVIVAL);
            case CREATIVE -> player.setGameMode(OPanelGameMode.CREATIVE);
            case SPECTATOR -> player.setGameMode(OPanelGameMode.SPECTATOR);
        }
        return player;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUUID(UUID uuid) {
        setUUID(uuid.toString());
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public void setIsOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public void setIsOp(boolean isOp) {
        this.isOp = isOp;
    }

    public void setIsBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    public void setGameMode(OPanelGameMode gamemode) {
        this.gamemode = gamemode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public boolean isOp() {
        return isOp;
    }

    @Override
    public boolean isBanned() {
        return isBanned;
    }

    @Override
    public OPanelGameMode getGameMode() {
        return gamemode;
    }
}
