package org.supertribe.websocket;

import org.apache.tomee.embedded.Configuration;
import org.apache.tomee.embedded.junit.TomEEEmbeddedRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import static java.util.Arrays.asList;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TribeWebSocketTest {
    @Rule
    public TomEEEmbeddedRule server =
            new TomEEEmbeddedRule(new Configuration().randomHttpPort().user("Tomitribe", "tomee"), "");

    @Test
    public void sayHi() throws Exception {
        final AtomicReference<String> message = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Endpoint endpoint = new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String content) {
                        message.set(content);
                        latch.countDown();
                    }
                });
            }
        };

        ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put("Authorization", asList("Basic " + printBase64Binary("Tomitribe:tomee".getBytes())));
            }
        };
        ClientEndpointConfig authorizationConfiguration = ClientEndpointConfig.Builder.create()
                .configurator(configurator)
                .build();

        Session session = ContainerProvider.getWebSocketContainer()
                .connectToServer(
                        endpoint, authorizationConfiguration,
                        new URI("ws://localhost:" + server.getPort() + "/socket"));

        latch.await(1, TimeUnit.MINUTES);
        session.close();

        assertEquals("Hello Tomitribe", message.get());
    }

    @Test(expected = DeploymentException.class)
    public void attack() throws Exception {
        ContainerProvider.getWebSocketContainer()
                .connectToServer(
                        new Endpoint() {
                            @Override
                            public void onOpen(Session session, EndpointConfig config) {
                                fail();
                            }
                        },
                        new URI("ws://localhost:" + server.getPort() + "/socket"));
    }
}
