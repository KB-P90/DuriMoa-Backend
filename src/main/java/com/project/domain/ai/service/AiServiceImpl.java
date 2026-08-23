package com.project.domain.ai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.project.domain.ai.domain.AiAction;
import com.project.domain.ai.domain.AiBudgetType;
import com.project.domain.ai.domain.AiIntent;
import com.project.domain.ai.dto.AiBudgetTypeModelResponseDTO;
import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;
import com.project.domain.ai.dto.AiIntentModelResponseDTO;
import com.project.domain.ai.dto.AiSpendingReportSummaryModelResponseDTO;
import com.project.domain.ai.model.AiSpendingReportSummaryContext;
import com.project.domain.ai.model.AiWeddingBudgetSelectionContext;
import java.util.List;
import java.util.stream.Collectors;
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
                    + "사용자 입력을 지원 기능 중 하나로 분류하세요. "
                    + "결혼 예산, 소비 내역 기반 결혼 비용, 예식장·스튜디오·드레스·메이크업 "
                    + "예산 요청은 WEDDING_BUDGET_RECOMMENDATION입니다. "
                    + "가입 이후 지출 흐름, 소비 분석, 소비 리포트, 카테고리·요일별 지출 분석 "
                    + "요청은 SPENDING_REPORT입니다. 인사는 GREETING입니다. "
                    + "그 외 요청은 UNKNOWN입니다. GREETING이면 결혼 예산 추천과 소비 리포트 "
                    + "기능을 자연스럽게 안내하고, UNKNOWN이면 현재 지원 기능을 간결하게 안내하세요. "
                    + "한국어로 한두 문장만 작성하세요.";
    private static final String BUDGET_SELECTION_INSTRUCTION =
            "당신은 예비부부의 결혼 예산 유형을 추천하는 두리모아 AI입니다. "
                    + "eligibleBudgetTypes에 포함된 값만 선택해야 합니다. "
                    + "균형형을 감당할 수 있고 최근 저축이 안정적이면 BALANCED를 우선하고, "
                    + "재정 여유와 저축률이 충분하며 FLEX가 허용된 경우에만 FLEX를 고려하세요. "
                    + "reason은 제공된 집계 수치만 근거로 한국어 한두 문장으로 작성하세요.";
    private static final String SPENDING_REPORT_SUMMARY_INSTRUCTION =
            "당신은 개인 소비를 분석하는 두리모아 AI입니다. 제공된 집계 수치만 사용해서 "
                    + "한국어 2~3문장으로 요약하세요. 첫 문장은 전체 지출 흐름, 두 번째 문장은 "
                    + "카테고리나 평일·주말 패턴, 필요하면 마지막 문장은 실천 팁을 작성하세요. "
                    + "사용 목적을 단정하지 말고 '그런 경향이 보여요'처럼 표현하세요. "
                    + "입력에 없는 거래 메모, 계좌, 카드 정보는 추측하거나 언급하지 마세요.";
    private static final String CHAT_FALLBACK_RESPONSE =
            "현재는 소비 내역을 바탕으로 한 결혼 예산 추천을 도와드릴 수 있어요.";
    private static final String UNKNOWN_RESPONSE =
            "아직 지원하지 않는 요청이에요.\n"
                    + "현재 두리모아 AI는 다음 기능을 지원해요.\n\n"
                    + "• 맞춤 결혼 예산 추천\n"
                    + "• 결혼 준비 소비 리포트\n"
                    + "• 커플 저축 플랜\n\n"
                    + "지원 기능 중 필요한 도움을 말씀해 주세요.";
    private static final String BUDGET_REASON_FALLBACK =
            "최근 3개월의 수입, 지출, 저축 여력과 지역별 결혼 비용 통계를 반영했어요.";
    private static final Long CONNECTION_TEST_MAX_OUTPUT_TOKENS = 256L;
    private static final Long STRUCTURED_MAX_OUTPUT_TOKENS = 512L;
    private static final Reasoning MINIMAL_REASONING = Reasoning.builder()
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
        log.info("[teamCode={}] OpenAI intent classification started: model={}", teamCode, model);

        try {
            StructuredResponseCreateParams<AiIntentModelResponseDTO> params =
                    ResponseCreateParams.builder()
                            .model(model)
                            .input(message.trim())
                            .instructions(CHAT_INSTRUCTION)
                            .reasoning(MINIMAL_REASONING)
                            .maxOutputTokens(STRUCTURED_MAX_OUTPUT_TOKENS)
                            .text(AiIntentModelResponseDTO.class)
                            .build();

            StructuredResponse<AiIntentModelResponseDTO> response = openAIClientProvider
                    .getObject()
                    .responses()
                    .create(params);
            AiIntentModelResponseDTO result = extractStructuredOutput(response);
            AiIntent intent = result == null || result.getIntent() == null
                    ? AiIntent.UNKNOWN
                    : result.getIntent();
            String responseText = intent == AiIntent.UNKNOWN
                    ? UNKNOWN_RESPONSE
                    : resolveResponseText(result);

            log.info(
                    "[teamCode={}] OpenAI intent classification completed: model={}, intent={}",
                    teamCode,
                    model,
                    intent);
            return AiChatResponseDTO.builder()
                    .model(model)
                    .response(responseText)
                    .intent(intent)
                    .action(resolveAction(intent))
                    .build();
        } catch (RuntimeException exception) {
            log.error(
                    "[teamCode={}] OpenAI intent classification failed: model={}, exceptionType={}",
                    teamCode,
                    model,
                    exception.getClass().getName(),
                    exception);
            return AiChatResponseDTO.builder()
                    .model(model)
                    .response(CHAT_FALLBACK_RESPONSE)
                    .intent(AiIntent.UNKNOWN)
                    .action(null)
                    .build();
        }
    }

    @Override
    public AiBudgetTypeModelResponseDTO selectWeddingBudgetType(
            AiWeddingBudgetSelectionContext context
    ) {
        List<AiBudgetType> eligibleTypes = context.getEligibleBudgetTypes();
        AiBudgetType fallbackType = resolveFallbackBudgetType(eligibleTypes);
        if (eligibleTypes == null || eligibleTypes.size() <= 1) {
            return new AiBudgetTypeModelResponseDTO(fallbackType, BUDGET_REASON_FALLBACK);
        }

        String model = getEnvironmentValue("OPENAI_MODEL", DEFAULT_MODEL);
        String input = buildBudgetSelectionInput(context);
        try {
            StructuredResponseCreateParams<AiBudgetTypeModelResponseDTO> params =
                    ResponseCreateParams.builder()
                            .model(model)
                            .input(input)
                            .instructions(BUDGET_SELECTION_INSTRUCTION)
                            .reasoning(MINIMAL_REASONING)
                            .maxOutputTokens(STRUCTURED_MAX_OUTPUT_TOKENS)
                            .text(AiBudgetTypeModelResponseDTO.class)
                            .build();
            StructuredResponse<AiBudgetTypeModelResponseDTO> response = openAIClientProvider
                    .getObject()
                    .responses()
                    .create(params);
            AiBudgetTypeModelResponseDTO result = extractStructuredOutput(response);

            if (result == null
                    || result.getBudgetType() == null
                    || !eligibleTypes.contains(result.getBudgetType())) {
                return new AiBudgetTypeModelResponseDTO(fallbackType, BUDGET_REASON_FALLBACK);
            }
            if (isBlank(result.getReason())) {
                result.setReason(BUDGET_REASON_FALLBACK);
            }
            return result;
        } catch (RuntimeException exception) {
            log.error(
                    "OpenAI wedding budget type selection failed: exceptionType={}",
                    exception.getClass().getName(),
                    exception);
            return new AiBudgetTypeModelResponseDTO(fallbackType, BUDGET_REASON_FALLBACK);
        }
    }

    @Override
    public AiSpendingReportSummaryModelResponseDTO summarizeSpendingReport(
            AiSpendingReportSummaryContext context
    ) {
        String model = getEnvironmentValue("OPENAI_MODEL", DEFAULT_MODEL);
        try {
            StructuredResponseCreateParams<AiSpendingReportSummaryModelResponseDTO> params =
                    ResponseCreateParams.builder()
                            .model(model)
                            .input(buildSpendingReportSummaryInput(context))
                            .instructions(SPENDING_REPORT_SUMMARY_INSTRUCTION)
                            .reasoning(MINIMAL_REASONING)
                            .maxOutputTokens(STRUCTURED_MAX_OUTPUT_TOKENS)
                            .text(AiSpendingReportSummaryModelResponseDTO.class)
                            .build();
            StructuredResponse<AiSpendingReportSummaryModelResponseDTO> response =
                    openAIClientProvider.getObject().responses().create(params);
            AiSpendingReportSummaryModelResponseDTO result = extractStructuredOutput(response);
            if (result == null || isBlank(result.getSummary())) {
                return new AiSpendingReportSummaryModelResponseDTO(null);
            }
            return result;
        } catch (RuntimeException exception) {
            log.error(
                    "OpenAI spending report summary failed: exceptionType={}",
                    exception.getClass().getName(),
                    exception);
            return new AiSpendingReportSummaryModelResponseDTO(null);
        }
    }

    private AiAction resolveAction(AiIntent intent) {
        if (intent == AiIntent.WEDDING_BUDGET_RECOMMENDATION) {
            return AiAction.START_WEDDING_BUDGET_FLOW;
        }
        if (intent == AiIntent.SPENDING_REPORT) {
            return AiAction.GENERATE_SPENDING_REPORT;
        }
        return null;
    }

    private String resolveResponseText(AiIntentModelResponseDTO result) {
        return result == null || isBlank(result.getResponse())
                ? CHAT_FALLBACK_RESPONSE
                : result.getResponse().trim();
    }

    private AiBudgetType resolveFallbackBudgetType(List<AiBudgetType> eligibleTypes) {
        if (eligibleTypes != null && eligibleTypes.contains(AiBudgetType.BALANCED)) {
            return AiBudgetType.BALANCED;
        }
        if (eligibleTypes != null && eligibleTypes.contains(AiBudgetType.FLEX)) {
            return AiBudgetType.FLEX;
        }
        return AiBudgetType.SAVING;
    }

    private String buildBudgetSelectionInput(AiWeddingBudgetSelectionContext context) {
        String eligibleTypes = context.getEligibleBudgetTypes().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        return "eligibleBudgetTypes=" + eligibleTypes
                + "\nmedianMonthlyIncome=" + context.getMedianMonthlyIncome()
                + "\nmedianMonthlyExpense=" + context.getMedianMonthlyExpense()
                + "\nmedianMonthlySaving=" + context.getMedianMonthlySaving()
                + "\nsustainableMonthlySaving=" + context.getSustainableMonthlySaving()
                + "\naffordableTotalBudget=" + context.getAffordableTotalBudget()
                + "\nmonthsUntilWedding=" + context.getMonthsUntilWedding()
                + "\nregion=" + context.getRegion();
    }

    private String buildSpendingReportSummaryInput(AiSpendingReportSummaryContext context) {
        return "totalExpense=" + context.getTotalExpense()
                + "\naverageMonthlyExpense=" + context.getAverageMonthlyExpense()
                + "\ntransactionCount=" + context.getTransactionCount()
                + "\ntopCategoryName=" + context.getTopCategoryName()
                + "\ntopCategoryRatio=" + context.getTopCategoryRatio()
                + "\nrecentTrendRate=" + context.getRecentTrendRate()
                + "\npeakDayName=" + context.getPeakDayName()
                + "\nweekendToWeekdayRatio=" + context.getWeekendToWeekdayRatio()
                + "\ntopWeekendCategoryName=" + context.getTopWeekendCategoryName();
    }

    private <T> T extractStructuredOutput(StructuredResponse<T> response) {
        return response.output().stream()
                .filter(outputItem -> outputItem.isMessage())
                .flatMap(outputItem -> outputItem.asMessage().content().stream())
                .filter(content -> content.isOutputText())
                .map(content -> content.asOutputText())
                .findFirst()
                .orElse(null);
    }

    private String extractOutputText(Response response) {
        return response.output().stream()
                .filter(outputItem -> outputItem.isMessage())
                .flatMap(outputItem -> outputItem.asMessage().content().stream())
                .filter(content -> content.isOutputText())
                .map(content -> content.asOutputText().text())
                .reduce("", String::concat);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String getEnvironmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
