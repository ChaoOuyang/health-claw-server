package com.healthclaw.server.agent;

import lombok.Data;

@Data
public class AgentResponse {
    private String reply;
    private String intent;
    private String reasoning;

    public static AgentResponse of(String intent, String reply) {
        AgentResponse r = new AgentResponse();
        r.intent = intent;
        r.reply = reply;
        r.reasoning = "";
        return r;
    }

    public static AgentResponse of(String intent, String reply, String reasoning) {
        AgentResponse r = new AgentResponse();
        r.intent = intent;
        r.reply = reply;
        r.reasoning = reasoning;
        return r;
    }
}
