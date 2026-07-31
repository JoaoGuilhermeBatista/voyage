package luvoygroup.voyage.features.admin.dto;

import java.util.List;

public record GeneratedContent(
    String name,
    String tagline,
    String description,
    String signature,
    List<String> tags
) {}
