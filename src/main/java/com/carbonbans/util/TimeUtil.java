package com.carbonbans.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)(mo|y|w|d|h|m|s)");

    // returns -1 = permanent, -2 = invalid/not a duration, else millis
    public static long parse(String input) {
        if (input == null) return -2;
        if (input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent") || input.equals("-1")) return -1;

        Matcher m = PATTERN.matcher(input.toLowerCase());
        long total = 0;
        boolean found = false;

        while (m.find()) {
            found = true;
            long val = Long.parseLong(m.group(1));
            switch (m.group(2)) {
                case "s": total += val * 1000L; break;
                case "m": total += val * 60_000L; break;
                case "h": total += val * 3_600_000L; break;
                case "d": total += val * 86_400_000L; break;
                case "w": total += val * 7 * 86_400_000L; break;
                case "mo": total += val * 30L * 86_400_000L; break;
                case "y": total += val * 365L * 86_400_000L; break;
            }
        }
        return found ? total : -2;
    }

    public static String format(long millis) {
        if (millis < 0) return "Permanent";
        long seconds = millis / 1000;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}