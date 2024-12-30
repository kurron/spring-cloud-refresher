package org.kurron.spring.cloud.refresher;

import io.awspring.cloud.dynamodb.DynamoDbOperations;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringCloudRefresherApplicationTests {

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    S3Operations s3;

    @Autowired
    DynamoDbOperations dynamoDb;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    LocalStackConnectionDetails connectionDetails;

    @Test
    @DisplayName("Make sure the application stands up")
    void contextLoads() {
    }

    @Test
    @DisplayName("Exercise DynamoDB calls")
    void testDynamoDB() {
        assertNotNull(connectionDetails);
        assertNotNull(dynamoDb);
        var i = 0;
    }

    @Test
    @DisplayName("Exercise S3 calls")
    void testS3() throws IOException {
        assertNotNull(connectionDetails);
        assertNotNull(s3);
        var bucket = Long.toHexString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
        assertFalse( s3.bucketExists(bucket), "Should never exist!");
        assertNotNull(s3.createBucket(bucket), "Bucket was not created!");
        byte[] buffer = new byte[256];
        ThreadLocalRandom.current().nextBytes(buffer);
        var key = Long.toHexString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
        var resource = s3.upload(bucket, key, new ByteArrayInputStream(buffer), ObjectMetadata.builder().contentType("application/octet-stream").build());
        assertTrue(resource.exists(), "Resource should exist!");
        assertEquals("application/octet-stream", resource.contentType());
        assertEquals(bucket, resource.getLocation().getBucket());
        try( var in = resource.getInputStream() ) {
            try ( var out = new ByteArrayOutputStream() ) {
                out.write(in.readAllBytes());
                byte[] read = out.toByteArray();
                assertArrayEquals(read, buffer, "Bytes read from the bucket do NOT match bytes written to the bucket!");
            }
        }
        s3.deleteObject(bucket, key);
        s3.deleteBucket(bucket);
        assertFalse(s3.bucketExists(bucket), "Bucket should have been deleted!");
    }

    @Test
    @DisplayName("Exercise Repositories")
    void loadAuditRecords() {
        assertNotNull(auditRepository);
        var saved = auditRepository.save(new AuditEntity(null, "192.168.1.1", OffsetDateTime.now(UTC)));
        assertEquals(1, saved.id());
        var found = auditRepository.findById(saved.id());
        //nanoseconds are slightly different so ignore them for the comparison
        assertEquals(saved.when().toLocalDateTime().toEpochSecond(ZoneOffset.MIN), found.orElseThrow().when().toLocalDateTime().toEpochSecond(ZoneOffset.MIN));
    }
}
