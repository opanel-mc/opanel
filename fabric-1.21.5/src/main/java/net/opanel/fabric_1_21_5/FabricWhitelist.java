package net.opanel.fabric_1_21_5;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FabricWhitelist implements OPanelWhitelist {
    private final Whitelist whitelist;
    private final Path whitelistPath;

    public FabricWhitelist(Whitelist whitelist) {
        this.whitelist = whitelist;
        whitelistPath = whitelist.getFile().toPath();
    }

    @Override
    public List<String> getNames() throws IOException {
        whitelist.load();
        return Arrays.asList(whitelist.getNames());
    }

    @Override
    public List<OPanelWhitelistEntry> getEntries() throws IOException {
        final String jsonStr = Utils.readTextFile(whitelistPath);
        final Type listType = new TypeToken<List<OPanelWhitelistEntry>>() {}.getType();
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, listType);
    }

    @Override
    public void write(List<OPanelWhitelistEntry> entries) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Utils.writeTextFile(whitelistPath, gson.toJson(entries));
        whitelist.load();
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
