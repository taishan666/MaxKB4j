package com.tarzan.maxkb4j.common.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

public class TokenUtil {


    public static int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Encoding encoding = Encodings.newDefaultEncodingRegistry()
                .getEncoding(EncodingType.CL100K_BASE);
        return encoding.countTokens(text);
    }

}
