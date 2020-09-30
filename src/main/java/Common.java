import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Common {
    private static final HttpClient httpClient;
    private static final Random random = new Random();

    static {
        httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    public static int randomInt(int bound) {
        return random.nextInt(bound);
    }

    public static JsonObject jsonObjectFromRequest(HttpRequest request) { //todo: improve logging of errors.
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                throw new RuntimeException("Error on HTTP: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static HttpRequest.BodyPublisher postDataFromMap(Map<String, String> queryMap) {
        var enc = StandardCharsets.UTF_8;
        return HttpRequest.BodyPublishers.ofString(queryMap.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), enc) + "=" + URLEncoder.encode(e.getValue(), enc))
                .collect(Collectors.joining("&")));
    }


    public static String prettyPrintPeriod(Period period) {
        List<String> timeStrings = new ArrayList<>(3);
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        if (years > 0) timeStrings.add(years + ((years > 1) ? " Years": " Year"));
        if (months > 0) timeStrings.add(months + ((months > 1) ? " Months": " Month"));
        if (days > 0) timeStrings.add(days + ((days > 1) ? " Days": " Day"));

        return String.join(", ", timeStrings).replaceAll(", (?!.*,)"," and ");
    }
}
