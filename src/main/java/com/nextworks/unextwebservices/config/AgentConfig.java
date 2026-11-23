package com.nextworks.unextwebservices.config;

import com.nextworks.unextwebservices.agent.rag.UnextRagAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

@Configuration
public class AgentConfig {

    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Primary
    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            UnextRagAdvisor unextRagAdvisor,
            @Value("classpath:prompts/system-prompt.md") Resource systemPrompt
    ) {
        return builder.clone()
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        unextRagAdvisor
                )
                .build();
    }

    @Bean("statelessChatClient")
    ChatClient statelessChatClient(
            ChatClient.Builder builder,
            UnextRagAdvisor unextRagAdvisor,
            @Value("classpath:prompts/system-prompt.md") Resource systemPrompt
    ) {
        return builder.clone()
                .defaultSystem(systemPrompt)
                .defaultAdvisors(unextRagAdvisor)
                .build();
    }
}
