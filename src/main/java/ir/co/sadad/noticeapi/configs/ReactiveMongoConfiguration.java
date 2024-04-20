package ir.co.sadad.noticeapi.configs;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories
@EnableReactiveMongoAuditing
@AllArgsConstructor
public class ReactiveMongoConfiguration {

    private final Environment environment;

//    public ReactiveMongoConfiguration(Environment environment) {
//        this.environment = environment;
//    }

    /**
     * Config to use Audit Provider to Return bmi identity ssn for Auditor
     * @return
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl(environment);
    }
}
