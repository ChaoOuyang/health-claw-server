package com.healthclaw.server.agent;

import lombok.Data;

@Data
public class AgentResponse {
    private String reply;
    private String intent;

    public static AgentResponse of(String intent, String reply) {
        AgentResponse r = new AgentResponse();
        r.intent = intent;
        r.reply = reply;
        return r;
    }
}
