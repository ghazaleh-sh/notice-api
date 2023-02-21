package ir.co.sadad.noticeapi.services.connector;

import ir.co.sadad.noticeapi.configs.ReactiveKafkaConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.regex.Pattern;

public class ReactiveKafkaSource {

    @Value(value = "${spring.kafka.producer.topic}")
    private String topics;

    /**
     * The code segment below consumes records from Kafka topics,
     * transforms the record and sends the output to an external sink.
     * Kafka consumer offsets are committed after records are successfully output to sink.
     *
     *
     */
    public void consumeToMongo(){
        ReceiverOptions<Integer, String> receiverOptions = ReceiverOptions.<Integer, String>create()
                .commitInterval(Duration.ZERO)
                .commitBatchSize(0)
                .subscription(Pattern.compile(topics));

//        KafkaReceiver.create(receiverOptions)
//                .receive()
//                .publishOn(aBoundedElasticScheduler)
//                .concatMap(m -> sink.store(transform(m))
//                        .doOnSuccess(r -> m.receiverOffset().commit().block()));
    }
}
