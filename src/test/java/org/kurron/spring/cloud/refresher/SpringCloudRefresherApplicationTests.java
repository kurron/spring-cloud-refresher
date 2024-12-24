package org.kurron.spring.cloud.refresher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringCloudRefresherApplicationTests {

    @Autowired
    AuditRepository auditRepository;

    @Test
    void contextLoads() {
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
