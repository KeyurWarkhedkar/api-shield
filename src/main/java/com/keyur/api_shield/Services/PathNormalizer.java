package com.keyur.api_shield.Services;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PathNormalizer {

    //list of known static segments that should never be normalized
    private static final Set<String> KNOWN_KEYWORDS = Set.of(
            "me", "profile", "health", "status", "metrics"
    );

    public String normalizeForTraffic(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return path;
        }

        String[] segments = path.split("/");

        for (int i = 0; i < segments.length; i++) {
            if (isLikelyId(segments[i])) {
                segments[i] = "*";
            }
        }

        //reconstruct the normalized path, skipping empty segments
        StringBuilder normalized = new StringBuilder();
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                normalized.append("/").append(segment);
            }
        }

        return normalized.toString();
    }

    private boolean isLikelyId(String segment) {
        if (segment == null || segment.isEmpty()) return false;

        //numeric IDs
        if (segment.matches("\\d+")) return true;

        //UUIDs
        if (segment.matches("[a-fA-F0-9\\-]{36}")) return true;

        //long alphanumeric IDs (likely custom IDs)
        if (segment.matches("[a-zA-Z0-9]{10,}")) return true;

        //known static keywords are NOT IDs
        if (KNOWN_KEYWORDS.contains(segment)) return false;

        return false;
    }
}
