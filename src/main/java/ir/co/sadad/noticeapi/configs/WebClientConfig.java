package ir.co.sadad.noticeapi.configs;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.net.ssl.SSLException;

/**
 * config of web client for rest services
 */
@Configuration
@Slf4j
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) throws SSLException {

        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();


        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(sslContext))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 200000)
                .wiretap(this.getClass().getCanonicalName(), LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(20000))
                                .addHandlerLast(new WriteTimeoutHandler(210000)));

        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request >>>>>>>>>>>>>: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response status >>>>>>>>>>>>>: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}