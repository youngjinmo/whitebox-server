package io.andy.shorten_url;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @GetMapping("/ping")
    public String pingTest() {
        log.debug("request ping..");
        return "pong!";
    }
}
