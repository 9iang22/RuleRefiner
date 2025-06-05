package testcode.script;

import javax.validation.ConstraintValidatorContext;

public class ElExpressionSample {

    //ok: el-injection
    private void safeELTemplate(String message, ConstraintValidatorContext context) {
         context.disableDefaultConstraintViolation();
         context
             .someMethod()
             .buildConstraintViolationWithTemplate("somestring")
             .addConstraintViolation();
    }
}