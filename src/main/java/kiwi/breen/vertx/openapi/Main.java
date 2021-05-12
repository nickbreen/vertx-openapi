package kiwi.breen.vertx.openapi;

import io.vertx.core.*;

public class Main
{
    public static final String ADDRESS = "openApiAddress";

    public static void main(String[] args)
    {
        final Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new OpenApiVerticle(ADDRESS, "openapi.yaml"));
        vertx.deployVerticle(new LoggingVertical());
        vertx.deployVerticle(new OperationsVertical());
    }

}
