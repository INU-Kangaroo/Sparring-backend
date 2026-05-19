package com.kangaroo.sparring.global.support;

public final class LogMaskingSupport {

    private LogMaskingSupport() {
    }

    public static String maskEmail(String email) {
        if (email == null) {
            return "null";
        }

        String normalized = email.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return maskFallback(normalized);
        }

        String local = normalized.substring(0, atIndex);
        String domain = normalized.substring(atIndex + 1);
        return maskLocal(local) + "@" + maskDomain(domain);
    }

    private static String maskLocal(String local) {
        if (local.length() <= 1) {
            return "*";
        }
        if (local.length() == 2) {
            return local.charAt(0) + "*";
        }
        return local.substring(0, 2) + "***";
    }

    private static String maskDomain(String domain) {
        int dotIndex = domain.indexOf('.');
        String head = dotIndex > 0 ? domain.substring(0, dotIndex) : domain;
        String tail = dotIndex > 0 ? domain.substring(dotIndex) : "";

        if (head.isEmpty()) {
            return "***" + tail;
        }
        if (head.length() == 1) {
            return "*" + tail;
        }
        return head.charAt(0) + "***" + tail;
    }

    private static String maskFallback(String value) {
        if (value.length() <= 1) {
            return "*";
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.substring(0, 2) + "***";
    }
}
