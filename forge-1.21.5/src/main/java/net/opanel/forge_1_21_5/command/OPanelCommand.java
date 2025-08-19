package net.opanel.forge_1_21_5.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.opanel.OPanel;
import net.opanel.common.Constants;
import net.opanel.forge_1_21_5.Main;

import static net.minecraft.commands.Commands.*;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class OPanelCommand {
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
