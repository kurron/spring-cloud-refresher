package org.kurron.spring.cloud.refresher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(path="/echo")
public class EchoResource {

    private static final Logger logger = LoggerFactory.getLogger(EchoResource.class);

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String currentTime() {
        var now = Instant.now().toString();
        logger.info("currentTime returning {}", now);
        return now;
    }
}
