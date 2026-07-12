package net.opanel.fabric_26_1.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.opanel.OPanel;
import net.opanel.common.Constants;
import net.opanel.web.WebServer;

import static net.minecraft.commands.Commands.*;

public class OPanelCommand implements CommandRegistrationCallback {
    private final OPanel instance;

    public OPanelCommand(OPanel instance) {
        this.instance = instance;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildCtx, Commands.CommandSelection selection) {
        dispatcher.register(
                literal("opanel")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS)))
                        .then(
                                literal("about")
                                        .executes(ctx -> {
                                            ctx.getSource().sendSuccess(() -> Component.nullToEmpty(Constants.ABOUT_INFO), false);
                                            return 1;
                                        })
                        )
                        .then(
                                literal("status")
                                        .executes(ctx -> {
                                            ctx.getSource().sendSuccess(() -> Component.nullToEmpty(instance.getStatus()), false);
                                            return 1;
                                        })
                        )
                        .then(
                                literal("start")
                                        .executes(ctx -> {
                                            WebServer webServer = instance.getWebServer();
                                            if(webServer.isRunning()) {
                                                ctx.getSource().sendSuccess(() -> Component.nullToEmpty("Web panel is already started."), false);
                                            } else {
                                                try {
                                                    webServer.start();
                                                    ctx.getSource().sendSuccess(() -> Component.nullToEmpty("Web panel is started successfully."), false);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    return 0;
                                                }
                                            }
                                            return 1;
                                        })
                        )
                        .then(
                                literal("stop")
                                        .executes(ctx -> {
                                            WebServer webServer = instance.getWebServer();
                                            if(!webServer.isRunning()) {
                                                ctx.getSource().sendSuccess(() -> Component.nullToEmpty("Web panel is already stopped."), false);
                                            } else {
                                                try {
                                                    webServer.stop();
                                                    ctx.getSource().sendSuccess(() -> Component.nullToEmpty("Web panel is stopped successfully."), false);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    return 0;
                                                }
                                            }
                                            return 1;
                                        })
                        )
                        .then(
                                literal("restart-server")
                                        .executes(ctx -> {
                                            instance.getServer().restart(instance.getConfig().serverRestartDelay);
                                            return 1;
                                        })
                        )
        );
    }
}
