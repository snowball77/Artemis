package de.tum.in.www1.artemis.config.websocket;

import static de.tum.in.www1.artemis.service.WebsocketMessagingService.getExerciseIdFromResultDestination;
import static de.tum.in.www1.artemis.service.WebsocketMessagingService.isResultNonPersonalDestination;
import static de.tum.in.www1.artemis.web.websocket.team.ParticipationTeamWebsocketService.getParticipationIdFromDestination;
import static de.tum.in.www1.artemis.web.websocket.team.ParticipationTeamWebsocketService.isParticipationTeamDestination;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.security.AuthoritiesConstants;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.ExerciseService;
import de.tum.in.www1.artemis.service.ParticipationService;
import de.tum.in.www1.artemis.service.UserService;
import de.tum.in.www1.artemis.validation.InetSocketAddressValidator;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

@Configuration
// See https://stackoverflow.com/a/34337731/3802758
public class WebsocketConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

    private final Logger log = LoggerFactory.getLogger(WebsocketConfiguration.class);

    public static final String IP_ADDRESS = "IP_ADDRESS";

    private final Environment env;

    private final ObjectMapper objectMapper;

    private final TaskScheduler messageBrokerTaskScheduler;

    private final TaskScheduler taskScheduler;

    private ParticipationService participationService;

    private AuthorizationCheckService authorizationCheckService;

    private UserService userService;

    private ExerciseService exerciseService;

    private static final int LOGGING_DELAY_SECONDS = 10;

    // Split the addresses by comma
    @Value("#{'${spring.websocket.broker.addresses}'.split(',')}")
    private List<String> brokerAddresses;

    @Value("${spring.websocket.broker.username}")
    private String brokerUsername;

    @Value("${spring.websocket.broker.password}")
    private String brokerPassword;

    public WebsocketConfiguration(Environment env, MappingJackson2HttpMessageConverter springMvcJacksonConverter, TaskScheduler messageBrokerTaskScheduler,
            TaskScheduler taskScheduler, AuthorizationCheckService authorizationCheckService, @Lazy ExerciseService exerciseService, UserService userService) {
        this.env = env;
        this.objectMapper = springMvcJacksonConverter.getObjectMapper();
        this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
        this.taskScheduler = taskScheduler;
        this.authorizationCheckService = authorizationCheckService;
        this.exerciseService = exerciseService;
        this.userService = userService;
    }

    @Autowired
    public void setParticipationService(ParticipationService participationService) {
        this.participationService = participationService;
    }

    /**
     * initialize the websocket configuration: activate logging when the profile websocketLog is active
     */
    @PostConstruct
    public void init() {
        // using Autowired leads to a weird bug, because the order of the method execution is changed. This somehow prevents messages send to single clients
        // later one, e.g. in the code editor. Therefore we call this method here directly to get a reference and adapt the logging period!
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        // Note: this mechanism prevents that this is logged during testing
        if (activeProfiles.contains("websocketLog")) {
            final var webSocketMessageBrokerStats = webSocketMessageBrokerStats();
            webSocketMessageBrokerStats.setLoggingPeriod(LOGGING_DELAY_SECONDS * 1000);

            taskScheduler.scheduleAtFixedRate(() -> {
                final var subscriptionCount = userRegistry().getUsers().stream().flatMap(simpUser -> simpUser.getSessions().stream())
                        .map(simpSession -> simpSession.getSubscriptions().size()).reduce(0, Integer::sum);
                log.info("Currently active websocket subscriptions: " + subscriptionCount);
            }, LOGGING_DELAY_SECONDS * 1000);
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Try to create a TCP client that will connect to the message broker (or the message brokers if multiple exists).
        // If tcpClient is null, there is no valid address specified in the config. This could be due to a development setup or a mistake in the config.
        TcpOperations<byte[]> tcpClient = createTcpClient();
        if (tcpClient != null) {
            log.info("Enabling StompBrokerRelay for WebSocket messages using " + String.join(", ", brokerAddresses));
            config
                    // Enable the relay for "/topic"
                    .enableStompBrokerRelay("/topic")
                    // Messages that could not be sent to an user (as he is not connected to this server) will be forwarded to "/topic/unresolved-user"
                    .setUserDestinationBroadcast("/topic/unresolved-user")
                    // Information about connected users will be sent to "/topic/user-registry"
                    .setUserRegistryBroadcast("/topic/user-registry")
                    // Set client username and password to the one loaded from the config
                    .setClientLogin(brokerUsername).setClientPasscode(brokerPassword)
                    // Set system username and password to the one loaded from the config
                    .setSystemLogin(brokerUsername).setSystemPasscode(brokerPassword)
                    // Set the TCP client to the one generated above
                    .setTcpClient(tcpClient);
        }
        else {
            log.info("Did NOT enable StompBrokerRelay for WebSocket messages");
            config.enableSimpleBroker("/topic").setHeartbeatValue(new long[] { 10000, 20000 }).setTaskScheduler(messageBrokerTaskScheduler);
        }
    }

    /**
     * Create a TCP client that will connect to the broker defined in the config.
     * If multiple brokers are configured, the client will connect to the first one and fail over to the next one in case a broker goes down.
     * If the last broker goes down, the first one is retried.
     * Also see https://github.com/spring-projects/spring-framework/issues/17057 and
     * https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-stomp-handle-broker-relay-configure
     * @return a TCP client with a round robin use
     */
    private ReactorNettyTcpClient<byte[]> createTcpClient() {
        final List<InetSocketAddress> brokerAddressList = brokerAddresses.stream().map(InetSocketAddressValidator::getValidAddress).filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        // Return null if no valid addresses can be found. This is e.g. due to a invalid config or a development setup without a broker.
        if (!brokerAddressList.isEmpty()) {
            // This provides a round-robin use of the brokers, we only want to fail over to the fallback broker if the primary broker fails, so we have the same order of brokers in
            // all nodes
            Iterator<InetSocketAddress> addressIterator = Iterables.cycle(brokerAddressList).iterator();
            return new ReactorNettyTcpClient<>(client -> client.remoteAddress(addressIterator::next), new StompReactorNettyCodec());
        }
        return null;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        DefaultHandshakeHandler handshakeHandler = defaultHandshakeHandler();
        // NOTE: by setting a WebSocketTransportHandler we disable http poll, http stream and other exotic workarounds and only support real websocket connections.
        // nowadays all modern browsers support websockets and workarounds are not necessary any more and might only lead to problems
        WebSocketTransportHandler webSocketTransportHandler = new WebSocketTransportHandler(handshakeHandler);
        registry.addEndpoint("/websocket/tracker").setAllowedOrigins("*").withSockJS().setTransportHandlers(webSocketTransportHandler)
                .setInterceptors(httpSessionHandshakeInterceptor());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new TopicSubscriptionInterceptor());
    }

    @NotNull
    @Override
    protected MappingJackson2MessageConverter createJacksonConverter() {
        // NOTE: We need to adapt the default messageConverter for WebSocket messages
        // with a messageConverter that uses the same ObjectMapper that our REST endpoints use.
        // This gives us consistency in how specific datatypes are serialized (e.g. timestamps)
        MappingJackson2MessageConverter converter = super.createJacksonConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    /**
     * @return initialize the handshake interceptor stores the remote IP address before handshake
     */
    @Bean
    public HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {

            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                    attributes.put(IP_ADDRESS, servletRequest.getRemoteAddress());
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
                if (exception != null) {
                    log.warn("Exception occurred in WS.afterHandshake: " + exception.getMessage());
                }
            }
        };
    }

    private DefaultHandshakeHandler defaultHandshakeHandler() {
        return new DefaultHandshakeHandler() {

            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                Principal principal = request.getPrincipal();
                if (principal == null) {
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS));
                    principal = new AnonymousAuthenticationToken("WebsocketConfiguration", "anonymous", authorities);
                }
                log.debug("determineUser: " + principal);
                return principal;
            }
        };
    }

    public class TopicSubscriptionInterceptor implements ChannelInterceptor {

        /**
         * Method is called before the user's message is sent to the controller
         *
         * @param message Message that the websocket client is sending (e.g. SUBSCRIBE, MESSAGE, UNSUBSCRIBE)
         * @param channel Current message channel
         * @return message that gets passed along further
         */
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
            Principal principal = headerAccessor.getUser();
            String destination = headerAccessor.getDestination();

            if (StompCommand.SUBSCRIBE.equals(headerAccessor.getCommand())) {
                try {
                    if (!allowSubscription(principal, destination)) {
                        logUnauthorizedDestinationAccess(principal, destination);
                        return null; // erase the forbidden SUBSCRIBE command the user was trying to send
                    }
                }
                catch (EntityNotFoundException e) {
                    // If the user is not found (e.g. because he is not logged in), he should not be able to subscribe to these topics
                    log.warn("An error occurred while subscribing user {} to destination {}: {}", principal.getName(), destination, e.getMessage());
                    return null;
                }
            }

            return message;
        }

        /**
         * Returns whether the subscription of the given principal to the given destination is permitted
         *
         * @param principal User principal of the user who wants to subscribe
         * @param destination Destination topic to which the user wants to subscribe
         * @return flag whether subscription is allowed
         */
        private boolean allowSubscription(Principal principal, String destination) {
            if (isParticipationTeamDestination(destination)) {
                Long participationId = getParticipationIdFromDestination(destination);
                return isParticipationOwnedByUser(principal, participationId);
            }
            if (isResultNonPersonalDestination(destination)) {
                Long exerciseId = getExerciseIdFromResultDestination(destination);

                // TODO: Is it right that TAs are not allowed to subscribe to exam exercises?
                Exercise exercise = exerciseService.findOne(exerciseId);
                if (exercise.hasExerciseGroup()) {
                    return isUserInstructorOrHigherForExercise(principal, exercise);
                }
                else {
                    return isUserTAOrHigherForExercise(principal, exercise);
                }
            }
            return true;
        }

        private void logUnauthorizedDestinationAccess(Principal principal, String destination) {
            if (principal == null) {
                log.warn("Anonymous user tried to access the protected topic: " + destination);
            }
            else {
                log.warn("User with login '" + principal.getName() + "' tried to access the protected topic: " + destination);
            }
        }
    }

    private boolean isParticipationOwnedByUser(Principal principal, Long participationId) {
        StudentParticipation participation = participationService.findOneStudentParticipation(participationId);
        return participation.isOwnedBy(principal.getName());
    }

    private boolean isUserInstructorOrHigherForExercise(Principal principal, Exercise exercise) {
        User user = userService.getUserWithGroupsAndAuthorities(principal.getName());
        return authorizationCheckService.isAtLeastInstructorInCourse(exercise.getCourseViaExerciseGroupOrCourseMember(), user);
    }

    private boolean isUserTAOrHigherForExercise(Principal principal, Exercise exercise) {
        User user = userService.getUserWithGroupsAndAuthorities(principal.getName());
        return authorizationCheckService.isAtLeastTeachingAssistantForExercise(exercise, user);
    }
}
