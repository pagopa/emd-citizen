package it.gov.pagopa.onboarding.citizen.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int pageSize,
        long totalElements,
        long totalPages,
        String nextCursor,
        boolean hasNext) {
}
