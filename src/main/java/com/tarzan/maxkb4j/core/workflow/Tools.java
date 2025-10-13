package com.tarzan.maxkb4j.core.workflow;

public class Tools {

    private static final String tool_message_template = """
            <details>
                <summary>
                    <strong>Called Tool: <em>%s</em></strong>
                </summary>
            
            %s
            
            </details>
            
            """;

    private static final String tool_message_json_template = """
            ```json
            %s
            ```
            """;

    public static String getToolMessage(String toolName, String toolMessage) {
        String toolMessageJson= String.format(tool_message_json_template,  toolMessage);
        return String.format(tool_message_template, toolName, toolMessageJson);
    }


}
