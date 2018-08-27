package org.supertribe.websocket;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunAsClient
@RunWith(Arquillian.class)
public class TribeWebSocketTest {

    @ArquillianResource()
    private URL url;

    @Deployment()
    public static final WebArchive app() {
        return ShrinkWrap.create(WebArchive.class, "app.war")
                .addClasses(TribeWebSocket.class)
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"), "web.xml");
    }

    /**
     * sending authentication
     *
     * @throws Exception
     */
    @Test
    public void sayHi() throws Exception {
        final AtomicReference<String> message = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        Endpoint endpoint = new Endpoint() {
            @Override
            public void onOpen(Session session,
                               EndpointConfig config) {
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
                        new URI("ws://localhost:" + url.getPort() + "/app/socket"));

        latch.await(1, TimeUnit.MINUTES);
        session.close();

        assertEquals("Hello Tomitribe", message.get());
    }

    /**
     * Not sending authentication
     *
     * @throws Exception
     */
    @Test(expected = DeploymentException.class)
    public void attack() throws Exception {
        ContainerProvider.getWebSocketContainer()
                .connectToServer(
                        new Endpoint() {
                            @Override
                            public void onOpen(Session session,
                                               EndpointConfig config) {
                                fail();
                            }
                        },
                        new URI("ws://localhost:" + url.getPort() + "/app/socket"));
    }


}
