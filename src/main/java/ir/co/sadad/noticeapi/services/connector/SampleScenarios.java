package ir.co.sadad.noticeapi.services.connector;

import ir.co.sadad.noticeapi.dtos.TransactionNoticeReqDto;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.*;

/**
 * Sample flows using Reactive API for Kafka.
 * To run a sample scenario:
 * <ol>
 *   <li> Start Zookeeper and Kafka server
 *   <li> Create Kafka topics {@link #TOPICS}
 *   <li> Update {@link #BOOTSTRAP_SERVERS} and {@link #TOPICS} if required
 *   <li> Run {@link SampleScenarios} as Java application (eg. {@link SampleScenarios} KAFKA_SINK)
 *        with all dependent jars in the CLASSPATH (eg. from IDE).
 *   <li> Shutdown Kafka server and Zookeeper when no longer required
 * </ol>
 */
@Slf4j
public class SampleScenarios {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPICS = "noticeTopic";
    private static NotificationRepository notificationRepository;

    public SampleScenarios(NotificationRepository notificationRepository) {
        SampleScenarios.notificationRepository = notificationRepository;
    }

    /**
     *  * This sample demonstrates the use of Kafka as a source when messages are transferred from
     *  * a Kafka topic to an external sink. Kafka offsets are committed when records are successfully
     *  * transferred. Unlimited retries on the source Kafka Flux ensure that the Kafka consumer is
     *  * restarted if there are any exceptions while processing records.
     *  */
    public static class KafkaSource extends AbstractScenario {
        private final String topic;
        private final Scheduler scheduler;

        private final NotificationRepository notificationRepository;

        public KafkaSource(String bootstrapServers, String topic, NotificationRepository notificationRepository) {
            super(bootstrapServers);
            this.topic = topic;
            this.notificationRepository = notificationRepository;
            this.scheduler = Schedulers.newSingle("sample", true);
        }
        public Flux<?> flux() {
            return KafkaReceiver.create(receiverOptions(Collections.singletonList(topic)).commitInterval(Duration.ZERO))
                    .receive()
                    .publishOn(scheduler)
//                    .concatMap(m -> storeInDB(m.value())
//                            .thenEmpty(m.receiverOffset().commit()))
                    .retry()
                    .doOnCancel(() -> close());
        }
//        public Mono<String> storeInDB(SendSingleNoticeReqDto singleNoticeReqDto) {
//            log.info("Successfully processed singleNoticeReqDto with title {} from Kafka", singleNoticeReqDto.getTitle());
//            return notificationRepository.insert(Notification
//                    .builder()
//                    .title(singleNoticeReqDto.getTitle())
//                    .description(singleNoticeReqDto.getDescription())
//                    .date(singleNoticeReqDto.getDate())
//                    .type("1")
//                    .build()
//            ).map(Notification::getId);
////            return Mono.empty();
//        }

        public void close() {
            super.close();
            scheduler.dispose();
        }

    }

//    static class CommittableSource {
//        private final List<Notification> sourceList = new ArrayList<>();
//        CommittableSource() {
//            sourceList.add(new Notification("1", "John", "Doe",null,"1"));
//            sourceList.add(new Notification("1", "Ada", "Lovelace", null,"1"));
//        }
//        CommittableSource(List<Notification> list) {
//            sourceList.addAll(list);
//        }
//        Flux<Notification> flux() {
//            return Flux.fromIterable(sourceList);
//        }
//
//        void commit(int id) {
//            log.trace("Committing {}", id);
//        }
//    }


    static abstract class AbstractScenario {
        String bootstrapServers = BOOTSTRAP_SERVERS;
        String groupId = "sample-group";
//        CommittableSource source;
//        KafkaSender<Integer, Notification> sender;
        List<Disposable> disposables = new ArrayList<>();

        AbstractScenario(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }
        public abstract Flux<?> flux();

        public void runScenario() throws InterruptedException {
            flux().blockLast();
            close();
        }

        public void close() {
//            if (sender != null)
//                sender.close();
            for (Disposable disposable : disposables)
                disposable.dispose();
        }

        public ReceiverOptions<Integer, TransactionNoticeReqDto> receiverOptions() {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, "sample-consumer");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ir.co.sadad.noticeapi.dtos.serializes.KafkaValueDeserializer.class);
            return ReceiverOptions.<Integer, TransactionNoticeReqDto>create(props);
        }

        public ReceiverOptions<Integer, TransactionNoticeReqDto> receiverOptions(Collection<String> topics) {
            return receiverOptions()
                    .addAssignListener(p -> log.info("Group {} partitions assigned {}", groupId, p))
                    .addRevokeListener(p -> log.info("Group {} partitions revoked {}", groupId, p))
                    .subscription(topics);
        }


//        public SenderOptions<Integer, Notification> senderOptions() {
//            Map<String, Object> props = new HashMap<>();
//            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//            props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
//            props.put(ProducerConfig.ACKS_CONFIG, "all");
//            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
//            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//            return SenderOptions.create(props);
//        }

//        public KafkaSender<Integer, Notification> sender(SenderOptions<Integer, Notification> senderOptions) {
//            sender = KafkaSender.create(senderOptions);
//            return sender;
//        }

//        public void source(CommittableSource source) {
//            this.source = source;
//        }
//
//        public CommittableSource source() {
//            return source;
//        }
    }

    public void testAsMainMethod(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: " + SampleScenarios.class.getName() + " <scenario>");
            System.exit(1);
        }
        AbstractScenario sampleScenario;
        sampleScenario = new KafkaSource(BOOTSTRAP_SERVERS, TOPICS, notificationRepository);

        sampleScenario.runScenario();
    }

}
