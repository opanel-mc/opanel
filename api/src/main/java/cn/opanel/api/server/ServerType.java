package cn.opanel.api.server;

public enum ServerType { // 服务器平台家族
    PAPER("Paper"), // Paper 平台
    FABRIC("Fabric"), // Fabric 平台
    FORGE("Forge"), // Forge 平台
    NEOFORGE("NeoForge"), // NeoForge 平台
    FOLIA("Folia"), // Folia 平台
    LEAVES("Leaves"); // Leaves 平台

    private final String name; // 人类可读的平台名称

    ServerType(String name) { // 使用显示名称构造平台类型
        this.name = name;
    }

    /**
     * Returns the human-readable platform name.
     *
     * @return display name
     */
    public String getName() { // 返回人类可读的平台名称
        return name;
    }
}
