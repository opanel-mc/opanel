package net.opanel.fabric_1_21_3.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.opanel.OPanel;
import net.opanel.common.Constants;

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
        );
    }
}
