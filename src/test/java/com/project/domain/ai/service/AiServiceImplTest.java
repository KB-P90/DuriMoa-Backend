package com.project.domain.ai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseOutputItem;
import com.openai.models.responses.StructuredResponseOutputMessage;
import com.openai.services.blocking.ResponseService;
import com.project.domain.ai.domain.AiAction;
import com.project.domain.ai.domain.AiBudgetType;
import com.project.domain.ai.domain.AiIntent;
import com.project.domain.ai.dto.AiBudgetTypeModelResponseDTO;
import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiIntentModelResponseDTO;
import com.project.domain.ai.model.AiWeddingBudgetSelectionContext;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class AiServiceImplTest {

    @Test
    void testConnectionReturnsOpenAIResponseText() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response response = textResponse("OpenAI 연결 성공");

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any()))
                .thenReturn(response);

        AiServiceImpl service = new AiServiceImpl(clientProvider);

        Assertions.assertEquals("OpenAI 연결 성공", service.testConnection().getResponse());
    }

    @Test
    void testConnectionRejectsEmptyOpenAIResponse() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response response = Mockito.mock(Response.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any()))
                .thenReturn(response);
        Mockito.when(response.output()).thenReturn(Collections.emptyList());

        AiServiceImpl service = new AiServiceImpl(clientProvider);

        Assertions.assertThrows(IllegalStateException.class, service::testConnection);
    }

    @Test
    void chatMapsWeddingIntentToServerAction() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        StructuredResponse<AiIntentModelResponseDTO> response = structuredResponse(
                new AiIntentModelResponseDTO(
                        AiIntent.WEDDING_BUDGET_RECOMMENDATION,
                        "결혼 예산을 추천해드릴게요."
                )
        );

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.doReturn(response)
                .when(responseService)
                .create(Mockito.any(StructuredResponseCreateParams.class));

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("우리 둘 결혼 예산 짜줘");

        Assertions.assertEquals(AiIntent.WEDDING_BUDGET_RECOMMENDATION, result.getIntent());
        Assertions.assertEquals(AiAction.START_WEDDING_BUDGET_FLOW, result.getAction());
        Assertions.assertEquals("결혼 예산을 추천해드릴게요.", result.getResponse());
    }

    @Test
    void chatReturnsSupportedFeatureGuideForUnknownIntent() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        StructuredResponse<AiIntentModelResponseDTO> response = structuredResponse(
                new AiIntentModelResponseDTO(
                        AiIntent.UNKNOWN,
                        "모델이 생성한 임의의 안내"
                )
        );

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.doReturn(response)
                .when(responseService)
                .create(Mockito.any(StructuredResponseCreateParams.class));

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("내일 날씨 알려줘");

        Assertions.assertEquals(AiIntent.UNKNOWN, result.getIntent());
        Assertions.assertNull(result.getAction());
        Assertions.assertTrue(result.getResponse().contains("맞춤 결혼 예산 추천"));
        Assertions.assertTrue(result.getResponse().contains("결혼 준비 소비 리포트"));
        Assertions.assertTrue(result.getResponse().contains("커플 저축 플랜"));
    }

    @Test
    void chatMapsSpendingReportIntentToGenerateAction() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        StructuredResponse<AiIntentModelResponseDTO> response = structuredResponse(
                new AiIntentModelResponseDTO(
                        AiIntent.SPENDING_REPORT,
                        "가입 이후 소비를 분석해드릴게요."
                )
        );

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.doReturn(response)
                .when(responseService)
                .create(Mockito.any(StructuredResponseCreateParams.class));

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("가입한 뒤 소비 리포트 보여줘");

        Assertions.assertEquals(AiIntent.SPENDING_REPORT, result.getIntent());
        Assertions.assertEquals(AiAction.GENERATE_SPENDING_REPORT, result.getAction());
    }

    @Test
    void chatReturnsUnknownFallbackWhenOpenAIFails() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.doThrow(new IllegalStateException("OpenAI unavailable"))
                .when(responseService)
                .create(Mockito.any(StructuredResponseCreateParams.class));

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("날씨 알려줘");

        Assertions.assertEquals(AiIntent.UNKNOWN, result.getIntent());
        Assertions.assertNull(result.getAction());
    }

    @Test
    void budgetTypeSelectionSkipsOpenAIWhenOnlySavingIsEligible() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        AiServiceImpl service = new AiServiceImpl(clientProvider);

        AiBudgetTypeModelResponseDTO result = service.selectWeddingBudgetType(
                AiWeddingBudgetSelectionContext.builder()
                        .eligibleBudgetTypes(List.of(AiBudgetType.SAVING))
                        .build()
        );

        Assertions.assertEquals(AiBudgetType.SAVING, result.getBudgetType());
        Mockito.verifyNoInteractions(clientProvider);
    }

    private Response textResponse(String text) {
        Response response = Mockito.mock(Response.class);
        ResponseOutputItem outputItem = Mockito.mock(ResponseOutputItem.class);
        ResponseOutputMessage outputMessage = Mockito.mock(ResponseOutputMessage.class);
        ResponseOutputMessage.Content content = Mockito.mock(ResponseOutputMessage.Content.class);
        ResponseOutputText outputText = Mockito.mock(ResponseOutputText.class);
        Mockito.when(response.output()).thenReturn(Collections.singletonList(outputItem));
        Mockito.when(outputItem.isMessage()).thenReturn(true);
        Mockito.when(outputItem.asMessage()).thenReturn(outputMessage);
        Mockito.when(outputMessage.content()).thenReturn(Collections.singletonList(content));
        Mockito.when(content.isOutputText()).thenReturn(true);
        Mockito.when(content.asOutputText()).thenReturn(outputText);
        Mockito.when(outputText.text()).thenReturn(text);
        return response;
    }

    private <T> StructuredResponse<T> structuredResponse(T value) {
        StructuredResponse<T> response = Mockito.mock(StructuredResponse.class);
        StructuredResponseOutputItem<T> outputItem = Mockito.mock(
                StructuredResponseOutputItem.class
        );
        StructuredResponseOutputMessage<T> outputMessage = Mockito.mock(
                StructuredResponseOutputMessage.class
        );
        StructuredResponseOutputMessage.Content<T> content = Mockito.mock(
                StructuredResponseOutputMessage.Content.class
        );
        Mockito.when(response.output()).thenReturn(Collections.singletonList(outputItem));
        Mockito.when(outputItem.isMessage()).thenReturn(true);
        Mockito.when(outputItem.asMessage()).thenReturn(outputMessage);
        Mockito.when(outputMessage.content()).thenReturn(Collections.singletonList(content));
        Mockito.when(content.isOutputText()).thenReturn(true);
        Mockito.when(content.asOutputText()).thenReturn(value);
        return response;
    }
}
