import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;


public class TwitchChatListenerManager extends ThreadedListenerManager {

    @Override
    public void onEvent(Event event) {
        if (event instanceof JoinEvent) { //TODO run this code in its own thread.
            var e = (JoinEvent) event;
            var name = e.getUser().getNick().toLowerCase();

            Triple<Long, Timestamp, String> lookup = Database.getTripleFromSql("""
                SELECT tui, updated_at, twitch_name FROM twitch_name_to_tui
                WHERE twitch_name = ?
            """, name);
            long tui = lookup != null ? lookup.first : 0;
            String display = lookup != null ? lookup.third : "";

            if (lookup == null || Duration.between(lookup.second.toInstant(), Instant.now()).toDays() > 90) {
                var resp = TwitchAPI.APIQuery("users?login=" + name);
                var obj = resp.get("data").getAsJsonArray().get(0).getAsJsonObject();
                tui = Long.parseLong(obj.get("id").getAsString());
                display = obj.get("display_name").getAsString();
                if (display.isEmpty()) display = name;
                Database.executePreparedSQL("""
                    INSERT INTO tuis (id, name) VALUES (?, ?)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name
                    """, tui, display
                );
                Database.executePreparedSQL("""
                    INSERT INTO twitch_name_to_tui (twitch_name, tui) VALUES (?, ?)
                    ON CONFLICT (twitch_name) DO UPDATE
                    SET tui = EXCLUDED.tui, updated_at = NOW()
                    """, name, tui
                );
            }

            var welcome = Database.getValueFromSQL("""
                    SELECT welcome_message FROM tuis WHERE id = ? AND updated_at < NOW() - '5 hour'::interval
                    """, String.class, tui);
            if (welcome != null) {
                if (welcome.startsWith(".") || welcome.startsWith("/") && !welcome.startsWith("/me")) {
                    welcome = "Stop being naughty: " + display;
                }
                TwitchChat.sendMessage(welcome);
                System.out.println("welcome Message to: " + display);
                Database.executePreparedSQL("""
                        UPDATE tuis SET updated_at = NOW() WHERE id = ?
                        """, tui);
            }
        }

        if (event instanceof MessageEvent) { //TODO USE java 16 and remove cast
            var e = (MessageEvent)event;
            long userID =  Long.parseLong(e.getTags().get("user-id"));
            String name = e.getTags().get("display-name");
            if (name.isEmpty()) name = e.getUser().getNick();

            Database.executePreparedSQL("""
                    INSERT INTO twitch_chat_lines (tui, text) VALUES (?, ?)
                    """, userID, e.getMessage()
            );

            Database.executePreparedSQL("""
                    INSERT INTO tuis (id, name, have_subbed, chat_lines) VALUES (?, ?, ?, 1)
                    ON CONFLICT (id) DO UPDATE
                    SET
                        name = EXCLUDED.name,
                        have_subbed = EXCLUDED.have_subbed OR tuis.have_subbed,
                        chat_lines = tuis.chat_lines + 1,
                        updated_at = NOW()
                    """, userID, name, e.getTags().get("subscriber").equals("1")
            );

            Database.executePreparedSQL("""
                    INSERT INTO twitch_name_to_tui (twitch_name, tui) VALUES (?, ?)
                    ON CONFLICT (twitch_name) DO UPDATE
                    SET tui = EXCLUDED.tui, updated_at = NOW()
                    """, name.toLowerCase(), userID
            );
        }
        super.onEvent(event);
    }
}
