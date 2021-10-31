package bbejeck.clients;

import bbejeck.chapter_6.proto.RetailPurchaseProto;
import bbejeck.chapter_6.proto.SensorProto;
import bbejeck.data.DataGenerator;
import bbejeck.serializers.ProtoSerializer;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class produces records for the various Kafka Streams applications.
 * In each case the producer will run indefinitely until you call {MockDataProducer#close}
 */
public class MockDataProducer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MockDataProducer.class);

    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    private static final String TRANSACTIONS_TOPIC = "transactions";
    private static final String YELLING_APP_TOPIC = "src-topic";
    private static final String NULL_KEY = null;
    private volatile boolean keepRunning = true;

    public MockDataProducer() {
    }

    public void producePurchasedItemsData() {
        producePurchasedItemsData(false);
    }

    public void producePurchasedItemsDataSchemaRegistry() {
        producePurchasedItemsData(true);
    }

    private void producePurchasedItemsData(boolean produceSchemaRegistry) {
        Runnable generateTask = () -> {
                LOG.info("Creating task for generating mock purchase transactions");
                final Map<String, Object> configs = producerConfigs();
                final Callback callback = callback();
                if (produceSchemaRegistry) {
                    configs.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
                    configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
                } else {
                    configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtoSerializer.class);
                }
                try (Producer<String, RetailPurchaseProto.RetailPurchase> producer = new KafkaProducer<>(configs)) {
                    LOG.info("Producer created now getting ready to send records");
                    while (keepRunning) {
                        Collection<RetailPurchaseProto.RetailPurchase> purchases = DataGenerator.generatePurchasedItems(100);
                        LOG.info("Generated {} records to send", purchases.size());
                        purchases.stream()
                                .map(purchase -> {
                                    ProducerRecord<String, RetailPurchaseProto.RetailPurchase> producerRecord =
                                            new ProducerRecord<>(TRANSACTIONS_TOPIC, NULL_KEY, purchase);
                                    addHeader(purchase, producerRecord);
                                    return producerRecord;
                                })
                                .forEach(pr -> producer.send(pr, callback));
                        LOG.info("Record batch sent");
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                LOG.info("Done generating purchase data");
        };
        executorService.submit(generateTask);
    }

    private void addHeader(RetailPurchaseProto.RetailPurchase purchase,
                           ProducerRecord<String, RetailPurchaseProto.RetailPurchase> producerRecord) {
        String department = purchase.getDepartment();
        String headerValue;
        if (department.equals("coffee")
                || department.equals("electronics")) {
            headerValue = department;
        } else {
            headerValue = "purchases";
        }
        producerRecord.headers().add("routing", headerValue.getBytes(StandardCharsets.UTF_8));
    }

    public void produceRandomTextData() {
        Runnable generateTask = () -> {
            final Map<String, Object> configs = producerConfigs();
            final Callback callback = callback();
            configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            try (Producer<String, String> producer = new KafkaProducer<>(configs)) {
                while (keepRunning) {
                    Collection<String> textValues = DataGenerator.generateRandomText();
                    textValues.stream()
                            .map(text -> new ProducerRecord<>(YELLING_APP_TOPIC, NULL_KEY, text))
                            .forEach(pr -> producer.send(pr, callback));

                    LOG.info("Text batch sent");
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        LOG.info("Done generating text data");
        executorService.submit(generateTask);
    }

    public void produceIotData() {
        Runnable generateTask = () -> {
            final Map<String, Object> configs = producerConfigs();
            final Callback callback = callback();
            configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtoSerializer.class);
            try (Producer<String, SensorProto.Sensor> producer = new KafkaProducer<>(configs)) {
                while (keepRunning) {
                    Map<String, List<SensorProto.Sensor>> sensorValuesMap = DataGenerator.generateSensorReadings(120);
                    sensorValuesMap.forEach((topic, value) -> {
                        value.stream().map(sensor -> new ProducerRecord<>(topic, NULL_KEY, sensor))
                                .forEach(pr -> producer.send(pr, callback));
                        LOG.info("Sensor batch sent");
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
        };
        LOG.info("Done generating text data");
        executorService.submit(generateTask);
    }




    public  void close() {
        LOG.info("Shutting down data generation");
        keepRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

    }

    private static Map<String, Object> producerConfigs() {
        Map<String, Object> producerConfigs = new HashMap<>();
        producerConfigs.put("bootstrap.servers", "localhost:9092");
        producerConfigs.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfigs.put("acks", "all");
        return producerConfigs;
    }

    private static Callback callback() {
        return ((metadata, exception) -> {
            if (exception != null) {
                LOG.error("Problem producing record", exception);
            }
        });
    }


}
