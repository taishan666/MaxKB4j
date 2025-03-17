package com.tarzan.maxkb4j.module.application.workflow.dto;

import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FlowParams extends BaseParams {

    @NotEmpty(message = "历史对答不能为空")
    private List<ApplicationChatRecordEntity> historyChatRecord=new ArrayList<>();

    @NotBlank(message = "用户问题不能为空")
    private String question;

    @NotBlank(message = "对话id不能为空")
    private String chatId;

    @NotBlank(message = "对话记录id不能为空")
    private String chatRecordId;

    @NotNull(message = "流式输出不能为空")
    private Boolean stream;

    private String clientId;

    private String clientType;

    @NotBlank(message = "用户id不能为空")
    private String userId;

    @NotNull(message = "换个答案不能为空")
    private Boolean reChat;

    // Example of how to validate the serializer
/*    public boolean isValid() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<FlowParams>> violations = validator.validate(this);

        if (!violations.isEmpty()) {
            for (ConstraintViolation<FlowParams> violation : violations) {
                System.out.println(violation.getMessage());
            }
            return false;
        }
        return true;
    }*/

/*    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInput = "{...}"; // Your JSON input here

        FlowParams flowParams = objectMapper.readValue(jsonInput, FlowParams.class);
        if (flowParams.isValid()) {
            System.out.println("Validation successful");
        } else {
            System.out.println("Validation failed");
        }
    }*/
}