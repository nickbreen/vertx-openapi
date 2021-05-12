package kiwi.breen.vertx.openapi;

import io.vertx.core.AbstractVerticle;

public class LoggingVertical extends AbstractVerticle
{
    @Override
    public void start() throws Exception
    {
        vertx.eventBus()
                .addInboundInterceptor(event ->
                {
                    System.err.println(event.body());
                    event.next();
                })
                .addOutboundInterceptor(event ->
                {
                    System.err.println(event.body());
                    event.next();
                });
    }
}
