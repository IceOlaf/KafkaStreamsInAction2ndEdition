package bbejeck.chapter_4;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * User: Bill Bejeck
 * Date: 1/24/21
 * Time: 8:55 PM
 */

@Testcontainers
public class AdminClientTest {

    private final Properties props = new Properties();
    private final int partitions = 1;
    private final short replication = 1;

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.0.0"));

    @BeforeEach
    public void setUp() {
        props.put("bootstrap.servers", kafka.getBootstrapServers());
    }

    @Test
    @DisplayName("should create topics")
    public void testCreateTopics() {
        try (final Admin adminClient = Admin.create(props)) {

            final List<NewTopic> topics = new ArrayList<>();
            topics.add(new NewTopic("topic-one", partitions, replication));
            topics.add(new NewTopic("topic-two", partitions, replication));

            List<String> expectedTopics = Arrays.asList("topic-one", "topic-two");

            adminClient.createTopics(topics);
            List<String> actualTopicNames = new ArrayList<>(getTopicNames(adminClient));

            Collections.sort(actualTopicNames);
            assertEquals(expectedTopics, actualTopicNames);
        }
    }


    @Test
    @DisplayName("should delete topics")
    public void testDeleteTopics() {
        try (final Admin adminClient = Admin.create(props)) {

            final List<NewTopic> topics = new ArrayList<>();
            topics.add(new NewTopic("topic-one", partitions, replication));
            topics.add(new NewTopic("topic-two", partitions, replication));

            List<String> expectedTopicNames = Arrays.asList("topic-one", "topic-two");

            adminClient.createTopics(topics);
            List<String> actualTopicNames = new ArrayList<>(getTopicNames(adminClient));

            Collections.sort(actualTopicNames);
            assertEquals(expectedTopicNames, actualTopicNames);

            adminClient.deleteTopics(Collections.singletonList("topic-two"));
            expectedTopicNames = Collections.singletonList("topic-one");
            actualTopicNames = new ArrayList<>(getTopicNames(adminClient));
            assertEquals(expectedTopicNames, actualTopicNames);
        }
    }


    private Set<String> getTopicNames(Admin adminClient) {
        try {
            return adminClient.listTopics().names().get(30L, TimeUnit.SECONDS);
        } catch (TimeoutException | CancellationException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}