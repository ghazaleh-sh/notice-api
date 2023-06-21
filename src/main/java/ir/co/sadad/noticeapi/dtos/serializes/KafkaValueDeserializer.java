package ir.co.sadad.noticeapi.dtos.serializes;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.co.sadad.noticeapi.dtos.TransactionNoticeReqDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author g.shahrokhabadi
 */

@Slf4j
public class KafkaValueDeserializer implements Deserializer<TransactionNoticeReqDto> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public TransactionNoticeReqDto deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(new String(data, StandardCharsets.UTF_8), TransactionNoticeReqDto.class);
        } catch (Exception e) {
            log.error("Unable to deserialize message {}", data, e);
            return null;
        }
    }

    @Override
    public void close() {
    }
}
