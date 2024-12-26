package org.kurron.spring.cloud.refresher;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import java.net.URI;

public interface LocalStackConnectionDetails extends ConnectionDetails {
    URI endpoint();
    String region();
    String accessKey();
    String secretKey();
}
