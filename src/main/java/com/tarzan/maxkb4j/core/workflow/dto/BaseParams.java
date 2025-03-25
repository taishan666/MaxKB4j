package com.tarzan.maxkb4j.core.workflow.dto;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public abstract class BaseParams {

    public boolean isValid() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<BaseParams>> violations = validator.validate(this);
        if (!violations.isEmpty()) {
            for (ConstraintViolation<BaseParams> violation : violations) {
                System.out.println(violation.getMessage());
            }
            return false;
        }
        return true;
    }
}
