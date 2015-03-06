package org.supertribe.websocket;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/socket")
public class TribeWebSocket {
    @OnOpen
    public void onOpen(final Session session) throws Exception {
        session.getBasicRemote().sendText("Hello " + session.getUserPrincipal().getName());
    }
}
