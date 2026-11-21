package com.maxkb4j.model.custom.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbsSTTModelTest {

    private final TestSTTModel model = new TestSTTModel();

    @ParameterizedTest
    @CsvSource({
            "mpeg, mp3",
            "mpga, mp3",
            "audio/mpeg, mp3",
            "octet-stream, mp3",
            "wav, wav",
            "x-wav, wav",
            "vnd.wave, wav",
            "webm, opus",
            "ogg, opus",
            "m4a, aac",
            "mp4, aac",
            "unknown, mp3",
            "audio/webm;codecs=opus, opus",
            "audio/mp4;codecs=mp4a.40.2, aac"
    })
    void normalizeFormatMapsMimeSubtypesToSupportedFormats(String suffix, String expected) {
        assertEquals(expected, model.normalize(suffix));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void normalizeFormatDefaultsToMp3WhenSuffixIsMissing(String suffix) {
        assertEquals("mp3", model.normalize(suffix));
    }

    @Test
    void getSampleRateFallsBackInsteadOfThrowingForOctetStream() {
        byte[] invalidAudio = {1, 2, 3};

        Integer sampleRate = assertDoesNotThrow(() -> model.sampleRate(invalidAudio, "octet-stream"));

        assertEquals(16000, sampleRate);
    }

    private static class TestSTTModel extends AbsSTTModel {
        @Override
        public String speechToText(byte[] audioBytes, String suffix) {
            return "";
        }

        private String normalize(String suffix) {
            return normalizeFormat(suffix);
        }

        private int sampleRate(byte[] audioBytes, String suffix) {
            return getSampleRate(audioBytes, suffix);
        }
    }
}
