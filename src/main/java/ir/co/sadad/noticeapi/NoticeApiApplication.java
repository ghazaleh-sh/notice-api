package ir.co.sadad.noticeapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "notice api", description = "این مایکروسرویس شامل اعلانات کاربران از قبیل پیام های کمپین و تراکنش های زیر 30 هزار تومان میباشد"))
public class NoticeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoticeApiApplication.class, args);
    }

}
