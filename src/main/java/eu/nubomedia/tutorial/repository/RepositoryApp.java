/*
 * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package eu.nubomedia.tutorial.repository;

import org.kurento.repository.RepositoryClient;
import org.kurento.repository.RepositoryClientProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebRTC in loopback with recording in repository capabilities main class.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 6.4.1
 */
@SpringBootApplication
@EnableWebSocket
public class RepositoryApp implements WebSocketConfigurer {

  @Bean
  public RepositoryHandler handler() {
    return new RepositoryHandler();
  }

  @Bean
  public RepositoryClient repositoryServiceProvider() {
    return RepositoryClientProvider.create();
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler(), "/repository");
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(RepositoryApp.class).run(args);
  }
}
