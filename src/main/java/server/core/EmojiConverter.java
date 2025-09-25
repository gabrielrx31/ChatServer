package server.core;

import java.util.HashMap;
import java.util.Map;

public class EmojiConverter {
    
    // Map of text shortcuts to Unicode emoji characters
    private static final Map<String, String> emojiMap = new HashMap<>();
    
    static {
        // Common ASCII emoticons
        emojiMap.put(":)", "😊");
        emojiMap.put(":-)", "😊");
        emojiMap.put(":D", "😃");
        emojiMap.put(":-D", "😃");
        emojiMap.put(":(", "😢");
        emojiMap.put(":-(", "😢");
        emojiMap.put(":P", "😛");
        emojiMap.put(":-P", "😛");
        emojiMap.put(";)", "😉");
        emojiMap.put(";-)", "😉");
        emojiMap.put(":o", "😮");
        emojiMap.put(":-o", "😮");
        emojiMap.put(":O", "😱");
        emojiMap.put(":-O", "😱");
        emojiMap.put(":|", "😐");
        emojiMap.put(":-|", "😐");
        emojiMap.put(":/", "😕");
        emojiMap.put(":-/", "😕");
        emojiMap.put(":\\", "😕");
        emojiMap.put(":-\\", "😕");
        emojiMap.put("<3", "❤️");
        emojiMap.put("</3", "💔");
        emojiMap.put(":*", "😘");
        emojiMap.put(":-*", "😘");
        emojiMap.put("XD", "😆");
        emojiMap.put("xD", "😆");
        emojiMap.put("^_^", "😊");
        emojiMap.put("^.^", "😊");
        emojiMap.put("-_-", "😑");
        emojiMap.put(">:(", "😠");
        emojiMap.put(">:-(", "😠");
        emojiMap.put("8)", "😎");
        emojiMap.put("8-)", "😎");
        emojiMap.put("B)", "😎");
        emojiMap.put("B-)", "😎");
        
        // Named emoji shortcuts (Discord/Slack style)
        emojiMap.put(":smile:", "😊");
        emojiMap.put(":grin:", "😃");
        emojiMap.put(":joy:", "😂");
        emojiMap.put(":cry:", "😢");
        emojiMap.put(":sob:", "😭");
        emojiMap.put(":angry:", "😠");
        emojiMap.put(":rage:", "😡");
        emojiMap.put(":heart:", "❤️");
        emojiMap.put(":broken_heart:", "💔");
        emojiMap.put(":kiss:", "😘");
        emojiMap.put(":wink:", "😉");
        emojiMap.put(":sunglasses:", "😎");
        emojiMap.put(":stuck_out_tongue:", "😛");
        emojiMap.put(":neutral_face:", "😐");
        emojiMap.put(":confused:", "😕");
        emojiMap.put(":shocked:", "😱");
        emojiMap.put(":surprised:", "😮");
        emojiMap.put(":laughing:", "😆");
        emojiMap.put(":thinking:", "🤔");
        emojiMap.put(":thumbsup:", "👍");
        emojiMap.put(":thumbsdown:", "👎");
        emojiMap.put(":ok_hand:", "👌");
        emojiMap.put(":clap:", "👏");
        emojiMap.put(":wave:", "👋");
        emojiMap.put(":fire:", "🔥");
        emojiMap.put(":star:", "⭐");
        emojiMap.put(":100:", "💯");
        emojiMap.put(":muscle:", "💪");
        emojiMap.put(":party:", "🎉");
        emojiMap.put(":cake:", "🎂");
        emojiMap.put(":pizza:", "🍕");
        emojiMap.put(":coffee:", "☕");
        emojiMap.put(":beer:", "🍺");
        emojiMap.put(":dog:", "🐶");
        emojiMap.put(":cat:", "🐱");
        emojiMap.put(":poop:", "💩");
        emojiMap.put(":rocket:", "🚀");
        emojiMap.put(":computer:", "💻");
        emojiMap.put(":phone:", "📱");
        emojiMap.put(":money:", "💰");
        emojiMap.put(":warning:", "⚠️");
        emojiMap.put(":check:", "✅");
        emojiMap.put(":x:", "❌");
        emojiMap.put(":question:", "❓");
        emojiMap.put(":exclamation:", "❗");
    }
    
    public static String convert(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String result = text;
        
        // Replace all emoji shortcuts with their Unicode equivalents
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            // Use regex word boundaries for named emojis to avoid partial matches
            if (entry.getKey().startsWith(":") && entry.getKey().endsWith(":") && entry.getKey().length() > 2) {
                result = result.replaceAll("\\b" + escapeRegex(entry.getKey()) + "\\b", entry.getValue());
            } else {
                // For ASCII emoticons, replace all occurrences
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
   
    private static String escapeRegex(String text) {
        return text.replaceAll("([\\[\\](){}*+?^$|\\\\])", "\\\\$1");
    }
   
    public static Map<String, String> getSupportedEmojis() {
        return new HashMap<>(emojiMap);
    }
    
    public static boolean containsEmojiShortcuts(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        for (String shortcut : emojiMap.keySet()) {
            if (text.contains(shortcut)) {
                return true;
            }
        }
        
        return false;
    }
}
