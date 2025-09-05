package com.tarzan.maxkb4j.core.langchain4j;

import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;

public class MyAiServices<T> extends AiServices<T>  {
    protected MyAiServices(AiServiceContext context) {
        super(context);
    }

    @Override
    public T build() {
        return null;
    }
/*
    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();
    private final Collection<TokenStreamAdapter> tokenStreamAdapters = loadFactories(TokenStreamAdapter.class);

    MyAiServices(AiServiceContext context) {
        super(context);
    }

    static void validateParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters == null || parameters.length < 2) {
            return;
        }

        for (Parameter parameter : parameters) {
            V v = parameter.getAnnotation(V.class);
            dev.langchain4j.service.UserMessage userMessage =
                    parameter.getAnnotation(dev.langchain4j.service.UserMessage.class);
            MemoryId memoryId = parameter.getAnnotation(MemoryId.class);
            UserName userName = parameter.getAnnotation(UserName.class);
            if (v == null && userMessage == null && memoryId == null && userName == null) {
                throw illegalConfiguration(
                        "Parameter '%s' of method '%s' should be annotated with @V or @UserMessage "
                                + "or @UserName or @MemoryId",
                        parameter.getName(), method.getName());
            }
        }
    }

    public T build() {

        performBasicValidation();

        if (!context.hasChatMemory() && ChatMemoryAccess.class.isAssignableFrom(context.aiServiceClass)) {
            throw illegalConfiguration(
                    "In order to have a service implementing ChatMemoryAccess, please configure the ChatMemoryProvider on the '%s'.",
                    context.aiServiceClass.getName());
        }

        for (Method method : context.aiServiceClass.getMethods()) {
            if (method.isAnnotationPresent(Moderate.class) && context.moderationModel == null) {
                throw illegalConfiguration(
                        "The @Moderate annotation is present, but the moderationModel is not set up. "
                                + "Please ensure a valid moderationModel is configured before using the @Moderate annotation.");
            }

            Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                throw illegalConfiguration("'%s' is not a supported return type of an AI Service method", returnType.getName());
            }
            if (returnType == Result.class || returnType == List.class || returnType == Set.class) {
                TypeUtils.validateReturnTypesAreProperlyParametrized(method.getName(), method.getGenericReturnType());
            }

            if (!context.hasChatMemory()) {
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(MemoryId.class)) {
                        throw illegalConfiguration(
                                "In order to use @MemoryId, please configure the ChatMemoryProvider on the '%s'.",
                                context.aiServiceClass.getName());
                    }
                }
            }
        }

        Object proxyInstance = Proxy.newProxyInstance(
                context.aiServiceClass.getClassLoader(),
                new Class<?>[]{context.aiServiceClass},
                new InvocationHandler() {

                    private final ExecutorService executor = Executors.newCachedThreadPool();

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {

                        if (method.getDeclaringClass() == Object.class) {
                            // methods like equals(), hashCode() and toString() should not be handled by this proxy
                            return method.invoke(this, args);
                        }

                        if (method.getDeclaringClass() == ChatMemoryAccess.class) {
                            return switch (method.getName()) {
                                case "getChatMemory" -> context.chatMemoryService.getChatMemory(args[0]);
                                case "evictChatMemory" -> context.chatMemoryService.evictChatMemory(args[0]) != null;
                                default -> throw new UnsupportedOperationException(
                                        "Unknown method on ChatMemoryAccess class : " + method.getName());
                            };
                        }

                        validateParameters(method);

                        final Object memoryId = findMemoryId(method, args).orElse(ChatMemoryService.DEFAULT);
                        final ChatMemory chatMemory = context.hasChatMemory()
                                ? context.chatMemoryService.getOrCreateChatMemory(memoryId)
                                : null;

                        Optional<dev.langchain4j.data.message.SystemMessage> systemMessage = prepareSystemMessage(memoryId, method, args);
                        dev.langchain4j.data.message.UserMessage userMessage = prepareUserMessage(method, args);
                        AugmentationResult augmentationResult = null;
                        if (context.retrievalAugmentor != null) {
                            List<ChatMessage> chatMemoryMessages = chatMemory != null ? chatMemory.messages() : null;
                            Metadata metadata = Metadata.from(userMessage, memoryId, chatMemoryMessages);
                            AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, metadata);
                            augmentationResult = context.retrievalAugmentor.augment(augmentationRequest);
                         //   userMessage = (dev.langchain4j.data.message.UserMessage) augmentationResult.chatMessage();
                        }

                        Type returnType = method.getGenericReturnType();
                        boolean streaming = returnType == TokenStream.class || canAdaptTokenStreamTo(returnType);
                        boolean supportsJsonSchema = supportsJsonSchema();
                        Optional<JsonSchema> jsonSchema = Optional.empty();
                        if (supportsJsonSchema && !streaming) {
                            jsonSchema = serviceOutputParser.jsonSchema(returnType);
                        }
                        if ((!supportsJsonSchema || jsonSchema.isEmpty()) && !streaming) {
                            userMessage = appendOutputFormatInstructions(returnType, userMessage);
                        }

                        List<ChatMessage> messages;
                        if (chatMemory != null) {
                            systemMessage.ifPresent(chatMemory::add);
                            chatMemory.add(userMessage);
                            messages = chatMemory.messages();
                        } else {
                            messages = new ArrayList<>();
                            systemMessage.ifPresent(messages::add);
                            messages.add(userMessage);
                        }

                        Future<Moderation> moderationFuture = triggerModerationIfNeeded(method, messages);

                        ToolServiceContext toolServiceContext =
                                context.toolService.createContext(memoryId, userMessage);

                        if (streaming) {
                            TokenStream tokenStream = new AiServiceTokenStream(AiServiceTokenStreamParameters.builder()
                                    .messages(messages)
                                    .toolSpecifications(toolServiceContext.toolSpecifications())
                                    .toolExecutors(toolServiceContext.toolExecutors())
                                    .retrievedContents(
                                            augmentationResult != null ? augmentationResult.contents() : null)
                                    .context(context)
                                    .memoryId(memoryId)
                                    .build());
                            // TODO moderation
                            if (returnType == TokenStream.class) {
                                return tokenStream;
                            } else {
                                return adapt(tokenStream, returnType);
                            }
                        }

                        ResponseFormat responseFormat = null;
                        if (supportsJsonSchema && jsonSchema.isPresent()) {
                            responseFormat = ResponseFormat.builder()
                                    .type(JSON)
                                    .jsonSchema(jsonSchema.get())
                                    .build();
                        }

                        ChatRequestParameters parameters = ChatRequestParameters.builder()
                                .toolSpecifications(toolServiceContext.toolSpecifications())
                                .responseFormat(responseFormat)
                                .build();

                        ChatRequest chatRequest = ChatRequest.builder()
                                .messages(messages)
                                .parameters(parameters)
                                .build();

                        ChatResponse chatResponse = context.chatModel.chat(chatRequest);

                        verifyModerationIfNeeded(moderationFuture);

                        ToolServiceResult toolServiceResult = context.toolService.executeInferenceAndToolsLoop(
                                chatResponse,
                                parameters,
                                messages,
                                context.chatModel,
                                chatMemory,
                                memoryId,
                                toolServiceContext.toolExecutors());

                        chatResponse = toolServiceResult.chatResponse();

                        Object parsedResponse = serviceOutputParser.parse(chatResponse, returnType);
                        if (typeHasRawClass(returnType, Result.class)) {
                            return Result.builder()
                                    .content(parsedResponse)
                                    .tokenUsage(chatResponse.tokenUsage())
                                    .sources(augmentationResult == null ? null : augmentationResult.contents())
                                    .finishReason(chatResponse.finishReason())
                                    .toolExecutions(toolServiceResult.toolExecutions())
                                    .build();
                        } else {
                            return parsedResponse;
                        }
                    }

                    private boolean canAdaptTokenStreamTo(Type returnType) {
                        for (TokenStreamAdapter tokenStreamAdapter : tokenStreamAdapters) {
                            if (tokenStreamAdapter.canAdaptTokenStreamTo(returnType)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    private Object adapt(TokenStream tokenStream, Type returnType) {
                        for (TokenStreamAdapter tokenStreamAdapter : tokenStreamAdapters) {
                            if (tokenStreamAdapter.canAdaptTokenStreamTo(returnType)) {
                                return tokenStreamAdapter.adapt(tokenStream);
                            }
                        }
                        throw new IllegalStateException("Can't find suitable TokenStreamAdapter");
                    }

                    private boolean supportsJsonSchema() {
                        return context.chatModel != null
                                && context.chatModel.supportedCapabilities().contains(RESPONSE_FORMAT_JSON_SCHEMA);
                    }

                    private dev.langchain4j.data.message.UserMessage appendOutputFormatInstructions(Type returnType, dev.langchain4j.data.message.UserMessage userMessage) {
                        String outputFormatInstructions = serviceOutputParser.outputFormatInstructions(returnType);
                        String text = userMessage.singleText() + outputFormatInstructions;
                        if (isNotNullOrBlank(userMessage.name())) {
                            userMessage = dev.langchain4j.data.message.UserMessage.from(userMessage.name(), text);
                        } else {
                            userMessage = dev.langchain4j.data.message.UserMessage.from(text);
                        }
                        return userMessage;
                    }

                    private Future<Moderation> triggerModerationIfNeeded(Method method, List<ChatMessage> messages) {
                        if (method.isAnnotationPresent(Moderate.class)) {
                            return executor.submit(() -> {
                                List<ChatMessage> messagesToModerate = removeToolMessages(messages);
                                return context.moderationModel
                                        .moderate(messagesToModerate)
                                        .content();
                            });
                        }
                        return null;
                    }
                });

        return (T) proxyInstance;
    }

    private Optional<dev.langchain4j.data.message.SystemMessage> prepareSystemMessage(Object memoryId, Method method, Object[] args) {
        return findSystemMessageTemplate(memoryId, method)
                .map(systemMessageTemplate -> PromptTemplate.from(systemMessageTemplate)
                        .apply(findTemplateVariables(systemMessageTemplate, method, args))
                        .toSystemMessage());
    }

    private Optional<String> findSystemMessageTemplate(Object memoryId, Method method) {
        dev.langchain4j.service.SystemMessage annotation =
                method.getAnnotation(dev.langchain4j.service.SystemMessage.class);
        if (annotation != null) {
            return Optional.of(getTemplate(
                    method, "System", annotation.fromResource(), annotation.value(), annotation.delimiter()));
        }

        return context.systemMessageProvider.apply(memoryId);
    }

    private static Map<String, Object> findTemplateVariables(String template, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();

        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            String variableName = getVariableName(parameters[i]);
            Object variableValue = args[i];
            variables.put(variableName, variableValue);
        }

        if (template.contains("{{it}}") && !variables.containsKey("it")) {
            String itValue = getValueOfVariableIt(parameters, args);
            variables.put("it", itValue);
        }

        return variables;
    }

    private static String getVariableName(Parameter parameter) {
        V annotation = parameter.getAnnotation(V.class);
        if (annotation != null) {
            return annotation.value();
        } else {
            return parameter.getName();
        }
    }

    private static String getValueOfVariableIt(Parameter[] parameters, Object[] args) {
        if (parameters.length == 1) {
            Parameter parameter = parameters[0];
            if (!parameter.isAnnotationPresent(MemoryId.class)
                    && !parameter.isAnnotationPresent(dev.langchain4j.service.UserMessage.class)
                    && !parameter.isAnnotationPresent(UserName.class)
                    && (!parameter.isAnnotationPresent(V.class) || isAnnotatedWithIt(parameter))) {
                return toString(args[0]);
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            if (isAnnotatedWithIt(parameters[i])) {
                return toString(args[i]);
            }
        }

        throw illegalConfiguration("Error: cannot find the value of the prompt template variable \"{{it}}\".");
    }

    private static boolean isAnnotatedWithIt(Parameter parameter) {
        V annotation = parameter.getAnnotation(V.class);
        return annotation != null && "it".equals(annotation.value());
    }

    private static dev.langchain4j.data.message.UserMessage prepareUserMessage(Method method, Object[] args) {

        String template = getUserMessageTemplate(method, args);
        Map<String, Object> variables = findTemplateVariables(template, method, args);

        Prompt prompt = PromptTemplate.from(template).apply(variables);

        Optional<String> maybeUserName = findUserName(method.getParameters(), args);
        return maybeUserName
                .map(userName -> dev.langchain4j.data.message.UserMessage.from(userName, prompt.text()))
                .orElseGet(prompt::toUserMessage);
    }

    private static String getUserMessageTemplate(Method method, Object[] args) {

        Optional<String> templateFromMethodAnnotation = findUserMessageTemplateFromMethodAnnotation(method);
        Optional<String> templateFromParameterAnnotation =
                findUserMessageTemplateFromAnnotatedParameter(method.getParameters(), args);

        if (templateFromMethodAnnotation.isPresent() && templateFromParameterAnnotation.isPresent()) {
            throw illegalConfiguration(
                    "Error: The method '%s' has multiple @UserMessage annotations. Please use only one.",
                    method.getName());
        }

        if (templateFromMethodAnnotation.isPresent()) {
            return templateFromMethodAnnotation.get();
        }
        if (templateFromParameterAnnotation.isPresent()) {
            return templateFromParameterAnnotation.get();
        }

        Optional<String> templateFromTheOnlyArgument =
                findUserMessageTemplateFromTheOnlyArgument(method.getParameters(), args);
        if (templateFromTheOnlyArgument.isPresent()) {
            return templateFromTheOnlyArgument.get();
        }

        throw illegalConfiguration("Error: The method '%s' does not have a user message defined.", method.getName());
    }

    private static Optional<String> findUserMessageTemplateFromMethodAnnotation(Method method) {
        return Optional.ofNullable(method.getAnnotation(dev.langchain4j.service.UserMessage.class))
                .map(a -> getTemplate(method, "User", a.fromResource(), a.value(), a.delimiter()));
    }

    private static Optional<String> findUserMessageTemplateFromAnnotatedParameter(
            Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(dev.langchain4j.service.UserMessage.class)) {
                return Optional.of(toString(args[i]));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findUserMessageTemplateFromTheOnlyArgument(Parameter[] parameters, Object[] args) {
        if (parameters != null && parameters.length == 1 && parameters[0].getAnnotations().length == 0) {
            return Optional.of(toString(args[0]));
        }
        return Optional.empty();
    }

    private static Optional<String> findUserName(Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(UserName.class)) {
                return Optional.of(args[i].toString());
            }
        }
        return Optional.empty();
    }

    private static String getTemplate(Method method, String type, String resource, String[] value, String delimiter) {
        String messageTemplate;
        if (!resource.trim().isEmpty()) {
            messageTemplate = getResourceText(method.getDeclaringClass(), resource);
            if (messageTemplate == null) {
                throw illegalConfiguration("@%sMessage's resource '%s' not found", type, resource);
            }
        } else {
            messageTemplate = String.join(delimiter, value);
        }
        if (messageTemplate.trim().isEmpty()) {
            throw illegalConfiguration("@%sMessage's template cannot be empty", type);
        }
        return messageTemplate;
    }

    private static String getResourceText(Class<?> clazz, String resource) {
        InputStream inputStream = clazz.getResourceAsStream(resource);
        if (inputStream == null) {
            inputStream = clazz.getResourceAsStream("/" + resource);
        }
        return getText(inputStream);
    }

    private static String getText(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try (Scanner scanner = new Scanner(inputStream);
             Scanner s = scanner.useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private static Optional<Object> findMemoryId(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(MemoryId.class)) {
                Object memoryId = args[i];
                if (memoryId == null) {
                    throw illegalArgument(
                            "The value of parameter '%s' annotated with @MemoryId in method '%s' must not be null",
                            parameters[i].getName(), method.getName());
                }
                return Optional.of(memoryId);
            }
        }
        return Optional.empty();
    }

    private static String toString(Object arg) {
        if (arg.getClass().isArray()) {
            return arrayToString(arg);
        } else if (arg.getClass().isAnnotationPresent(StructuredPrompt.class)) {
            return StructuredPromptProcessor.toPrompt(arg).text();
        } else {
            return arg.toString();
        }
    }

    private static String arrayToString(Object arg) {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(arg);
        for (int i = 0; i < length; i++) {
            sb.append(toString(Array.get(arg, i)));
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static <T> AiServices<T> builder(Class<T> aiService) {
        AiServiceContext context = new AiServiceContext(aiService);
        return new MyAiServices<>(context);
    }*/
}
