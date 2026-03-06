package br.com.wallet.project.adapter.in.web.validation;
import br.com.wallet.project.adapter.in.web.request.GenericTransactionRequest;
import br.com.wallet.project.adapter.in.web.validation.group.ValidationGroupResolver;
import jakarta.validation.*;
import org.hibernate.validator.internal.engine.messageinterpolation.DefaultLocaleResolver;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import java.util.*;
public class GenericTransactionValidator implements ConstraintValidator<ValidGenericTransaction, GenericTransactionRequest> {
    private Validator validator;
    @Override
    public void initialize(ValidGenericTransaction annotation) {
        Set<Locale> locales = new HashSet<>();
        locales.add(Locale.ENGLISH);
        ValidatorFactory factory = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ResourceBundleMessageInterpolator(locales, Locale.ENGLISH, new DefaultLocaleResolver(), true))
            .buildValidatorFactory();
        this.validator = factory.getValidator();
    }
    @Override
    public boolean isValid(GenericTransactionRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;
        Class<?> group = ValidationGroupResolver.resolve(request.getTransactionType());
        Set<ConstraintViolation<GenericTransactionRequest>> violations = validator.validate(request, group);
        if (!violations.isEmpty()) {
            context.disableDefaultConstraintViolation();
            violations.forEach(v -> context.buildConstraintViolationWithTemplate(v.getMessage())
                .addPropertyNode(v.getPropertyPath().toString()).addConstraintViolation());
            return false;
        }
        return true;
    }
}
