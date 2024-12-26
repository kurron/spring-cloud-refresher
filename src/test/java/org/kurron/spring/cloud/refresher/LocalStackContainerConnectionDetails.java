package org.kurron.spring.cloud.refresher;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.net.URI;

@SuppressWarnings("unused")
class LocalStackContainerConnectionDetails extends ContainerConnectionDetailsFactory<LocalStackContainer, LocalStackConnectionDetails> {
    // Spring needs a default constructor
    public LocalStackContainerConnectionDetails() {}

    @Override
    protected LocalStackConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LocalStackContainer> source) {
        return new CustomLocalStackConnectionDetails(source);
    }

    private static final class CustomLocalStackConnectionDetails extends ContainerConnectionDetailsFactory.ContainerConnectionDetails<LocalStackContainer> implements LocalStackConnectionDetails {
        private CustomLocalStackConnectionDetails(ContainerConnectionSource<LocalStackContainer> source) {
            super(source);
        }

        @Override
        public URI endpoint() {
            return getContainer().getEndpoint();
        }

        @Override
        public String region() {
            return getContainer().getRegion();
        }

        @Override
        public String accessKey() {
            return getContainer().getAccessKey();
        }

        @Override
        public String secretKey() {
            return getContainer().getSecretKey();
        }
    }
}