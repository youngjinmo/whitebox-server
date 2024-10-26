package io.andy.shorten_url.util.ip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.andy.shorten_url.exception.server.LocationUtilException;
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
            if (result.status().equals("fail")) {
                log.error("failed to get location by ip={}", ip);
                throw new IllegalStateException("FAILED TO GET LOCATION BY IP");
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new LocationUtilException();
        } catch (Exception e) {
            log.error("failed to get location by ip, message={}", e.getMessage());
            throw e;
        }
    }
}
