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

import java.util.Date;

import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

/**
 * User session.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.4.1
 */
public class UserSession {

  private final Logger log = LoggerFactory.getLogger(UserSession.class);

  private String id;
  private WebRtcEndpoint webRtcEndpoint;
  private MediaPipeline mediaPipeline;
  private RepositoryItemRecorder repoItem;
  private Date stopTimestamp;
  private KurentoClient kurentoClient;

  public UserSession(WebSocketSession session) {
    this.id = session.getId();

    // One KurentoClient instance per session
    kurentoClient = KurentoClient.create();
    log.info("Created kurentoClient (session {})", id);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WebRtcEndpoint getWebRtcEndpoint() {
    return webRtcEndpoint;
  }

  public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
    this.webRtcEndpoint = webRtcEndpoint;
  }

  public MediaPipeline getMediaPipeline() {
    return mediaPipeline;
  }

  public void setMediaPipeline(MediaPipeline mediaPipeline) {
    this.mediaPipeline = mediaPipeline;
  }

  public KurentoClient getKurentoClient() {
    return kurentoClient;
  }

  public RepositoryItemRecorder getRepoItem() {
    return repoItem;
  }

  public void setRepoItem(RepositoryItemRecorder repoItem) {
    this.repoItem = repoItem;
  }

  public void addCandidate(IceCandidate candidate) {
    webRtcEndpoint.addIceCandidate(candidate);
  }

  public Date getStopTimestamp() {
    return stopTimestamp;
  }

  public void release() {
    this.mediaPipeline.release();
    this.webRtcEndpoint = null;
    this.mediaPipeline = null;
    if (this.stopTimestamp == null) {
      this.stopTimestamp = new Date();
    }

    log.info("Releasing media pipeline {} (session {})", getMediaPipeline().getId(), getId());
    getMediaPipeline().release();
    log.info("Destroying kurentoClient (session {})", getId());
    getKurentoClient().destroy();
  }
}
