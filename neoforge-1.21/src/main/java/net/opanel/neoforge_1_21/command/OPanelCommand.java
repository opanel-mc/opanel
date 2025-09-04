package net.opanel.neoforge_1_21.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.opanel.OPanel;
import net.opanel.common.Constants;
import net.opanel.neoforge_1_21.Main;

import static net.minecraft.commands.Commands.*;

@EventBusSubscriber(modid = Main.MODID)
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
        );
    }
}
