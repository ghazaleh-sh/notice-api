package ir.co.sadad.noticeapi.configs;

import ir.co.sadad.noticeapi.exceptions.handlers.ReactiveExceptionHandler;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class AppConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.addBasenames("classpath:message_exception");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(getValidator());
    }

    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

//
//    @Bean
//    public CacheManager cacheManager() {
//        return new ConcurrentMapCacheManager("configs");
//    }
//

    @Bean
    @Order(-2)
    public ReactiveExceptionHandler reactiveExceptionHandler(WebProperties webProperties,
                                                             ApplicationContext applicationContext,
                                                             ServerCodecConfigurer configurer) {

        ReactiveExceptionHandler exceptionHandler = new ReactiveExceptionHandler(
                new DefaultErrorAttributes(), webProperties.getResources(), applicationContext, messageSource());
        exceptionHandler.setMessageWriters(configurer.getWriters());
        exceptionHandler.setMessageReaders(configurer.getReaders());
        return exceptionHandler;

    }
}
