package com.freshmart.smartstock.config;

import com.freshmart.smartstock.llm.HttpLlmService;
import com.freshmart.smartstock.llm.LlmService;
import com.freshmart.smartstock.llm.MockLlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * No API key configured -> MockLlmService (same LlmService interface), so the app and tests
 * always run without secrets. With a key -> real HTTP implementation.
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmService llmService(AppProperties props) {
        AppProperties.Llm llm = props.llm();
        if (llm == null || !llm.hasApiKey()) {
            log.warn("app.llm.api-key is empty -> using MockLlmService");
            return new MockLlmService();
        }
        log.info("Using HttpLlmService (model={})", llm.model());
        return new HttpLlmService(llm);
    }
}
