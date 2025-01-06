package org.kurron.spring.cloud.refresher;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.awspring.cloud.dynamodb.DynamoDbOperations;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Autowired
    DynamoDbClient dynamoDbClient;

    @Autowired
    SqsOperations sqs;

    @Autowired
    private SecretsManagerClient secretsManagerClient;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    LocalStackConnectionDetails connectionDetails;

    @Test
    @DisplayName("Make sure the application stands up")
    void contextLoads() {
    }

    // luckily Jackson can handle records, making life much simpler
    record PointToPointMessage(@JsonProperty("message") String message, @JsonProperty("when") Instant when) {}

    // Records are not supported, so we have to go old school and use a class with mutators
    @SuppressWarnings("unused")
    @DynamoDbBean
    public static class Person {
        private String id; // partition key
        private String firstName; // not part of the ky
        private String lastName; // sort key

        public Person() {
            // required for Spring
        }

        public Person(String id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbAttribute("firstName")
        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        @DynamoDbAttribute("lastName")
        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return Objects.equals(id, person.id) && Objects.equals(firstName, person.firstName) && Objects.equals(lastName, person.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, firstName, lastName);
        }
    }

    @Test
    @DisplayName("Exercise DynamoDB calls")
    void testDynamoDB() {
        assertNotNull(connectionDetails);
        assertNotNull(dynamoDb);
        assertNotNull(dynamoDbClient);
        // a "row" is an Item, each Item contains Attributes (bits of data), Hash/Partition key index to an Item, Range/Sort sorts/limits Partition keys
        // scalar type stores 1 value (string, number, binary, boolean)
        // set type stores multiple scalars (string set, number set, binary set) in unordered fashion
        // document type (list is ordered array, map is JSON)
        var table = "person"; // default behavior is snake case of the entity
        var createResponse = dynamoDbClient.createTable(CreateTableRequest.builder().tableName(table)
                                                                                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                                                                                    .attributeDefinitions(AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build())
                                                                                    .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
                                                                                    .build());
        assertEquals(table, createResponse.tableDescription().tableName(), "Table names don't match!");
        var toSave = new Person(UUID.randomUUID().toString(), "Ron", "Kurr");
        var saved = dynamoDb.save(toSave);
        var allItems = dynamoDb.scanAll(Person.class);
        assertEquals(saved, allItems.stream().findFirst().orElseThrow().items().stream().findFirst().orElseThrow(), "Item was not found!");
        var loaded = dynamoDb.load(Key.builder().partitionValue(saved.id).build(), Person.class);
        assertEquals(saved, loaded, "Items don't match!");
        var newFirstName = randomHexString();
        loaded.setFirstName(newFirstName);
        var mutated = dynamoDb.update(loaded);
        assertEquals(newFirstName, mutated.firstName);
        var deleted = dynamoDb.delete(mutated);
        var shouldNotExist = dynamoDb.load(Key.builder().partitionValue(deleted.id).build(), Person.class);
        assertNull(shouldNotExist, "Item should not be in the table!");
        var deleteResponse = dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(table).build());
        assertEquals(table, deleteResponse.tableDescription().tableName(), "Table names don't match!");
    }

    @Test
    @DisplayName("Exercise S3 calls")
    void testS3() throws IOException {
        assertNotNull(connectionDetails);
        assertNotNull(s3);
        var bucket = randomHexString();
        assertFalse( s3.bucketExists(bucket), "Should never exist!");
        assertNotNull(s3.createBucket(bucket), "Bucket was not created!");
        byte[] buffer = new byte[256];
        ThreadLocalRandom.current().nextBytes(buffer);
        var key = randomHexString();
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
    @DisplayName("Exercise SQS calls")
    void testSQS() {
        assertNotNull(sqs);
        var payload = new PointToPointMessage(randomHexString(), Instant.now());
        var queue = randomHexString();
        var header = randomHexString();
        var value = randomHexString();
        SendResult<PointToPointMessage> sent = sqs.send(to -> to.queue(queue).payload(payload).header(header, value));
        assertTrue(sent.message().getHeaders().containsKey(header), "Custom header not found!");
        assertTrue(sent.message().getHeaders().containsValue(value), "Custom header value not found!");
        assertEquals(payload.message, sent.message().getPayload().message, "Message values don't match!");
        Optional<Message<PointToPointMessage>> received = sqs.receive(from -> from.queue(queue).visibilityTimeout(Duration.ofSeconds(10)).pollTimeout(Duration.ofSeconds(5)), PointToPointMessage.class);
        assertTrue(received.orElseThrow().getHeaders().containsKey(header), "Custom header not found!");
        assertTrue(received.orElseThrow().getHeaders().containsValue(value), "Custom header value not found!");
        assertEquals(payload.message, received.orElseThrow().getPayload().message, "Message values don't match!");
    }

    @Test
    @DisplayName("Exercise Secrets Manager calls")
    void testSecretsManager() {
        assertNotNull(secretsManagerClient);
        var name = randomHexString();
        var value = randomHexString();
        var secret = CreateSecretRequest.builder().name(name).secretString(value).build();
        var response = secretsManagerClient.createSecret(secret);
        var i = 0;
    }

    private String randomHexString() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
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
