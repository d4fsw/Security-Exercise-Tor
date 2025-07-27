import java.util.HashMap;
import java.util.Map;

public class Command {

    private static final String SEPARATOR = "-=%&%=-";
    private static final String VALUE_SEPARATOR = "%=%";

    public static final String SEND = "Send";
    public static final String RECEIVE = "Start Receive";
    public static final String LOGIN = "Login";
    public static final String REGISTER = "Register";

    public static final String USERNAME = "U";
    public static final String PASSWORD = "P";
    public static final String LENGTH = "L";
    public static final String ERROR = "E";
    public static final String SUCCESS = "S";
    public static final String FILE = "F";

    private String type;
    private HashMap<String, String> params = new HashMap<>();

    Command(String type) {
        this.type = type;
    }

    public Command addParameter(String name, String value) {
        params.put(name, value);
        return this;
    }

    public String getParameter(String name) {
        return params.get(name);
    }

    public String getType() {
        return type;
    }

    public String toMessage() {
        StringBuilder builder = new StringBuilder(type);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.append(SEPARATOR).append(entry.getKey()).append(VALUE_SEPARATOR).append(entry.getValue());
        }
        return builder.toString();
    }

    public static Command fromMessage( String message) {
        try {
            String[] parts = message.split(SEPARATOR);
            if (parts.length < 1)
                return null;
            Command reply = new Command(parts[0]);
            for (int idx = 1; idx < parts.length; idx++) {
                String part = parts[idx];
                if (!part.contains(VALUE_SEPARATOR))
                    continue;
                String key = part.substring(0, part.indexOf(VALUE_SEPARATOR));
                String value = part.substring(part.indexOf(VALUE_SEPARATOR) + VALUE_SEPARATOR.length());
                reply.addParameter(key, value);
            }
            return reply;
        } catch (Exception ignored) {
            return null;
        }
    }
}
