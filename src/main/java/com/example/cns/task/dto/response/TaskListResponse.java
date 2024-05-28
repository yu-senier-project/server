package com.example.cns.task.dto.response;

import java.util.List;

public record TaskListResponse(
        String owner,
        List<TaskResponse> todoList
) {
    public TaskListResponse(String owner) {
        this(owner, null);
    }
}
