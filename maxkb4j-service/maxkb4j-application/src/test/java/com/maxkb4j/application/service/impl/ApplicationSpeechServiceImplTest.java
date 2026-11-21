package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.service.IApplicationInternalService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.model.base.STTModel;
import com.maxkb4j.model.service.IModelProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationSpeechServiceImplTest {

    private static final String APP_ID = "app-1";
    private static final String STT_MODEL_ID = "stt-1";

    @Mock
    private IModelProviderService modelFactory;
    @Mock
    private IApplicationInternalService applicationService;
    @Mock
    private STTModel sttModel;

    private ApplicationSpeechServiceImpl speechService;

    @BeforeEach
    void setUp() {
        speechService = new ApplicationSpeechServiceImpl(modelFactory, applicationService);
        ApplicationVO app = new ApplicationVO();
        app.setSttModelId(STT_MODEL_ID);
        when(applicationService.getAppDetail(APP_ID, false)).thenReturn(app);
        when(modelFactory.buildSTTModel(org.mockito.ArgumentMatchers.eq(STT_MODEL_ID), any(JSONObject.class)))
                .thenReturn(sttModel);
        when(sttModel.speechToText(any(), nullable(String.class))).thenReturn("recognized");
    }

    @ParameterizedTest
    @MethodSource("audioMetadata")
    void speechToTextResolvesAudioSuffixWithoutThrowing(String filename, String contentType,
                                                         String expectedSuffix) throws Exception {
        byte[] audio = {1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, audio);

        String result = speechService.speechToText(APP_ID, file, false);

        assertEquals("recognized", result);
        ArgumentCaptor<String> suffixCaptor = ArgumentCaptor.forClass(String.class);
        verify(sttModel).speechToText(org.mockito.ArgumentMatchers.eq(audio), suffixCaptor.capture());
        assertEquals(expectedSuffix, suffixCaptor.getValue());
    }

    private static Stream<Arguments> audioMetadata() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(null, "invalid", null),
                Arguments.of(null, "audio/", null),
                Arguments.of(null, "audio/mpeg", "mpeg"),
                Arguments.of(null, "application/octet-stream", "octet-stream"),
                Arguments.of(null, "audio/webm;codecs=opus", "webm"),
                Arguments.of("voice.MP3", "application/octet-stream", "mp3"),
                Arguments.of("voice", "audio/wav", "wav")
        );
    }
}
