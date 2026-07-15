package net.opanel.common.features;

import java.nio.file.Path;

public interface PaperDimensionFeature {
    /**
     * Get the relative path of the nether dimension
     */
    Path getNetherPath();
    /**
     * Get the relative path of the end dimension
     */
    Path getTheEndPath();
}
