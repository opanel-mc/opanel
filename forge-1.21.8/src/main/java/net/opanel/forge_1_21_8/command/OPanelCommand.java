package net.opanel.forge_1_21_8.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.opanel.OPanel;
import net.opanel.common.Constants;
import net.opanel.forge_1_21_8.Main;
import net.opanel.web.WebServer;

import static net.minecraft.commands.Commands.*;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class OPanelCommand {
    // Will be set later by Main class
    public static OPanel instance;

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                literal("opanel")
                        .requires(source -> source.hasPermission(2))
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
        );
    }
}
