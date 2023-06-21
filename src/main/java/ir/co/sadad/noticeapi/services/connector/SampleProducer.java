package ir.co.sadad.noticeapi.services.connector;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

/**
 * Sample producer application using Reactive API for Kafka.
 * To run sample producer
 * <ol>
 *   <li> Start Zookeeper and Kafka server
 *   <li> Update {@link #BOOTSTRAP_SERVERS} and {@link #TOPIC} if required
 *   <li> Create Kafka topic {@link #TOPIC}
 *   <li> Run {@link SampleProducer} as Java application with all dependent jars in the CLASSPATH (eg. from IDE).
 *   <li> Shutdown Kafka server and Zookeeper when no longer required
 * </ol>
 */

@Slf4j
public class SampleProducer {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "demo-topic";

    /*
    The generic types of SenderOptions<K, V> and KafkaSender<K, V> are the key and value types of producer records
    published using the KafkaSender and corresponding serializers must be set on the SenderOptions instance before the KafkaSender is created.
     */
    private final KafkaSender<Integer, String> sender;
    private final DateTimeFormatter dateFormat;

    public SampleProducer(String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);//.maxInFlight(1024);

        /*
        Other configuration options for the reactive KafkaSender like the maximum number of in-flight messages can also be configured
         before the KafkaSender instance is created.

         The KafkaSender is now ready to send messages to Kafka.
         but no connections to Kafka have been made yet.
         */
        sender = KafkaSender.create(senderOptions);
        dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss:SSS z dd MMM yyyy");
    }

    /*
    Each outbound message to be sent to Kafka is represented as a SenderRecord.

     A SenderRecord is a Kafka ProducerRecord with additional correlation metadata for matching send results to records.
     ProducerRecord consists of a key/value pair to send to Kafka and the name of the Kafka topic to send the message to
     */
    public void sendMessages(String topic, int count, CountDownLatch latch) throws InterruptedException {
        sender.<Integer>send(Flux.range(1, count)
                .map(i -> SenderRecord.create(new ProducerRecord<>(topic, i, "Message_" + i), i))) //Reactive send operation for the outbound Flux,creates a sequence of messages to send to Kafka
                .doOnError(e -> log.error("Send failed", e))
//              .doOnNext(r -> System.out.printf("Message #%d send response: %s\n", r.correlationMetadata(), r.recordMetadata()))
                .subscribe(r -> {       //	Subscribe to trigger the actual flow of records from outboundFlux to Kafka.
                    RecordMetadata metadata = r.recordMetadata();
                    Instant timestamp = Instant.ofEpochMilli(metadata.timestamp());
                    System.out.printf("Message %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s\n",
                            r.correlationMetadata(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            dateFormat.format(timestamp));
                    latch.countDown();
                });
    }

    public void close() {
        sender.close();
    }

//    public static void main(String[] args) throws Exception {
//        int count = 20;
//        CountDownLatch latch = new CountDownLatch(count);
//        SampleProducer producer = new SampleProducer(BOOTSTRAP_SERVERS);
//        producer.sendMessages(TOPIC, count, latch);
//        latch.await(10, TimeUnit.SECONDS);
//        producer.close();
//    }
}
