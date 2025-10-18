package com.example.resourceservice.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OAuth2ClientConfig {

  private static final Logger LOGGER = LogManager.getLogger(OAuth2ClientConfig.class);

  // Bean to store authorized clients (tokens)
  // ClientRegistrationRepository is auto-configured from application yaml
  @Bean
  public OAuth2AuthorizedClientService authorizedClientService(
      ClientRegistrationRepository clientRegistrationRepository) {
    return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
  }

  // Bean for the OAuth2AuthorizedClientRepository
  @Bean
  public OAuth2AuthorizedClientRepository authorizedClientRepository(
      OAuth2AuthorizedClientService authorizedClientService) {
    return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
  }

  // Manager to handle client credentials flow
  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientService authorizedClientService) {

    var authorizedClientManager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);

    var provider = OAuth2AuthorizedClientProviderBuilder.builder()
        .clientCredentials()
        .build();

    authorizedClientManager.setAuthorizedClientProvider(provider);
    return authorizedClientManager;
  }

  // RestTemplate for other services (no token)
  @Bean
  @LoadBalanced
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  // RestTemplate for storage-service with token interceptor
  @Bean
  public RestTemplate storageRestTemplate(OAuth2AuthorizedClientManager authorizedClientManager) {
    RestTemplate restTemplate = new RestTemplate();

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    interceptors.add((request, body, execution) -> {
      OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
          .withClientRegistrationId("storage-service")
          .principal("storage-client") // dummy principal for client_credentials
          .build();

      OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authRequest);
      if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
        String tokenValue = authorizedClient.getAccessToken().getTokenValue();
        LOGGER.info("Received response from Auth Server for getting token {}", tokenValue);
        request.getHeaders().setBearerAuth(tokenValue);
      }

      return execution.execute(request, body);
    });

    restTemplate.setInterceptors(interceptors);
    return restTemplate;
  }
}
