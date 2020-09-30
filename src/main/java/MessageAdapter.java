import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MessageAdapter extends Listener {
    @Override
    default void onEvent(Event event) {
        if (!(event instanceof MessageEvent)) return;
        MessageEvent e = (MessageEvent)event;
        long userID =  Long.parseLong(e.getTags().get("user-id"));
        String name = e.getTags().get("display-name");
        if (name.isEmpty()) name = e.getUser().getNick();
        onMessage(name, userID, e.getMessage(), e);
        if (e.getMessage().startsWith("!")) {
            Matcher matcher = Pattern.compile("^!([^ ]*) ?(.*)$").matcher(e.getMessage());
            if (matcher.matches()) {
                onBotCommand(matcher.group(1), matcher.group(2), name, userID, e);
            }
        }
    }

    default void onMessage(String displayName, long userID, String message, MessageEvent e) { }

    default void onBotCommand(String command, String data, String userName, long userID, MessageEvent e) { }
}
