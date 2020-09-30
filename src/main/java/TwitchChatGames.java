import org.pircbotx.hooks.events.MessageEvent;

public class TwitchChatGames implements MessageAdapter {

    @Override
    public void onBotCommand(String command, String data, String userName, long userID, MessageEvent e) {
        switch (command) {
            case "dg" -> playDiceGolf(data, userName, userID);
        }
    }

    private static void playDiceGolf(String data, String userName, long userID) {
        int DICEGOLFMAXDAILYGAMES = 20, start = 100;
        StringBuilder sb = new StringBuilder("⛳ DiceGolf: ");

        try {
            start = Integer.parseInt(data);
        } catch (NumberFormatException e) {}
        start = Math.max(start, 2);

        StringBuilder gameBuilder = new StringBuilder();
        gameBuilder.append(start).append(" ➜");

        long sum = 0, length = 0, orig = start;
        while (start != 1) {
            start = Common.randomInt(start)+1;
            gameBuilder.append(' ').append(start).append(',');
            sum += start;
            length++;
        } // ⛳ DiceGolf: 100 ➜ 87, 65, 23, 2, 1 ⚌ 5 ∑ 152 ⛳
        gameBuilder.setLength(gameBuilder.length()-1);
        sb.append(gameBuilder).append(" ⚌ ").append(length).append(" ∑ ").append(sum).append(" ⛳");

        //if played more than 20 games just return without rank
        long games = Database.getValueFromSQL("""
                SELECT COUNT(*) FROM dicegolf
                WHERE tui = ? AND created_at >= NOW() - INTERVAL '22 hours'
                """, Long.class, userID);
        if (games >= 20) {
            System.out.println(games);
            TwitchChat.sendMessage(sb.toString());
            return;
        }

        Database.executePreparedSQL("""
                INSERT INTO dicegolf (tui, length, sum, game, start) VALUES (?, ?, ?, ?, ?)
                """, userID, length, sum, gameBuilder.toString(), orig);

        if (length == 1) {
            var res = Database.getValueFromSQL("""
                        SELECT count(*) FROM dicegolf WHERE length = 1 and start >= ?
                        """, Long.class, orig);
            sb.append(" HOLE IN ONE! — Rank: ").append(res);
        } else {
            var res = Database.getValueFromSQL("""
                        SELECT count(*) FROM dicegolf WHERE start = ? AND (length > ? or (length = ? and sum >= ?))
                        """, Long.class, orig, length, length, sum);
            var res2 = Database.getValueFromSQL("""
                        SELECT count(*) FROM dicegolf WHERE start = ? AND (sum > ? or (sum = ? and length <= ?))
                        """, Long.class, orig, sum, sum, length);
            sb.append(" Rank ➜ Throws: ").append(res).append(", Sum: ").append(res2);
        }
        TwitchChat.sendMessage(sb.toString());
    }
}
