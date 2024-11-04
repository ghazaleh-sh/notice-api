package ir.co.sadad.noticeapi.services.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UtilitiesTest {

    @Test
    public void testCurrentUTC(){
        assertNotEquals("", Utilities.getCurrentUTC());
        }

    @Test
    public void testCurrentUTCDate(){
        assertEquals("2024-06-16", Utilities.getCurrentUTCDate());
    }


}