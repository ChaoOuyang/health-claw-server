package com.healthclaw.server.agent;

import lombok.Data;

@Data
public class AgentRequest {
    private String message;
    private String date; // yyyy-MM-dd，不传则用今天
}
