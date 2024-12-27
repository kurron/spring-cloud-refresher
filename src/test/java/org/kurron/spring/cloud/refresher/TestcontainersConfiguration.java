package org.kurron.spring.cloud.refresher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    static final Logger log = LoggerFactory.getLogger(TestcontainersConfiguration.class);

/*
    @Bean
    @ServiceConnection
*/
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
    }

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));
    }

    @SuppressWarnings("resource")
    @Bean
    @ServiceConnection // we're using a custom code to support this -- see spring.factories for a pointer
    static LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest")).withServices(LocalStackContainer.Service.S3);
    }

    // this is an interesting way to set the property before all the beans get wired up. Never used this technique before.
    @Bean
    DynamicPropertyRegistrar localStackPropertiesRegistrar(LocalStackContainer container) {
        return registry -> {
            //registry.add("spring.cloud.aws.endpoint", container::getEndpoint);
            registry.add("spring.cloud.aws.s3.endpoint", container::getEndpoint);
            registry.add("spring.cloud.aws.s3.region", container::getRegion);
        };
    }

    @Bean
    public AwsCredentialsProvider customAwsCredentialsProvider(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") LocalStackConnectionDetails connectionDetails) {
        return () -> new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return connectionDetails.accessKey();
            }

            @Override
            public String secretAccessKey() {
                return connectionDetails.secretKey();
            }
        };
    }

    @Bean
    public AwsRegionProvider customAwsRegionProvider(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") LocalStackConnectionDetails connectionDetails) {
        return () -> Region.of(connectionDetails.region());
    }

}
