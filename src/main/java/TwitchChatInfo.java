import org.pircbotx.hooks.events.MessageEvent;

public class TwitchChatInfo  implements MessageAdapter {
    @Override
    public void onBotCommand(String command, String data, String userName, long userID, MessageEvent e) {
        switch (command) {
            case "followage" -> replyFollowAge(userName, userID);
        }
    }


    private static void replyFollowAge(String name, long userID) {
        var period = TwitchAPI.followAge(userID);
        if (period == null) TwitchChat.sendMessage(name + " you do not follow the stream bobSigh");
        else TwitchChat.sendMessage(name + " you have followed for: " + Common.prettyPrintPeriod(period));
    }
}
