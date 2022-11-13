package Utility;

public class StringUtility {
    public static String normalize(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_')
                continue;
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
