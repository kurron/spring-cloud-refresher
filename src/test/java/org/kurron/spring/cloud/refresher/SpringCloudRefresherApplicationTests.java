package org.kurron.spring.cloud.refresher;

import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.s3.S3Operations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringCloudRefresherApplicationTests {

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    S3Operations s3;

    @Autowired
    LocalStackConnectionDetails connectionDetails;

    @Autowired
    S3Properties s3Properties;

    @Test
    void contextLoads() {
    }

    @BeforeEach
    void beforeEachTest() {
        s3Properties.setEndpoint(connectionDetails.endpoint());
    }

    @Test
    void testS3() {
        assertNotNull(s3Properties);
        assertNotNull(connectionDetails);
        assertNotNull(s3);
        var exists = s3.bucketExists(Long.toHexString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)));
        assertFalse(exists, "Should never exist!");
    }

    @Test
    void loadAuditRecords() {
        assertNotNull(auditRepository);
        var saved = auditRepository.save(new AuditEntity(null, "192.168.1.1", OffsetDateTime.now(UTC)));
        assertEquals(1, saved.id());
        var found = auditRepository.findById(saved.id());
        //nano seconds are slightly different so ignore them for the comparison
        assertEquals(saved.when().toLocalDateTime().toEpochSecond(ZoneOffset.MIN), found.orElseThrow().when().toLocalDateTime().toEpochSecond(ZoneOffset.MIN));
    }
}
