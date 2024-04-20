package ir.co.sadad.noticeapi.configs;

import org.joda.time.LocalDateTime;
import org.reactivestreams.Publisher;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuditorAwareImpl implements ReactiveBeforeConvertCallback<PersistentEntity> {
    private final Environment environment;

    public AuditorAwareImpl(Environment environment) {
        this.environment = environment;
    }

    /**
     * method for getting current auditor ,
     *
     * @return ssn of bmi Identity Token From Client.
     */
    @Override
    public Publisher<PersistentEntity> onBeforeConvert(PersistentEntity entity, String collection) {
        var user = ReactiveSecurityContextHolder.getContext()
                .map(Neo4jProperties.Authentication::getPrincipal)
                .cast(UserDetails.class)
                .map(userDetails -> new Username(userDetails.getUsername()))
                .switchIfEmpty(Mono.empty());

        var currentTime = LocalDateTime.now();

        if (entity.getId() == null) {
            entity.setCreatedDate(currentTime);
        }
        entity.setLastModifiedDate(currentTime);

        return user
                .map(u -> {
                            if (entity.getId() == null) {
                                entity.setCreatedBy(u);
                            }
                            entity.setLastModifiedBy(u);

                            return entity;
                        }
                )
                .defaultIfEmpty(entity);
    }

}
