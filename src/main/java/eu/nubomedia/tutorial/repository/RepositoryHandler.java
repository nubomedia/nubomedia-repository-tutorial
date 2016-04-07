/*
 * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package eu.nubomedia.tutorial.repository;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.internal.NotEnoughResourcesException;
import org.kurento.repository.RepositoryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Recording in repository handler (application and media logic).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.4.1
 */
public class RepositoryHandler extends TextWebSocketHandler {

  @Autowired
  private RepositoryClient repositoryClient;

  private final Logger log = LoggerFactory.getLogger(RepositoryHandler.class);
  private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    try {
      JsonObject jsonMessage = new GsonBuilder().create().fromJson(message.getPayload(),
          JsonObject.class);

      log.debug("Incoming message: {}", jsonMessage);

      switch (jsonMessage.get("id").getAsString()) {
      case "start":
        start(session, jsonMessage);
        break;
      case "stop":
        stop(session);
        break;
      case "stopPlay":
        stopPlay(session);
        break;
      case "play":
        play(session, jsonMessage);
        break;
      case "onIceCandidate": {
        onIceCandidate(session, jsonMessage);
        break;
      }
      default:
        error(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
      }
    } catch (NotEnoughResourcesException e) {
      log.warn("Not enough resources", e);
      notEnoughResources(session);

    } catch (Throwable t) {
      log.error("Exception starting session", t);
      error(session, t.getClass().getSimpleName() + ": " + t.getMessage());
    }
  }

  private void start(WebSocketSession session, JsonObject jsonMessage) {
    // User session
    String sessionId = session.getId();
    UserSession user = new UserSession(sessionId, repositoryClient, this);
    users.put(sessionId, user);

    // Media logic for recording
    String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
    user.startRecording(session, sdpOffer);
  }

  private void stop(WebSocketSession session) {
    UserSession user = users.get(session.getId());
    if (user != null) {
      user.stopRecording();
    }
  }

  private void play(WebSocketSession session, JsonObject jsonMessage) {
    UserSession user = users.get(session.getId());
    if (user != null) {
      // Media logic for playing
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();

      user.playRecording(session, sdpOffer);
    }
  }

  private void stopPlay(WebSocketSession session) {
    UserSession user = users.get(session.getId());
    if (user != null) {
      user.stopPlay();
    }
  }

  public void sendPlayEnd(WebSocketSession session) {
    // Send playEnd message to client
    JsonObject response = new JsonObject();
    response.addProperty("id", "playEnd");
    sendMessage(session, new TextMessage(response.toString()));
  }

  private void onIceCandidate(WebSocketSession session, JsonObject jsonMessage) {
    JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();
    UserSession user = users.get(session.getId());
    if (user != null) {
      user.addCandidate(jsonCandidate);
    }
  }

  private void notEnoughResources(WebSocketSession session) {
    // Send notEnoughResources message to client
    JsonObject response = new JsonObject();
    response.addProperty("id", "notEnoughResources");
    sendMessage(session, new TextMessage(response.toString()));

    // Release media session
    release(session);
  }

  private void error(WebSocketSession session, String message) {
    // Send error message to client
    JsonObject response = new JsonObject();
    response.addProperty("id", "error");
    response.addProperty("message", message);
    sendMessage(session, new TextMessage(response.toString()));

    // Release media session
    release(session);
  }

  private void release(WebSocketSession session) {
    UserSession user = users.remove(session.getId());
    if (user != null) {
      user.destroy();
    }
  }

  public synchronized void sendMessage(WebSocketSession session, TextMessage message) {
    try {
      session.sendMessage(message);
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    log.info("Closed websocket connection of session {}", session.getId());
    release(session);
  }
}
