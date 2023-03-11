package ir.co.sadad.noticeapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class NoticeServiceImplTest {

    @Autowired
    private NoticeServiceImpl noticeService;

    @Test
    void updateRemainNotices() {
        String preId = "640c205f0cbbcc575b3497ec";
        String lastSeen = "640c20e20cbbcc575b3497ef";
        String ssn = "007";

//        assertEquals(5, noticeService.updateRemainNotices(, preId, lastSeen));

    }
}