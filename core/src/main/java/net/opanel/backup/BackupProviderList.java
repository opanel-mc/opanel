package net.opanel.backup;

import java.util.ArrayList;
import java.util.List;

public class BackupProviderList {
    public List<BackupProviderConfig> providers;

    public BackupProviderList() {
        providers = new ArrayList<>();
    }
}
