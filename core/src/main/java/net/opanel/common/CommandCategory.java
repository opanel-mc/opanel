package net.opanel.common;

import java.util.*;

/**
 * Command categorization utility for better command organization and filtering
 */
public class CommandCategory {
    
    public enum Category {
        ADMIN("管理命令", "Administrative commands"),
        PLAYER("玩家命令", "Player commands"),
        WORLD("世界命令", "World management commands"),
        GAME("游戏命令", "Game mechanics commands"),
        UTILITY("工具命令", "Utility commands"),
        PLUGIN("插件命令", "Plugin commands"),
        OTHER("其他命令", "Other commands");
        
        private final String chineseName;
        private final String englishName;
        
        Category(String chineseName, String englishName) {
            this.chineseName = chineseName;
            this.englishName = englishName;
        }
        
        public String getChineseName() {
            return chineseName;
        }
        
        public String getEnglishName() {
            return englishName;
        }
    }
    
    // Command categorization mappings
    private static final Map<String, Category> COMMAND_CATEGORIES = new HashMap<>();
    
    static {
        // Admin commands
        addCommands(Category.ADMIN, 
            "op", "deop", "ban", "unban", "pardon", "kick", "whitelist", 
            "reload", "stop", "restart", "save-all", "save-on", "save-off",
            "difficulty", "defaultgamemode", "gamerule", "seed", "debug"
        );
        
        // Player commands
        addCommands(Category.PLAYER,
            "gamemode", "tp", "teleport", "spawn", "home", "sethome", 
            "back", "tpa", "tpaccept", "tpdeny", "msg", "tell", "r", "reply",
            "me", "say", "list", "who", "whois", "seen", "mail"
        );
        
        // World commands
        addCommands(Category.WORLD,
            "time", "weather", "worldborder", "setworldspawn", "spawnpoint",
            "fill", "clone", "setblock", "replaceitem", "blockdata",
            "testforblock", "testforblocks", "structure", "locate"
        );
        
        // Game commands
        addCommands(Category.GAME,
            "give", "clear", "enchant", "experience", "xp", "effect", 
            "summon", "kill", "entitydata", "scoreboard", "team",
            "advancement", "recipe", "function", "tag", "data"
        );
        
        // Utility commands
        addCommands(Category.UTILITY,
            "help", "?", "version", "plugins", "pl", "about", "info",
            "tps", "lag", "gc", "memory", "uptime", "ping", "motd"
        );
    }
    
    private static void addCommands(Category category, String... commands) {
        for (String command : commands) {
            COMMAND_CATEGORIES.put(command.toLowerCase(), category);
        }
    }
    
    /**
     * Get the category of a command
     */
    public static Category getCategory(String command) {
        if (command == null || command.isEmpty()) {
            return Category.OTHER;
        }
        
        String cleanCommand = command.toLowerCase().replaceFirst("/", "");
        return COMMAND_CATEGORIES.getOrDefault(cleanCommand, Category.PLUGIN);
    }
    
    /**
     * Filter commands by category
     */
    public static List<String> filterByCategory(List<String> commands, Category category) {
        List<String> filtered = new ArrayList<>();
        for (String command : commands) {
            if (getCategory(command) == category) {
                filtered.add(command);
            }
        }
        return filtered;
    }
    
    /**
     * Group commands by category
     */
    public static Map<Category, List<String>> groupByCategory(List<String> commands) {
        Map<Category, List<String>> grouped = new EnumMap<>(Category.class);
        
        // Initialize all categories
        for (Category category : Category.values()) {
            grouped.put(category, new ArrayList<>());
        }
        
        // Group commands
        for (String command : commands) {
            Category category = getCategory(command);
            grouped.get(category).add(command);
        }
        
        return grouped;
    }
    
    /**
     * Get priority score for command sorting (higher = more important)
     */
    public static int getPriority(String command) {
        Category category = getCategory(command);
        return switch (category) {
            case ADMIN -> 100;
            case PLAYER -> 90;
            case GAME -> 80;
            case WORLD -> 70;
            case UTILITY -> 60;
            case PLUGIN -> 50;
            case OTHER -> 40;
        };
    }
    
    /**
     * Sort commands by priority and alphabetically within each category
     */
    public static List<String> sortByPriority(List<String> commands) {
        List<String> sorted = new ArrayList<>(commands);
        sorted.sort((a, b) -> {
            int priorityA = getPriority(a);
            int priorityB = getPriority(b);
            
            if (priorityA != priorityB) {
                return Integer.compare(priorityB, priorityA); // Higher priority first
            }
            
            return a.compareToIgnoreCase(b); // Alphabetical within same priority
        });
        
        return sorted;
    }
}