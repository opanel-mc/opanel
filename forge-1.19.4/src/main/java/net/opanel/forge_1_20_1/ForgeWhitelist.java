package net.opanel.forge_1_20_1;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ForgeWhitelist implements OPanelWhitelist {
    private final UserWhiteList whitelist;
    private final Path whitelistPath;

    public ForgeWhitelist(UserWhiteList whitelist) {
        this.whitelist = whitelist;
        whitelistPath = whitelist.getFile().toPath();
    }

    @Override
    public List<String> getNames() throws IOException {
        whitelist.load();
        return Arrays.asList(whitelist.getUserList());
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
