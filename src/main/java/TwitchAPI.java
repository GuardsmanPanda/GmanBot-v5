import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

public class TwitchAPI {
    private static final String prefix = "https://api.twitch.tv/helix/";
    private static Instant  tokenExpires = Instant.MIN;
    private static String twitchToken = "";

    static {
        Pair<String, Timestamp> res = Database.getPairFromSql("""
            SELECT  access_token, expires_at FROM oauth_tokens WHERE name = 'gbob_twitch' 
        """);
        twitchToken = res.first;
        tokenExpires = res.second.toInstant();
    }

    public static void main(String[] args) {
        System.out.println(followAge(26797258));
    }

    public static long twitchIdFromName(String name) {
        var resp = APIQuery("users?login=" + name);
        return Long.parseLong(resp.get("data").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString());
    }

    public static Period followAge(long tui) {
        var resp = APIQuery("users/follows?to_id=" + Config.env("guardsmanbob_tui") + "&from_id=" + tui);
        if (resp.get("total").getAsInt() != 1) return null;
        var data = resp.get("data").getAsJsonArray().get(0).getAsJsonObject();
        return Period.between(Instant.parse(data.get("followed_at").getAsString()).atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now());
    }

    public static JsonObject APIQuery(String query) {
        var request = HttpRequest.newBuilder(URI.create(prefix + query))
                .header("Authorization" , "Bearer " + getBobsTwitchAPIToken())
                .header("Client-ID", Config.env("twitch_client_id"))
                .GET()
                .build();
        return Common.jsonObjectFromRequest(request);
    }

    private static String getBobsTwitchAPIToken() {
        if (tokenExpires.isBefore(Instant.now())) {
            var x = new HashMap<String,String>() {{
                put("grant_type", "refresh_token");
                put("client_id", Config.env("twitch_client_id"));
                put("client_secret", Config.env("twitch_client_secret"));
                put("refresh_token", Database.getValueFromSQL("SELECT refresh_token from oauth_tokens WHERE name = 'gbob_twitch'", String.class));
            }};
            var request = HttpRequest.newBuilder(URI.create("https://id.twitch.tv/oauth2/token"))
                    .POST(Common.postDataFromMap(x))
                    .build();
            var response = Common.jsonObjectFromRequest(request);
            if (response != null) {
                int expire = response.get("expires_in").getAsInt();
                tokenExpires = Instant.now().plus(expire-600, ChronoUnit.SECONDS);
                twitchToken = response.get("access_token").getAsString();
                String rt = response.get("refresh_token").getAsString();
                Database.executePreparedSQL("""
                        UPDATE oauth_tokens SET access_token = ?, refresh_token = ?, expires_at = ?
                        WHERE name = 'gbob_twitch'
                        """, twitchToken, rt, Timestamp.from(tokenExpires));
            }
        }
        return twitchToken;
    }
}
