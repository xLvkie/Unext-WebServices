package com.nextworks.unextwebservices.agent.rag;

import com.nextworks.unextwebservices.agent.tools.AgentRuleGuard;
import com.nextworks.unextwebservices.entity.User;
import com.nextworks.unextwebservices.service.RagContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnextRagAdvisor implements CallAdvisor {

    public static final String USER_CONTEXT_PARAM = "unext_user";

    private final RagContextService ragContextService;
    private final AgentRuleGuard agentRuleGuard;

    @Override
    public String getName() {
        return "UnextRagAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        User user = resolveUser(request);
        UserMessage userMessage = request.prompt().getUserMessage();
        String userQuery = userMessage != null ? userMessage.getText() : "";

        if (user == null) {
            return chain.nextCall(request);
        }

        String ragContext = ragContextService.retrieveContext(user, userQuery);
        Prompt augmentedPrompt = request.prompt().augmentSystemMessage(sys -> {
            String baseSystem = (sys != null && sys.getText() != null && !sys.getText().isBlank())
                    ? sys.getText() + "\n\n"
                    : "";
            return new SystemMessage(baseSystem + ragContext);
        });

        ChatClientRequest updatedRequest = request.mutate()
                .prompt(augmentedPrompt)
                .build();

        return chain.nextCall(updatedRequest);
    }

    private User resolveUser(ChatClientRequest request) {
        Object ctxUser = request.context().get(USER_CONTEXT_PARAM);
        if (ctxUser instanceof User user) {
            return user;
        }
        return agentRuleGuard.resolveCurrentUserOptional();
    }
}
