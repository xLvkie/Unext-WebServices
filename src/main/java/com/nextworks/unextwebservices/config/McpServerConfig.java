package com.nextworks.unextwebservices.config;

import com.nextworks.unextwebservices.agent.tools.InstitutionTools;
import com.nextworks.unextwebservices.agent.tools.PostulantTools;
import com.nextworks.unextwebservices.agent.tools.RecruiterTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    ToolCallbackProvider unextMcpToolCallbackProvider(
            PostulantTools postulantTools,
            RecruiterTools recruiterTools,
            InstitutionTools institutionTools
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(postulantTools, recruiterTools, institutionTools)
                .build();
    }
}
