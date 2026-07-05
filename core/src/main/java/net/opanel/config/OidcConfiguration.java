package net.opanel.config;

import java.util.ArrayList;
import java.util.List;

public class OidcConfiguration {
    public List<String> allowedUserIds;

    public OidcConfiguration() {
        this.allowedUserIds = new ArrayList<>();
    }
}
