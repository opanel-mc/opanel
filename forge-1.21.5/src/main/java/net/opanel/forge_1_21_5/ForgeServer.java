package net.opanel.forge_1_21_5;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelSave;
import net.opanel.common.OPanelServer;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ForgeServer implements OPanelServer {
    private static final Path serverPropertiesPath = Paths.get("").resolve("server.properties");

    private final MinecraftServer server;
    private final DedicatedServer dedicatedServer;

    public ForgeServer(MinecraftServer server) {
        this.server = server;
        dedicatedServer = (DedicatedServer) server;
    }

    /** @todo */
    @Override
    public byte[] getFavicon() {
        return null;
    }

    @Override
    public String getMotd() {
        return server.getMotd();
    }

    @Override
    public void setMotd(String motd) throws IOException {
        // Call setMotd() first
        server.setMotd(motd);
        // Directly modify motd in server.properties
        writePropertiesContent(getPropertiesContent().replaceAll("motd=.+", "motd="+ motd));
        // Directly modify motd in memory
        final DedicatedServerProperties properties = dedicatedServer.getProperties();
        try {
            // Force modifying motd field through reflect because it is final
            Field motdField = properties.getClass().getDeclaredField("motd");
            motdField.setAccessible(true);
            motdField.set(properties, motd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getVersion() {
        return server.getServerVersion();
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    /** @todo */
    @Override
    public List<OPanelSave> getSaves() {
        return List.of();
    }

    /** @todo */
    @Override
    public OPanelSave getSave(String saveName) {
        return null;
    }

    /** @todo */
    @Override
    public List<OPanelPlayer> getOnlinePlayers() {
        return List.of();
    }

    /** @todo */
    @Override
    public List<OPanelPlayer> getPlayers() {
        return List.of();
    }

    @Override
    public int getMaxPlayerCount() {
        return server.getMaxPlayers();
    }

    /** @todo */
    @Override
    public OPanelPlayer getPlayer(String uuid) {
        return null;
    }

    @Override
    public boolean isWhitelistEnabled() {
        return server.getPlayerList().isUsingWhitelist();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        server.getPlayerList().setUsingWhiteList(enabled);
    }

    /** @todo */
    @Override
    public OPanelWhitelist getWhitelist() {
        return null;
    }

    @Override
    public void sendServerCommand(String command) {
        Commands manager = server.getCommands();
        CommandSourceStack source = server.createCommandSourceStack();
        manager.performPrefixedCommand(source, command);
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        for(CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChildren()) {
            commands.add(node.getName());
        }
        return commands;
    }

    /** @todo */
    @Override
    public HashMap<String, Object> getGamerules() {
        return new HashMap<>();
    }

    /** @todo */
    @Override
    public void setGamerules(HashMap<String, Object> gamerules) {

    }

    @Override
    public void reload() {
        // directly execute /reload
        sendServerCommand("reload");
    }

    @Override
    public void stop() {
        server.halt(false);
    }

    @Override
    public String getPropertiesContent() throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        return Utils.readTextFile(serverPropertiesPath);
    }

    @Override
    public void writePropertiesContent(String newContent) throws IOException {
        if(!Files.exists(serverPropertiesPath)) {
            throw new IOException("Cannot find server.properties");
        }
        Utils.writeTextFile(serverPropertiesPath, newContent);
    }

    @Override
    public long getIngameTime() {
        return server.overworld().getDayTime();
    }
}
