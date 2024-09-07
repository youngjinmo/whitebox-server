package io.andy.shorten_url.util.ip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import io.andy.shorten_url.exception.server.InternalServerException;

public final class IpLocationUtils {
    private final WebClient webClient;

    public IpLocationUtils(String hostUrl) {
        webClient = WebClient.create(hostUrl);
    }

    public IpApiResponse getLocationByIp(String ip) {
        try {
            String response = webClient.get()
                .uri("/"+ip)
                .retrieve()
                .bodyToMono(String.class).block();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response, IpApiResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error: "+e.getMessage());
        }
        // logback
        throw new InternalServerException("외부 api 호출 에러");
    }
}
