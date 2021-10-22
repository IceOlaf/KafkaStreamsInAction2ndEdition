/*
 * Copyright 2016 Bill Bejeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bbejeck.chapter_6;

import bbejeck.BaseStreamsApplication;
import bbejeck.clients.MockDataProducer;
import bbejeck.utils.Topics;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaStreamsYellingAppWithPeek extends BaseStreamsApplication {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamsYellingAppWithPeek.class);

    @Override
    public Topology topology(Properties streamProperties) {
        ForeachAction<String, String> sysout = (key, value) ->
                System.out.println("key " + key
                        + " value " + value);

        Serde<String> stringSerde = Serdes.String();
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> simpleFirstStream = builder.stream("src-topic",
                        Consumed.with(stringSerde, stringSerde))
                .peek(sysout);
        KStream<String, String> upperCasedStream = simpleFirstStream
                .mapValues((key, value) -> value.toUpperCase())
                .peek(sysout);
        
        upperCasedStream.to("out-topic", Produced.with(stringSerde, stringSerde));

        return builder.build(streamProperties);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Creating topics");
        Topics.create("src-topic", "out-topic");
        MockDataProducer.produceRandomTextData();
        Properties streamProperties = new Properties();
        streamProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "yelling_app_id");
        streamProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        KafkaStreamsYellingAppWithPeek yellingApp = new KafkaStreamsYellingAppWithPeek();
        Topology topology = yellingApp.topology(streamProperties);
        try (KafkaStreams kafkaStreams = new KafkaStreams(topology, streamProperties)) {
            LOG.info("Hello World Yelling App Started");
            kafkaStreams.start();
            Thread.sleep(35000);
            LOG.info("Shutting down the Yelling APP now");
            MockDataProducer.shutdown();
        }

    }
}