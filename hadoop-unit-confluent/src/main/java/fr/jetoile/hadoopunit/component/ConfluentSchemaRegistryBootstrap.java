/*
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

package fr.jetoile.hadoopunit.component;

import fr.jetoile.hadoopunit.Component;
import fr.jetoile.hadoopunit.HadoopUnitConfig;
import fr.jetoile.hadoopunit.HadoopUtils;
import fr.jetoile.hadoopunit.exception.BootstrapException;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import io.confluent.kafkarest.KafkaRestApplication;
import io.confluent.kafkarest.KafkaRestConfig;
import io.confluent.rest.RestConfigException;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.utils.Time;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static fr.jetoile.hadoopunit.HadoopUnitConfig.*;

public class ConfluentSchemaRegistryBootstrap implements Bootstrap {
    final public static String NAME = Component.CONFLUENT_SCHEMAREGISTRY.name();

    static final private Logger LOGGER = LoggerFactory.getLogger(ConfluentSchemaRegistryBootstrap.class);

    private State state = State.STOPPED;

    private Configuration configuration;

    private Map<String, String> schemaRegistryConfig = new HashMap<>();

    private Server schemaregistryServer;


    public ConfluentSchemaRegistryBootstrap() {
        try {
            configuration = HadoopUtils.INSTANCE.loadConfigFile(null);
            loadConfig();
        } catch (BootstrapException e) {
            LOGGER.error("unable to load configuration", e);
        }
    }

    public ConfluentSchemaRegistryBootstrap(URL url) {
        try {
            configuration = HadoopUtils.INSTANCE.loadConfigFile(url);
            loadConfig();
        } catch (BootstrapException e) {
            LOGGER.error("unable to load configuration", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProperties() {
        return "\n \t\t\t schemaregistry listener:" + configuration.getString(CONFLUENT_KAFKA_HOST_KEY);
    }

    public void loadConfig() {
        schemaRegistryConfig.put("debug", configuration.getString(CONFLUENT_SCHEMAREGISTRY_DEBUG_KEY));
        schemaRegistryConfig.put("listeners", "http://" + configuration.getString(CONFLUENT_SCHEMAREGISTRY_HOST_KEY) + ":" + configuration.getString(CONFLUENT_SCHEMAREGISTRY_PORT_KEY));
        schemaRegistryConfig.put("kafkastore.topic", configuration.getString(CONFLUENT_SCHEMAREGISTRY_TOPIC_KEY));
        schemaRegistryConfig.put("kafkastore.connection.url", configuration.getString(ZOOKEEPER_HOST_KEY) + ":" + configuration.getString(ZOOKEEPER_PORT_KEY));
    }

    @Override
    public void loadConfig(Map<String, String> configs) {

    }

    private void build() throws IOException, InterruptedException {
        try {
            SchemaRegistryConfig config = new SchemaRegistryConfig(schemaRegistryConfig);
            SchemaRegistryRestApplication app = new SchemaRegistryRestApplication(config);
            schemaregistryServer = app.createServer();
        } catch (RestConfigException e) {
            LOGGER.error("Server configuration failed: ", e);
        } catch (Exception e) {
            LOGGER.error("Server died unexpectedly: ", e);
        }
    }

    @Override
    public Bootstrap start() {
        if (state == State.STOPPED) {
            state = State.STARTING;
            LOGGER.info("{} is starting", this.getClass().getName());
            try {
                build();
                schemaregistryServer.start();
            } catch (Exception e) {
                LOGGER.error("unable to add confluent schema registry", e);
            }
            state = State.STARTED;
            LOGGER.info("{} is started", this.getClass().getName());
        }

        return this;
    }

    @Override
    public Bootstrap stop() {
        if (state == State.STARTED) {
            state = State.STOPPING;
            LOGGER.info("{} is stopping", this.getClass().getName());
            try {
                schemaregistryServer.stop();
            } catch (Exception e) {
                LOGGER.error("unable to stop confluent schema registry", e);
            }
            state = State.STOPPED;
            LOGGER.info("{} is stopped", this.getClass().getName());
        }
        return this;
    }

}
