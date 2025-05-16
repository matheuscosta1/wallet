package br.com.wallet.project.controller.validation;

import br.com.wallet.project.controller.request.GenericRequest;
import br.com.wallet.project.controller.validation.strategy.ValidationGroupResolver;
import br.com.wallet.project.domain.TransactionType;
import jakarta.validation.*;
import org.hibernate.validator.internal.engine.messageinterpolation.DefaultLocaleResolver;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, GenericRequest> {
    private Validator validator;
    @Override
    public void initialize(ValidTransactionRequest constraintAnnotation) {
        Set<Locale> supportedLocales = new HashSet<>();
        supportedLocales.add(Locale.ENGLISH);
        ValidatorFactory factory =
                Validation.byDefaultProvider()
                        .configure()
                        .messageInterpolator(
                                new ResourceBundleMessageInterpolator(
                                        supportedLocales,
                                        Locale.ENGLISH,
                                        new DefaultLocaleResolver(),
                                        true))
                        .buildValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Override
    public boolean isValid(GenericRequest genericRequest, ConstraintValidatorContext context) {
        if(genericRequest == null) {
            return true;
        }

        Class<?> validationGroup = determineValidationGroup(genericRequest.getTransactionType());
        Set<ConstraintViolation<GenericRequest>> violations = validator.validate(genericRequest, validationGroup);

        if(!violations.isEmpty()) {
            context.disableDefaultConstraintViolation();
            for(ConstraintViolation<GenericRequest> violation: violations) {
                context.buildConstraintViolationWithTemplate(violation.getMessage())
                        .addPropertyNode(violation.getPropertyPath().toString())
                        .addConstraintViolation();
            }
            return false;
        }
        return true;
    }

    private Class<?> determineValidationGroup(TransactionType transactionType) {
        ValidationGroupResolver validationGroupResolver = new ValidationGroupResolver();
        return validationGroupResolver.determineValidationGroup(transactionType);
    }
}
