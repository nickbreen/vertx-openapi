package kiwi.breen.vertx.openapi;

import io.vertx.core.*;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.openapi.RouterBuilder;

public class OpenApiVerticle extends AbstractVerticle
{
    private String address;
    private String openApiSpecUrl;

    OpenApiVerticle(final String address, final String openApiSpecUrl)
    {
        this.address = address;
        this.openApiSpecUrl = openApiSpecUrl;
    }

    @Override
    public void start(final Promise<Void> startPromise) throws Exception
    {
        final OAuth2Auth oauth2Provider = GithubAuth.create(vertx, "aClientId", "aSecret");
        final OAuth2AuthHandler petstoreAuthHandler = OAuth2AuthHandler.create(vertx, oauth2Provider);

        final AuthenticationHandler apiKeyAuthHandler = new AuthenticationHandlerImpl<AuthenticationProvider>(
                (credentials, resultHandler) ->
                {
                    final TokenCredentials tokenCredentials = credentials.mapTo(TokenCredentials.class);
                    resultHandler.handle(Future.succeededFuture(User.fromToken(tokenCredentials.getToken())));
                })
        {
            @Override
            public void parseCredentials(final RoutingContext context, final Handler<AsyncResult<Credentials>> handler)
            {
                final String apiKey = context.request().getHeader("api_key");
                final TokenCredentials tokenCredentials = new TokenCredentials(apiKey);
                handler.handle(Future.succeededFuture(tokenCredentials));
            }
        };

        RouterBuilder.create(vertx, openApiSpecUrl)
                .onFailure(failure -> System.err.printf("%s%n", failure))
                .onSuccess(routerBuilder -> System.out.println(routerBuilder.getOpenAPI().getOpenAPI().toBuffer().toString()))
                .onSuccess(routerBuilder -> routerBuilder.operations().forEach(operation -> operation.routeToEventBus(address)))
                .map(routerBuilder -> routerBuilder
                        .securityHandler("petstore_auth", petstoreAuthHandler)
                        .securityHandler("api_key", apiKeyAuthHandler))
                .map(routerBuilder ->
                {
                    final Router router = Router.router(vertx);
                    router.mountSubRouter("/api/v3", routerBuilder.createRouter()); // from servers[].url
                    return router;
                })
                .flatMap(router -> vertx.createHttpServer().requestHandler(router).listen(8080))
                .onComplete(httpServer -> startPromise.handle(httpServer.mapEmpty()));

    }
}
