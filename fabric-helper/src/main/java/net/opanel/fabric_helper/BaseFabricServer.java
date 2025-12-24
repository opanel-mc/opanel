package net.opanel.fabric_helper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import net.opanel.common.OPanelPlayer;
import net.opanel.common.OPanelServer;
import net.opanel.common.ServerType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public abstract class BaseFabricServer implements OPanelServer {
    protected final MinecraftServer server;

    public BaseFabricServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.FABRIC;
    }

    @Override
    public String getMotd() {
        return server.getServerMotd();
    }

    @Override
    public void setMotd(String motd) throws IOException {
        // Call setMotd() first
        server.setMotd(motd);
        // Directly modify motd in server.properties
        String formatted = motd.replaceAll("\n", Matcher.quoteReplacement("\\n"));
        OPanelServer.writePropertiesContent(OPanelServer.getPropertiesContent().replaceAll("motd=.+", Matcher.quoteReplacement("motd="+ formatted)));
    }

    @Override
    public String getVersion() {
        return server.getVersion();
    }

    @Override
    public int getPort() {
        return server.getServerPort();
    }

    @Override
    public void saveAll() {
        server.saveAll(true, true, true);
    }

    @Override
    public int getMaxPlayerCount() {
        return server.getMaxPlayerCount();
    }

    @Override
    public OPanelPlayer getPlayer(String uuid) {
        for(OPanelPlayer player : getPlayers()) {
            if(player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public void removePlayerData(String uuid) throws IOException {
        final Path playerDataFolder = server.getSavePath(WorldSavePath.PLAYERDATA);
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat"));
        Files.deleteIfExists(playerDataFolder.resolve(uuid +".dat_old"));
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        for(CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
            commands.add(node.getName());
        }
        return commands;
    }

    @Override
    public List<String> getCommandTabList(int argIndex, String command) {
        if(argIndex == 1) return getCommands();

        List<String> tabList = new ArrayList<>();
        String[] args = command.split(" ");
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
        CommandNode<ServerCommandSource> currentNode = dispatcher.getRoot();
        for(int i = 0; i <= args.length; i++) {
            if(currentNode == null) break;
            if(i + 1 == argIndex) {
                for(CommandNode<ServerCommandSource> subNode : currentNode.getChildren()) {
                    tabList.add(subNode.getName());
                }
                break;
            }
            if(i == args.length) break;
            currentNode = currentNode.getChild(args[i]);
        }
        return tabList;
    }

    @Override
    public void reload() {
        // directly execute /reload
        sendServerCommand("reload");
    }

    @Override
    public void stop() {
        server.stop(false);
    }

    @Override
    public long getIngameTime() {
        return server.getOverworld().getTimeOfDay();
    }
}
