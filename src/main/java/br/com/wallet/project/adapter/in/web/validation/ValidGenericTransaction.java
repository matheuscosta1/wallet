package br.com.wallet.project.adapter.in.web.validation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GenericTransactionValidator.class)
public @interface ValidGenericTransaction {
    String message() default "Invalid data for the given transaction type.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
