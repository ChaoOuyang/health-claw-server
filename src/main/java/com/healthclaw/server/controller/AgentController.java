package com.healthclaw.server.controller;

import com.healthclaw.server.agent.AgentRequest;
import com.healthclaw.server.agent.AgentResponse;
import com.healthclaw.server.agent.HermesAgentService;
import com.healthclaw.server.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final HermesAgentService agentService;

    public AgentController(HermesAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentResponse> chat(@RequestBody AgentRequest req) {
        try {
            AgentResponse resp = agentService.process(req.getMessage(), req.getDate());
            return ApiResponse.ok(resp);
        } catch (Exception e) {
            return ApiResponse.error("Agent 处理失败: " + e.getMessage());
        }
    }
}
