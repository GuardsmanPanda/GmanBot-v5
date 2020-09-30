import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.events.MessageEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class TwitchChat {
    private static PircBotX bot;
    private static final String channel = "#guardsmanbob";

    static {
        var config = new Configuration.Builder()
                .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
                .addCapHandler(new EnableCapHandler("twitch.tv/commands"))
                .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
                .setListenerManager(new TwitchChatListenerManager())
                .addAutoJoinChannel(channel)
                .setOnJoinWhoEnabled(false)
                .setAutoNickChange(false)
                .setAutoReconnect(true)
                .setName("gmanbot")
                .buildForServer("irc.chat.twitch.tv", 6667, Config.env("twitch_chat_pw"));
        bot = new PircBotX(config);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(TwitchChat::hourStats, 1, 1, TimeUnit.HOURS);
    }

    public static void main(String[] args) {
        bot.getConfiguration().getListenerManager().addListener(new TwitchChatConfig());
        bot.getConfiguration().getListenerManager().addListener(new TwitchChatGames());
        bot.getConfiguration().getListenerManager().addListener(new TwitchChatInfo());
        bot.getConfiguration().getListenerManager().addListener(new InfoListener());
        connect();
    }

    public static void connect() {
        try {
            bot.startBot();
        } catch (IOException | IrcException e) {
            e.printStackTrace();
        }
    }

    /*
    MESSAGE BITS
     */
    public static void sendMessage(String message) {
        bot.sendIRC().message(channel, message);
    }


    private static void hourStats() {
        System.out.println("hours stats");
        int idle = bot.getUserChannelDao()
                .getChannel(channel)
                .getUsers()
                .stream()
                .map(user -> Database.executePreparedSQL("""
                            UPDATE tuis
                            SET idle_hours = idle_hours + 1, bob_coins = bob_coins + 4
                            WHERE id = (SELECT tui FROM twitch_name_to_tui WHERE twitch_name = ?)
                            AND updated_at < NOW() - '1 hour'::interval
                        """, user.getNick()))
                .mapToInt(Integer::intValue)
                .sum();
        int active = Database.executePreparedSQL("""
            UPDATE tuis
            SET active_hours = active_hours + 1, bob_coins = bob_coins + 6
            WHERE updated_at > NOW() - '1 hour'::interval
        """);
        System.out.println("Hour stats: " + active + " active, " + idle + " idle.");
    }

    private static class InfoListener implements MessageAdapter {
        @Override
        public void onMessage(String name, long id, String message, MessageEvent e) {
            // System.out.println(name + "> "  + message);
            // System.out.println(e.getTags());
        }
    }
}
