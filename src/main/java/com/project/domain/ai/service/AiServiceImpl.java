package com.project.domain.ai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String DEFAULT_MODEL = "gpt-5-nano";
    private static final String CONNECTION_TEST_PROMPT =
            "연결 테스트입니다. 반드시 'OpenAI 연결 성공'이라고만 답변하세요.";
    private static final String CHAT_INSTRUCTION =
            "당신은 결혼 준비와 공동 예산 관리를 돕는 두리모아 AI입니다. "
                    + "사용자의 질문에 한국어로 친절하고 간결하게 답변하세요. "
                    + "짧은 인사에도 반드시 자연스러운 최종 답변을 작성하세요.\n\n사용자 질문:\n";
    private static final String CHAT_RETRY_INSTRUCTION =
            "\n\n사용자에게 보여줄 최종 답변 텍스트를 반드시 작성하세요.";
    private static final String CHAT_FALLBACK_RESPONSE =
            "답변을 생성하지 못했어요. 잠시 후 다시 질문해 주세요.";
    private static final Long CONNECTION_TEST_MAX_OUTPUT_TOKENS = 256L;
    private static final Long CHAT_MAX_OUTPUT_TOKENS = 1024L;
    private static final Integer CHAT_MAX_ATTEMPTS = 2;
    private static final Reasoning CHAT_REASONING = Reasoning.builder()
            .effort(ReasoningEffort.MINIMAL)
            .build();

    private final ObjectProvider<OpenAIClient> openAIClientProvider;

    @Override
    public AiConnectionTestResponseDTO testConnection() {
        String model = getEnvironmentValue("OPENAI_MODEL", DEFAULT_MODEL);
        String teamCode = getEnvironmentValue("TEAM_CODE", "NOT_CONFIGURED");

        log.info("[teamCode={}] OpenAI connection test started: model={}", teamCode, model);

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(model)
                .input(CONNECTION_TEST_PROMPT)
                .maxOutputTokens(CONNECTION_TEST_MAX_OUTPUT_TOKENS)
                .build();

        Response response = openAIClientProvider.getObject()
                .responses()
                .create(params);
        String responseText = extractOutputText(response);

        if (responseText == null || responseText.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }

        log.info("[teamCode={}] OpenAI connection test completed: model={}", teamCode, model);
        return AiConnectionTestResponseDTO.builder()
                .model(model)
                .response(responseText)
                .build();
    }

    @Override
    public AiChatResponseDTO chat(String message) {
        String model = getEnvironmentValue("OPENAI_MODEL", DEFAULT_MODEL);
        String teamCode = getEnvironmentValue("TEAM_CODE", "NOT_CONFIGURED");
        long startedAt = System.nanoTime();

        log.info("[teamCode={}] OpenAI chat request started: model={}", teamCode, model);

        try {
            for (int attempt = 1; attempt <= CHAT_MAX_ATTEMPTS; attempt++) {
                long attemptStartedAt = System.nanoTime();
                ResponseCreateParams params = createChatParams(model, message, attempt);
                Response response = openAIClientProvider.getObject()
                        .responses()
                        .create(params);
                long attemptElapsedMillis =
                        (System.nanoTime() - attemptStartedAt) / 1_000_000L;

                log.info(
                        "[teamCode={}] OpenAI chat response received: model={}, attempt={}, "
                                + "outputCount={}, elapsedMillis={}",
                        teamCode,
                        model,
                        attempt,
                        response.output().size(),
                        attemptElapsedMillis);

                String responseText = extractOutputText(response);
                boolean hasText = responseText != null && !responseText.trim().isEmpty();
                log.info(
                        "[teamCode={}] OpenAI chat text extracted: model={}, attempt={}, "
                                + "hasText={}",
                        teamCode,
                        model,
                        attempt,
                        hasText);

                if (hasText) {
                    log.info(
                            "[teamCode={}] OpenAI chat request completed: model={}, attempt={}",
                            teamCode,
                            model,
                            attempt);
                    return AiChatResponseDTO.builder()
                            .model(model)
                            .response(responseText)
                            .build();
                }

                log.warn(
                        "[teamCode={}] OpenAI chat output text missing: model={}, attempt={}, "
                                + "willRetry={}",
                        teamCode,
                        model,
                        attempt,
                        attempt < CHAT_MAX_ATTEMPTS);
            }

            log.warn(
                    "[teamCode={}] OpenAI chat fallback returned after retries: model={}",
                    teamCode,
                    model);
            return AiChatResponseDTO.builder()
                    .model(model)
                    .response(CHAT_FALLBACK_RESPONSE)
                    .build();
        } catch (RuntimeException exception) {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            log.error(
                    "[teamCode={}] OpenAI chat request failed: model={}, exceptionType={}, "
                            + "elapsedMillis={}",
                    teamCode,
                    model,
                    exception.getClass().getName(),
                    elapsedMillis,
                    exception);
            throw exception;
        }
    }

    private ResponseCreateParams createChatParams(String model, String message, int attempt) {
        String input = CHAT_INSTRUCTION + message.trim();
        if (attempt > 1) {
            input += CHAT_RETRY_INSTRUCTION;
        }

        return ResponseCreateParams.builder()
                .model(model)
                .input(input)
                .reasoning(CHAT_REASONING)
                .maxOutputTokens(CHAT_MAX_OUTPUT_TOKENS)
                .build();
    }

    private String extractOutputText(Response response) {
        return response.output().stream()
                .filter(outputItem -> outputItem.isMessage())
                .flatMap(outputItem -> outputItem.asMessage().content().stream())
                .filter(content -> content.isOutputText())
                .map(content -> content.asOutputText().text())
                .reduce("", String::concat);
    }

    private String getEnvironmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
