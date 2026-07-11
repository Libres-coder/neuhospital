package com.neusoft.hospital.common;

/**
 * GB 11643-1999 / GB/T 17760-1999 Chinese resident identity card check-digit
 * calculator. Validates the format of an 18-digit ID number — region prefix
 * (first 6 digits), birthday (digits 7–14), and the trailing check digit
 * computed from a weight × position mod-11 lookup table.
 *
 * <p>This is a STRUCTURAL check: it confirms the number was correctly
 * transcribed (not corrupted and not faked by trivially generating digits
 * 1–17 and computing the 18th). It does NOT verify that the name + ID
 * pair actually belongs to a real person — that requires a third-party
 * gateway (公安部接口 / 银联 eID / 支付宝实名 etc.). Treat this as a
 * "well-formed" gate, not identity proof.</p>
 */
public final class IdCardValidator {

    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECKSUM = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdCardValidator() {}

    /** Returns true if {@code id} is a structurally valid 18-digit ID number. */
    public static boolean isValid(String id) {
        if (id == null || id.length() != 18) return false;
        // first 17 must be digits, last may be digit or X/x
        for (int i = 0; i < 17; i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        char last = id.charAt(17);
        if (!((last >= '0' && last <= '9') || last == 'X' || last == 'x')) return false;

        // birthday check (positions 6..13, 1-based; 0-based: 6..13 inclusive)
        try {
            int year = Integer.parseInt(id.substring(6, 10));
            int month = Integer.parseInt(id.substring(10, 12));
            int day = Integer.parseInt(id.substring(12, 14));
            if (year < 1900 || year > 2100) return false;
            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;
            // rough days-per-month check; ignore leap-year edge for reject — Java will catch it
            int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            int dim = days[month - 1];
            if (month == 2 && isLeap(year)) dim = 29;
            if (day > dim) return false;
        } catch (NumberFormatException e) {
            return false;
        }

        // checksum
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * WEIGHT[i];
        }
        char expected = CHECKSUM[sum % 11];
        char actual = Character.toUpperCase(id.charAt(17));
        return expected == actual;
    }

    private static boolean isLeap(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}
