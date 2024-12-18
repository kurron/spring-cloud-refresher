package org.kurron.spring.cloud.refresher;

import jakarta.servlet.http.HttpServletRequest;
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
    public String currentTime(HttpServletRequest request) {
        var where = determineCallerAddress(request);
        var when = Instant.now().toString();
        logger.info("currentTime called from {} at {}", where, when);
        return when;
    }

    private String determineCallerAddress(HttpServletRequest request) {
        var ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
