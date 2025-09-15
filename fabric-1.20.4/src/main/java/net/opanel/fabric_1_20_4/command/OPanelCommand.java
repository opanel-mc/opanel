package net.opanel.fabric_1_20_4.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.opanel.OPanel;
import net.opanel.common.Constants;
import net.opanel.web.WebServer;

import static net.minecraft.server.command.CommandManager.*;

public class OPanelCommand implements CommandRegistrationCallback {
    private final OPanel instance;

    public OPanelCommand(OPanel instance) {
        this.instance = instance;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("opanel")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                literal("about")
                                        .executes(ctx -> {
                                            ctx.getSource().sendFeedback(() -> Text.of(Constants.ABOUT_INFO), false);
                                            return 1;
                                        })
                        )
                        .then(
                                literal("status")
                                        .executes(ctx -> {
                                            ctx.getSource().sendFeedback(() -> Text.of(instance.getStatus()), false);
                                            return 1;
                                        })
                        )
                        .then(
                                literal("start")
                                        .executes(ctx -> {
                                            WebServer webServer = instance.getWebServer();
                                            if(webServer.isRunning()) {
                                                ctx.getSource().sendFeedback(() -> Text.of("Web panel is already started."), false);
                                            } else {
                                                try {
                                                    webServer.start();
                                                    ctx.getSource().sendFeedback(() -> Text.of("Web panel is started successfully."), false);
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
                                                ctx.getSource().sendFeedback(() -> Text.of("Web panel is already stopped."), false);
                                            } else {
                                                try {
                                                    webServer.stop();
                                                    ctx.getSource().sendFeedback(() -> Text.of("Web panel is stopped successfully."), false);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    return 0;
                                                }
                                            }
                                            return 1;
                                        })
                        )
        );
    }
}
