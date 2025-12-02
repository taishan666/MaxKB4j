package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface NL2SqlAssistant {

    @SystemMessage("\"You are an expert in writing SQL queries.\\nYou have access to a {{sqlDialect}} database with the following structure:\\n{{databaseStructure}}\\nIf a user asks a question that can be answered by querying this database, generate an SQL SELECT query.\\nDo not output anything else aside from a valid SQL statement!")
    Result<String> generateSqlQuery(@V("sqlDialect") String sqlDialect, @V("databaseStructure") String databaseStructure, @UserMessage String query);
}
