package cn.opanel.api.server;

public enum Dimension { // 服务器维度
    OVERWORLD("overworld"), // 主世界
    NETHER("nether"), // 下界
    THE_END("the_end"); // 末地

    private final String name; // OPanel 协议使用的稳定序列化名称

    Dimension(String name) { // 使用序列化名称构造维度
        this.name = name;
    }

    /**
     * Returns the stable serialized name used by OPanel protocols.
     *
     * @return the serialized dimension name
     */
    public String getName() { // 返回 OPanel 协议使用的稳定序列化名称
        return name;
    }
}
