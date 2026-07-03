package net.opanel.web;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OidcManager {
    private static final long STATE_MAX_AGE_MS = 10 * 60 * 1000; // 10 minutes
    private static final long DISCOVER_REFRESH_MS = 6 * 60 * 60 * 1000; // 6 hours
    private final ConcurrentHashMap<String, StateEntry> stateStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "opanel-oidc-cleanup");
        t.setDaemon(true);
        return t;
    });

    public OidcManager() {
        scheduler.scheduleAtFixedRate(this::cleanExpiredStates, 5, 5, TimeUnit.MINUTES);
    }

    private OIDCProviderMetadata providerMetadata;
    private IDTokenValidator idTokenValidator;
    private boolean discovered = false;
    private long lastDiscoveredAt = 0;

    /**
     * Perform OpenID Connect Discovery and cache the provider metadata.
     */
    public void discover(String discoveryUrl, String clientId) throws Exception {
        if(discoveryUrl.equalsIgnoreCase("/.well-known/openid-configuration")) {
            discoveryUrl = discoveryUrl.replace("/.well-known/openid-configuration", "");
        }
        Issuer issuer = new Issuer(discoveryUrl);
        OIDCProviderMetadata metadata = OIDCProviderMetadata.resolve(issuer);
        this.providerMetadata = metadata;

        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(metadata.getJWKSetURI().toURL());
        JWSKeySelector<SecurityContext> jwsKeySelector = new JWSVerificationKeySelector<>(
                JWSAlgorithm.Family.SIGNATURE, jwkSource
            );
        this.idTokenValidator = new IDTokenValidator(
                metadata.getIssuer(),
                new ClientID(clientId),
                jwsKeySelector,
                null
        );

        this.discovered = true;
        this.lastDiscoveredAt = System.currentTimeMillis();
    }

    public boolean isDiscovered() {
        return discovered;
    }

    /**
     * Whether the provider metadata and JWK source should be refreshed
     * (e.g. after key rotation or metadata changes on the provider side).
     */
    public boolean needDiscovery() {
        return System.currentTimeMillis() - lastDiscoveredAt > DISCOVER_REFRESH_MS;
    }

    /**
     * Build the OIDC authorization URL and generate a state value.
     */
    public String buildAuthorizationUrl(String clientId, String redirectUri) throws Exception {
        if(!discovered || providerMetadata == null) {
            throw new IllegalStateException("OIDC provider has not been discovered yet");
        }

        State state = new State();
        Nonce nonce = new Nonce();

        stateStore.put(state.getValue(), new StateEntry(nonce.getValue(), System.currentTimeMillis()));

        AuthorizationRequest request = new AuthorizationRequest.Builder(
                ResponseType.CODE,
                new ClientID(clientId))
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .redirectionURI(new URI(redirectUri))
                .scope(new Scope("openid", "profile"))
                .state(state)
                .customParameter("nonce", nonce.getValue())
                .build();

        return request.toURI().toString();
    }

    /**
     * Process the OIDC callback: validate state, exchange code for tokens, validate the ID token.
     */
    public JWTClaimsSet handleCallback(String callbackUrl, String clientId, String clientSecret, String redirectUri) throws Exception {
        if(!discovered || providerMetadata == null) {
            throw new IllegalStateException("OIDC provider has not been discovered yet");
        }

        AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(callbackUrl));

        if(!authResponse.indicatesSuccess()) {
            throw new RuntimeException("OIDC authentication failed: " + authResponse.toErrorResponse().getErrorObject().getDescription());
        }

        State responseState = authResponse.getState();
        if(responseState == null) {
            throw new RuntimeException("Missing state parameter in OIDC callback");
        }

        StateEntry stateEntry = stateStore.remove(responseState.getValue());
        if(stateEntry == null) {
            throw new RuntimeException("Invalid or expired state parameter in OIDC callback");
        }

        if(System.currentTimeMillis() - stateEntry.timestamp > STATE_MAX_AGE_MS) {
            throw new RuntimeException("OIDC state parameter has expired");
        }

        AuthorizationCode code = authResponse.toSuccessResponse().getAuthorizationCode();

        TokenRequest tokenRequest;
        ClientID clientIDObj = new ClientID(clientId);
        URI redirectUriObj = new URI(redirectUri);

        if(clientSecret != null && !clientSecret.isEmpty()) {
            ClientAuthentication clientAuth = new ClientSecretBasic(clientIDObj, new Secret(clientSecret));
            tokenRequest = new TokenRequest(
                    providerMetadata.getTokenEndpointURI(),
                    clientAuth,
                    new AuthorizationCodeGrant(code, redirectUriObj));
        } else {
            tokenRequest = new TokenRequest(
                    providerMetadata.getTokenEndpointURI(),
                    clientIDObj,
                    new AuthorizationCodeGrant(code, redirectUriObj));
        }

        TokenResponse tokenResponse;
        try {
            tokenResponse = OIDCTokenResponseParser.parse(tokenRequest.toHTTPRequest().send());
        } catch (IOException e) {
            throw new RuntimeException("OIDC token endpoint unreachable: " + e.getMessage());
        }

        if(!tokenResponse.indicatesSuccess()) {
            throw new RuntimeException("OIDC token request failed: " + tokenResponse.toErrorResponse().getErrorObject().getDescription());
        }

        OIDCTokenResponse oidcTokenResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
        JWT idToken = oidcTokenResponse.getOIDCTokens().getIDToken();

        IDTokenClaimsSet claimsSet;
        try {
            claimsSet = idTokenValidator.validate(idToken, Nonce.parse(stateEntry.nonce));
        } catch (Exception e) {
            throw new RuntimeException("OIDC ID token validation failed: " + e.getMessage());
        }

        return claimsSet.toJWTClaimsSet();
    }

    public void cleanExpiredStates() {
        long now = System.currentTimeMillis();
        stateStore.entrySet().removeIf(entry -> now - entry.getValue().timestamp > STATE_MAX_AGE_MS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if(!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class StateEntry {
        final String nonce;
        final long timestamp;

        StateEntry(String nonce, long timestamp) {
            this.nonce = nonce;
            this.timestamp = timestamp;
        }
    }
}