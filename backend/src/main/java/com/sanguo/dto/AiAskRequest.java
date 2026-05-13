package com.sanguo.dto;

import lombok.Data;

@Data
public class AiAskRequest {
    private String questionContent;
    private String conversationId;
    private Integer seedRecordId;
    private Integer maxHistoryTurns;
}

