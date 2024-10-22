package io.andy.shorten_url.util.ip;

import io.andy.shorten_url.exception.server.InternalServerException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class IpLocationUtils {
    private final WebClient webClient;

    public IpLocationUtils() {
        webClient = WebClient.create(ExternalApiHostUrl.IP_API);
    }

    public IpApiResponse getLocationByIp(String ip) {
        try {
            String response = webClient.get()
                    .uri("/"+ip)
                    .retrieve()
                    .bodyToMono(String.class).block();
            ObjectMapper objectMapper = new ObjectMapper();
            IpApiResponse result = objectMapper.readValue(response, IpApiResponse.class);
            if (result.getStatus().equals("fail")) {
                throw new IllegalStateException("FAILED TO GET LOCATION BY IP");
            }
            return result;
        } catch (Exception e) {
            log.error("failed to get location by ip, message={}", e.getMessage());
        }
        throw new InternalServerException("SERVER ERROR");
    }
}
