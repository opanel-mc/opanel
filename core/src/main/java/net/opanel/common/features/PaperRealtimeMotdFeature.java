package net.opanel.common.features;

import java.util.concurrent.CompletableFuture;

public interface PaperRealtimeMotdFeature {
    CompletableFuture<String> getMotdAsync();
}
