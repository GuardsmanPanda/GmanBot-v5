import javax.xml.crypto.Data;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;


public class DatabaseImporter {

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        //heart_import();
        testCode();
    }

    private static void testCode() throws SQLException {
        var res = Database.getRowSetFromSQL("SELECT name, id FROM tuis WHERE name = 'awrod'");
        res.next();
        System.out.println(res.getString(1) + " " + res.getLong(2));
    }

    private static void dataImport() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.impl.jdbc.EmbedConnection");
        Connection connection = DriverManager.getConnection("jdbc:derby:BobsDB;create=true");
        ResultSet rows =  connection.createStatement().executeQuery("SELECT * FROM twitchchatusers");

        while (rows.next()) {
            long tui = Long.parseLong(rows.getString(1));
            String name = rows.getString(2);
            String flag = rows.getString("flag");
            boolean subbed = rows.getBoolean("hassubscribed");
            System.out.println(tui + " -> " + name + " -> " + flag + " -> " + subbed);

            Database.executePreparedSQL("""
                    INSERT INTO tuis (id, name, have_subbed, flag) VALUES (?, ?, ?, ?),
                    ON CONFLICT (id) DO UPDATE
                    SET 
                        have_subbed = tuis.have_subbed OR EXCLUDED.have_subbed,
                        flag = EXCLUDED.flag
                    """, tui, name, subbed, flag);
            Database.executePreparedSQL("""
                    INSERT INTO twitch_name_to_tui (twitch_name, tui) VALUES (?, ?)
                    ON CONFLICT (twitch_name) DO NOTHING
                    """, name.toLowerCase(), tui);
        }
    }

    private static void heart_import() throws ClassNotFoundException, SQLException {
        Class.forName("org.apache.derby.impl.jdbc.EmbedConnection");
        Connection connection = DriverManager.getConnection("jdbc:derby:BobsDB;create=true");
        ResultSet rows = connection.createStatement().executeQuery("SELECT * FROM twitchchatusers");
        while (rows.next()) {
            long tui = Long.parseLong(rows.getString("twitchuserid"));
            int chat_lines = rows.getInt("chatlines");
            int active_hours = rows.getInt("activehours");
            int idle_hours =rows.getInt("idlehours");
            int bob_coins = rows.getInt("bobcoins");
            String welcome_message = rows.getString("welcomemessage");
            if (welcome_message.equals("none")) welcome_message = null;
            Database.executePreparedSQL("""
                    UPDATE tuis SET
                        chat_lines = ?, active_hours = ?, idle_hours = ?, bob_coins = ?, welcome_message = ?
                        WHERE id = ?
                    """, chat_lines, active_hours, idle_hours, bob_coins, welcome_message, tui);
            System.out.println(tui + " -> " + chat_lines + " -> " + active_hours + " -> " + idle_hours + " -> " + bob_coins + " -> " + welcome_message);
        }
    }


}
