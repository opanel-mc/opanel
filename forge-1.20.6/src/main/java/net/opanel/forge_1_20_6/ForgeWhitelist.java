package net.opanel.forge_1_20_6;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.opanel.common.OPanelWhitelist;
import net.opanel.forge_helper.BaseForgeWhitelist;

import java.io.IOException;
import java.util.UUID;

public class ForgeWhitelist extends BaseForgeWhitelist implements OPanelWhitelist {
    public ForgeWhitelist(UserWhiteList whitelist) {
        super(whitelist);
    }

    @Override
    public void add(OPanelWhitelistEntry entry) throws IOException {
        if(getNames().contains(entry.name)) return;
        GameProfile profile = new GameProfile(UUID.fromString(entry.uuid), entry.name);
        whitelist.add(new UserWhiteListEntry(profile));
        whitelist.save();
    }

    @Override
    public void remove(OPanelWhitelistEntry entry) throws IOException {
        if(!getNames().contains(entry.name)) return;
        GameProfile profile = new GameProfile(UUID.fromString(entry.uuid), entry.name);
        whitelist.remove(new UserWhiteListEntry(profile));
        whitelist.save();
    }
}
