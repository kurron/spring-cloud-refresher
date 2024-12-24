package org.kurron.spring.cloud.refresher;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;

@RestController
@RequestMapping(path="/echo")
public class EchoResource {

    private static final Logger logger = LoggerFactory.getLogger(EchoResource.class);

    private final AuditRepository repository;

    private final String instanceId;

    public EchoResource(AuditRepository repository, @Value("${spring.application.id}") String instanceId) {
        this.repository = repository;
        this.instanceId = instanceId;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String currentTime(HttpServletRequest request) {
        var where = determineCallerAddress(request);
        var when =  OffsetDateTime.now(UTC);
        logger.info("currentTime called from {} at {}", where, when);
        var saved = repository.save(new AuditEntity(null, where, when));
        logger.info("Audit record {} saved", saved.id());
        return "Instance " + instanceId + " serviced a call from " + where + " @ " + when;
    }

    private String determineCallerAddress(HttpServletRequest request) {
        var ipAddress = request.getHeader("X-Forwarded-For");
        if (null == ipAddress || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
