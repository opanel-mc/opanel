package net.opanel.fabric_1_20_3;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.opanel.common.OPanelWhitelist;
import net.opanel.fabric_helper.BaseFabricWhitelist;

import java.io.IOException;
import java.util.UUID;

public class FabricWhitelist extends BaseFabricWhitelist implements OPanelWhitelist {
    public FabricWhitelist(Whitelist whitelist) {
        super(whitelist);
    }

    @Override
    public void add(OPanelWhitelistEntry entry) throws IOException {
        if(getNames().contains(entry.name)) return;
        GameProfile profile = new GameProfile(UUID.fromString(entry.uuid), entry.name);
        whitelist.add(new WhitelistEntry(profile));
        whitelist.save();
    }

    @Override
    public void remove(OPanelWhitelistEntry entry) throws IOException {
        if(!getNames().contains(entry.name)) return;
        GameProfile profile = new GameProfile(UUID.fromString(entry.uuid), entry.name);
        whitelist.remove(new WhitelistEntry(profile));
        whitelist.save();
    }
}
