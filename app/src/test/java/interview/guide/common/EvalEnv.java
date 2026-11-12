package interview.guide.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 测评运行开关与本地配置读取。
 *
 * <p>开关支持两种写法：环境变量（RUN_RAG_EVAL=true）或同名 -D 系统属性（-Drun.rag.eval=true）。
 */
public final class EvalEnv {

  private static Map<String, String> dotEnv;

  private EvalEnv() {
  }

  public static boolean ragEnabled() {
    return enabled("RUN_RAG_EVAL", "run.rag.eval");
  }

  public static boolean voiceContextEnabled() {
    return enabled("RUN_VOICE_CONTEXT_EVAL", "run.voice.context.eval");
  }

  public static boolean enabled(String envName, String sysPropName) {
    return "true".equalsIgnoreCase(get(envName, "false"))
        || "true".equalsIgnoreCase(System.getProperty(sysPropName, "false"));
  }

  public static String get(String key, String defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      value = dotEnv().get(key);
    }
    return value == null || value.isBlank() ? defaultValue : value;
  }

  // Maven 不会把 .env 注入进程环境，测评仍需同一份本地配置，故向上找仓库根 .env 作为回退
  private static synchronized Map<String, String> dotEnv() {
    if (dotEnv == null) {
      dotEnv = readDotEnv();
    }
    return dotEnv;
  }

  private static Map<String, String> readDotEnv() {
    Map<String, String> values = new HashMap<>();
    Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (dir != null) {
      Path file = dir.resolve(".env");
      if (Files.isRegularFile(file)) {
        return parseDotEnv(file, values);
      }
      dir = dir.getParent();
    }
    return values;
  }

  private static Map<String, String> parseDotEnv(Path file, Map<String, String> values) {
    try {
      for (String line : Files.readAllLines(file)) {
        String trimmed = line.trim();
        int separator = trimmed.indexOf('=');
        if (trimmed.isEmpty() || trimmed.startsWith("#") || separator <= 0) {
          continue;
        }
        values.put(trimmed.substring(0, separator).trim(), unquote(trimmed.substring(separator + 1).trim()));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("无法读取 " + file, e);
    }
    return values;
  }

  private static String unquote(String value) {
    boolean quoted = value.length() >= 2
        && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")));
    return quoted ? value.substring(1, value.length() - 1) : value;
  }
}
