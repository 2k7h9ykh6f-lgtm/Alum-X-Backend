package com.opencode.alumxbackend.jobposts.dto;

/**
 * Supported ordering strategies for the post feed.
 *
 * <ul>
 *     <li>{@link #LATEST} - newest posts first (default, backward compatible).</li>
 *     <li>{@link #MOST_LIKED} - posts with the most likes first.</li>
 *     <li>{@link #MOST_COMMENTED} - posts with the most comments first.</li>
 * </ul>
 */
public enum PostSortType {
    LATEST,
    MOST_LIKED,
    MOST_COMMENTED;

    /**
     * Resolves a raw sort value (e.g. a query parameter) to a {@link PostSortType}.
     * Accepts both camelCase ("mostLiked") and snake_case ("most_liked"). Unknown,
     * blank, or {@code null} values fall back to {@link #LATEST} so callers that do
     * not specify a sort keep the original behaviour.
     */
    public static PostSortType fromString(String value) {
        if (value == null) {
            return LATEST;
        }
        return switch (value.trim().toLowerCase()) {
            case "mostliked", "most_liked" -> MOST_LIKED;
            case "mostcommented", "most_commented" -> MOST_COMMENTED;
            default -> LATEST;
        };
    }
}
