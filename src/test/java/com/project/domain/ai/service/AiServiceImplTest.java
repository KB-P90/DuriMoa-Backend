package com.project.domain.ai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.services.blocking.ResponseService;
import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AiServiceImplTest {

    @Test
    void testConnectionReturnsOpenAIResponseText() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response response = Mockito.mock(Response.class);
        ResponseOutputItem outputItem = Mockito.mock(ResponseOutputItem.class);
        ResponseOutputMessage outputMessage = Mockito.mock(ResponseOutputMessage.class);
        ResponseOutputMessage.Content content = Mockito.mock(ResponseOutputMessage.Content.class);
        ResponseOutputText outputText = Mockito.mock(ResponseOutputText.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any())).thenReturn(response);
        Mockito.when(response.output()).thenReturn(Collections.singletonList(outputItem));
        Mockito.when(outputItem.isMessage()).thenReturn(true);
        Mockito.when(outputItem.asMessage()).thenReturn(outputMessage);
        Mockito.when(outputMessage.content()).thenReturn(Collections.singletonList(content));
        Mockito.when(content.isOutputText()).thenReturn(true);
        Mockito.when(content.asOutputText()).thenReturn(outputText);
        Mockito.when(outputText.text()).thenReturn("OpenAI 연결 성공");

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiConnectionTestResponseDTO result = service.testConnection();

        Assertions.assertEquals("OpenAI 연결 성공", result.getResponse());
        Assertions.assertNotNull(result.getModel());
        Mockito.verify(responseService).create(Mockito.<ResponseCreateParams>any());
    }

    @Test
    void testConnectionRejectsEmptyOpenAIResponse() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response response = Mockito.mock(Response.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any())).thenReturn(response);
        Mockito.when(response.output()).thenReturn(Collections.emptyList());

        AiServiceImpl service = new AiServiceImpl(clientProvider);

        Assertions.assertThrows(IllegalStateException.class, service::testConnection);
    }

    @Test
    void chatReturnsOpenAIResponseText() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response response = Mockito.mock(Response.class);
        ResponseOutputItem outputItem = Mockito.mock(ResponseOutputItem.class);
        ResponseOutputMessage outputMessage = Mockito.mock(ResponseOutputMessage.class);
        ResponseOutputMessage.Content content = Mockito.mock(ResponseOutputMessage.Content.class);
        ResponseOutputText outputText = Mockito.mock(ResponseOutputText.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any())).thenReturn(response);
        Mockito.when(response.output()).thenReturn(Collections.singletonList(outputItem));
        Mockito.when(outputItem.isMessage()).thenReturn(true);
        Mockito.when(outputItem.asMessage()).thenReturn(outputMessage);
        Mockito.when(outputMessage.content()).thenReturn(Collections.singletonList(content));
        Mockito.when(content.isOutputText()).thenReturn(true);
        Mockito.when(content.asOutputText()).thenReturn(outputText);
        Mockito.when(outputText.text()).thenReturn("안녕하세요! 무엇을 도와드릴까요?");

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("안녕");

        Assertions.assertEquals("안녕하세요! 무엇을 도와드릴까요?", result.getResponse());
        Assertions.assertNotNull(result.getModel());
        ArgumentCaptor<ResponseCreateParams> paramsCaptor =
                ArgumentCaptor.forClass(ResponseCreateParams.class);
        Mockito.verify(responseService).create(paramsCaptor.capture());
        Assertions.assertEquals(1024L, paramsCaptor.getValue().maxOutputTokens().orElseThrow());
        Assertions.assertEquals(
                ReasoningEffort.MINIMAL,
                paramsCaptor.getValue().reasoning().orElseThrow().effort().orElseThrow());
    }

    @Test
    void chatRetriesWhenFirstResponseHasNoOutputText() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response firstResponse = Mockito.mock(Response.class);
        Response secondResponse = Mockito.mock(Response.class);
        ResponseOutputItem reasoningItem = Mockito.mock(ResponseOutputItem.class);
        ResponseOutputItem messageItem = Mockito.mock(ResponseOutputItem.class);
        ResponseOutputMessage outputMessage = Mockito.mock(ResponseOutputMessage.class);
        ResponseOutputMessage.Content content = Mockito.mock(ResponseOutputMessage.Content.class);
        ResponseOutputText outputText = Mockito.mock(ResponseOutputText.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any()))
                .thenReturn(firstResponse, secondResponse);
        Mockito.when(firstResponse.output()).thenReturn(Collections.singletonList(reasoningItem));
        Mockito.when(reasoningItem.isMessage()).thenReturn(false);
        Mockito.when(secondResponse.output()).thenReturn(Collections.singletonList(messageItem));
        Mockito.when(messageItem.isMessage()).thenReturn(true);
        Mockito.when(messageItem.asMessage()).thenReturn(outputMessage);
        Mockito.when(outputMessage.content()).thenReturn(Collections.singletonList(content));
        Mockito.when(content.isOutputText()).thenReturn(true);
        Mockito.when(content.asOutputText()).thenReturn(outputText);
        Mockito.when(outputText.text()).thenReturn("안녕하세요!");

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("ㅎㅇ");

        Assertions.assertEquals("안녕하세요!", result.getResponse());
        Mockito.verify(responseService, Mockito.times(2))
                .create(Mockito.<ResponseCreateParams>any());
    }

    @Test
    void chatReturnsFallbackWhenBothResponsesHaveNoOutputText() {
        ObjectProvider<OpenAIClient> clientProvider = Mockito.mock(ObjectProvider.class);
        OpenAIClient client = Mockito.mock(OpenAIClient.class);
        ResponseService responseService = Mockito.mock(ResponseService.class);
        Response response = Mockito.mock(Response.class);
        ResponseOutputItem reasoningItem = Mockito.mock(ResponseOutputItem.class);

        Mockito.when(clientProvider.getObject()).thenReturn(client);
        Mockito.when(client.responses()).thenReturn(responseService);
        Mockito.when(responseService.create(Mockito.<ResponseCreateParams>any())).thenReturn(response);
        Mockito.when(response.output()).thenReturn(Collections.singletonList(reasoningItem));
        Mockito.when(reasoningItem.isMessage()).thenReturn(false);

        AiServiceImpl service = new AiServiceImpl(clientProvider);
        AiChatResponseDTO result = service.chat("ㅎㅇ");

        Assertions.assertEquals(
                "답변을 생성하지 못했어요. 잠시 후 다시 질문해 주세요.",
                result.getResponse());
        Mockito.verify(responseService, Mockito.times(2))
                .create(Mockito.<ResponseCreateParams>any());
    }
}
