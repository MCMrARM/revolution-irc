package io.mrarm.irc.util;

public class FormatUtils {

    private static final String[] BYTE_SIZE_SUFFIXES = new String[] { "B", "KB", "MB", "GB" };

    public static int getByteFormatUnit(long size) {
        int i;
        //noinspection StatementWithEmptyBody
        for (i = 0; size > 1024 && i < BYTE_SIZE_SUFFIXES.length; i++, size /= 1024);
        return i;
    }

    public static String formatByteSize(long size, int unit) {
        size = (size * 10) >> (10 * unit);
        return (size / 10) + (size % 10 != 0 ? ("." + (size % 10)) : "") + BYTE_SIZE_SUFFIXES[unit];
    }

    public static String formatByteSize(long size) {
        size *= 10;
        int i;
        //noinspection StatementWithEmptyBody
        for (i = 0; size > 1024 * 10 && i < BYTE_SIZE_SUFFIXES.length; i++, size /= 1024);
        return (size / 10) + (size % 10 != 0 ? ("." + (size % 10)) : "") + BYTE_SIZE_SUFFIXES[i];
    }

}
