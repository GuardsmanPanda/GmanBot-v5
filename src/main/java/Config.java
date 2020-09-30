import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class Config {
    private static final HashMap<String, String> envMap = new HashMap<>(), fileEnvMap = new HashMap<>();

    static {
        try {
            for (String line : Files.readAllLines(Path.of(".env"))) {
                if (line.charAt(0) == '#') continue;
                int idx = line.indexOf('=');
                fileEnvMap.put(line.substring(0, idx), line.substring(idx+1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String env(String key) {
        return envMap.computeIfAbsent(key.toLowerCase(), k -> Database.getValueFromSQL("SELECT value FROM env WHERE key = ?",  String.class, k));
    }

    public static String file_env(String key) {
        return fileEnvMap.get(key);
    }
}
