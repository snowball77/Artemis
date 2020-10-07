package de.tum.in.www1.artemis.service.connectors.bitbucket;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Profile("bitbucket")
@Component
public class BitbucketAuthorizationInterceptor implements ClientHttpRequestInterceptor {

    @Deprecated
    @Value("${artemis.version-control.user}")
    private String bitbucketUser;

    @Deprecated
    @Value("${artemis.version-control.password}")
    private String bitbucketPassword;

    @Value("${artemis.version-control.token:#{null}}")
    private Optional<String> bitbucketToken;

    @NotNull
    @Override
    public ClientHttpResponse intercept(HttpRequest request, @NotNull byte[] body, @NotNull ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (request.getHeaders().getContentType() == null) {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        }
        // prefer bitbucket token if it is available
        if (bitbucketToken.isPresent()) {
            request.getHeaders().setBearerAuth(bitbucketToken.get());
        }
        else {
            request.getHeaders().setBasicAuth(bitbucketUser, bitbucketPassword);
        }
        return execution.execute(request, body);
    }
}
