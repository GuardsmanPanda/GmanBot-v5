import org.pircbotx.hooks.events.MessageEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TwitchChatConfig  implements MessageAdapter {
    private static final Map<String, String> heartMapping = new HashMap<>();
    private static final Map<String, String> flagMapping = new HashMap<>();
    private static final List<String> flagNames = new ArrayList<>();
    private static final List<String> heartColors;

    static {
        fillFlagListAndMap();
        fillHeartMap();
        heartColors = heartMapping.values().stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void onBotCommand(String command, String data, String userName, long userID, MessageEvent e) {
        switch (command) {
            case "setflag" -> setFlag(userID, data);
            case "bobheart" -> heartsBob(userID, data);
            case "setwelcomemessage" -> setWelcomeMessage(userID, data);
        }
    }

    private static void setWelcomeMessage(long tui, String message) {
        if (message.length() > 0) Database.executePreparedSQL("UPDATE tuis SET welcome_message = ?  WHERE id = ?", message, tui);
    }

    private static void heartsBob(long userID, String data) {
        if (data.equals("colors")) {
            TwitchChat.sendMessage("You can chose a heart in: " + String.join(" \uD83D\uDD38", heartColors));
            return;
        }
        data = data.replaceAll("\\W+", "").toLowerCase();
        if (data.equals("random")) data = heartColors.get(Common.randomInt(heartColors.size()));
        if (!heartMapping.containsKey(data)) System.out.println("COULD NOT COLOR HEART FOR: " + data);
        Database.executePreparedSQL("UPDATE tuis SET heart_color = ? WHERE id = ?", heartMapping.getOrDefault(data, "red"), userID);
    }

    private static void setFlag(long userID, String data) {
        data = data.replaceAll("\\W+", "").toLowerCase().trim();
        if (data.equals("random")) {
            Database.executePreparedSQL("UPDATE tuis SET flag = ? WHERE id = ?", flagNames.get(Common.randomInt(flagNames.size())), userID);
        }
        else if (flagMapping.containsKey(data)) {
            Database.executePreparedSQL("UPDATE tuis SET flag = ? WHERE id = ?", flagMapping.get(data), userID);
        }
        else System.out.println("COULDN'T TRANSLATE: " + data);
    }

    private static void fillFlagListAndMap() {
        try {
            for (String text : Files.readAllLines(Path.of("data/flagtranslation.txt"))) {
                String[] words = text.split(",");
                flagMapping.put(words[0].toLowerCase(), words[0]);
                flagNames.add(words[0]);
                for (int i = 1; i < words.length; i++) {
                    if (flagMapping.containsKey(words[i])) {
                        System.out.println("ERROR on flag mapping: " + flagMapping.get(words[i]) + " AND:" + text);
                    }
                    flagMapping.put(words[i], words[0]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fillHeartMap() {
        heartMapping.put("red", "red");
        heartMapping.put("", "red");

        heartMapping.put("blue", "blue");
        heartMapping.put("darkblue", "blue");

        heartMapping.put("lightblue", "lightblue");
        heartMapping.put("skyblue", "lightblue");
        heartMapping.put("sky", "lightblue");

        heartMapping.put("green", "green");
        heartMapping.put("darkgreen", "green");
        heartMapping.put("emerald", "green");

        heartMapping.put("lightgreen", "lightgreen");
        heartMapping.put("limegreen", "lightgreen");
        heartMapping.put("lime", "lightgreen");

        heartMapping.put("white", "white");

        heartMapping.put("black", "black");
        heartMapping.put("dark", "black");

        heartMapping.put("pink", "pink");

        heartMapping.put("orange", "orange");

        heartMapping.put("yellow", "yellow");

        heartMapping.put("rainbow", "rainbow");
        heartMapping.put("pride", "rainbow");
        heartMapping.put("lgbt", "rainbow");
    }
}
