package com.eventpurchase.project.global.common.response;

import java.util.List;

public record CursorResponse<T>(
        List<T> content,
        Long nextLastId,
        boolean hasNext
) {}