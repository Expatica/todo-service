package com.expatica.todoservice.config;

import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Configuration for bean validation (jakarta.validation).
 * <p>
 * Provides a {@link Validator} bean that can be injected into services
 * and tests to enforce jakarta.validation.constraints at runtime.
 */
@Configuration
public class ValidationConfig {

    @Bean
    public Validator validator(ClockProvider clockProvider) {
        ValidatorFactory factory = Validation
                .byDefaultProvider()
                .configure()
                .clockProvider(clockProvider)
                .buildValidatorFactory();
        return factory.getValidator();
    }

    @Bean
    public static MethodValidationPostProcessor validationPostProcessor(Validator validator) {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.setValidator(validator);
        return postProcessor;
    }
}



