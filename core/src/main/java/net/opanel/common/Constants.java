package net.opanel.common;

import net.opanel.OPanel;

public class Constants {
    public static final int BSTATS_ID = 29765;

    public static final String ABOUT_INFO = """
            §8===========================================
            §r §6§lOPanel§r - §fA Minecraft server management panel

            §r§7Version: §r%s
            §r§7Author: §rNriotHrreion
            §r§7Website: §r§nhttps://opanel.cn
            §r§7Source Code: §r§nhttps://github.com/opanel-mc/opanel
            §r§7License: §cGPL-3.0
            §8===========================================""".formatted(OPanel.VERSION);

    public static final String INITIAL_ACCESS_KEY_TEMPLATE = """
            # Remember to DELETE this file for your server security!
            # 为了您服务器的安全，请记得删除此文件！

            """;
}
