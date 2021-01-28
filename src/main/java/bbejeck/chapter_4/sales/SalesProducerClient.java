package bbejeck.chapter_4.sales;

import bbejeck.chapter_4.avro.ProductTransaction;
import bbejeck.common.DataSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;

/**
 * User: Bill Bejeck
 * Date: 1/18/21
 * Time: 7:44 PM
 */
public class SalesProducerClient {
    private static final Logger LOG = LogManager.getLogger(SalesProducerClient.class);
    private final Map<String,Object> producerConfigs;
    private final DataSource<ProductTransaction> salesDataSource;
    private volatile boolean keepProducing = true;



    public SalesProducerClient(final Map<String, Object> producerConfigs,
                               final DataSource<ProductTransaction> salesDataSource) {
        this.producerConfigs = producerConfigs;
        this.salesDataSource = salesDataSource;
    }

    public void runProducer() {
        try (Producer<String, ProductTransaction> producer = new KafkaProducer<>(producerConfigs)) {
            final String topicName = (String)producerConfigs.get("topic.name");
            LOG.info("Created producer instance with {}", producerConfigs);
            while(keepProducing) {
                Collection<ProductTransaction> purchases = salesDataSource.fetch();
                LOG.info("Received sales data");
                purchases.forEach(purchase -> {
                    ProducerRecord<String, ProductTransaction> producerRecord = new ProducerRecord<>(topicName, purchase.getCustomerName(), purchase);
                    producer.send(producerRecord, (metadata, exception) -> {
                        if (exception != null) {
                            LOG.error("Error producing records ", exception);
                        } else {
                            LOG.info("Produced record at offset {} with timestamp {}", metadata.offset(), metadata.timestamp());
                        }
                    });
                });
                try {
                    // Pause so results don't just blast across the screen
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            LOG.info("Producer loop exiting now");
        }
    }

    public void close() {
        LOG.info("Received signal to close");
        keepProducing = false;
    }

}
