package net.opanel.fabric_helper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.Whitelist;
import net.opanel.common.OPanelWhitelist;
import net.opanel.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public abstract class BaseFabricWhitelist implements OPanelWhitelist {
    protected final Whitelist whitelist;
    protected final Path whitelistPath;

    public BaseFabricWhitelist(Whitelist whitelist) {
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
}
