public class StringUtils {
    public static boolean isNullOrBlank(String string) {
        return string == null || string.trim().length() == 0;
    }
}
