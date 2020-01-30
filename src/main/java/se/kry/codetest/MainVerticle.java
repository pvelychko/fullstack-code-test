package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    public static final int URL_POOLING_INTERVAL = 1000 * 10;
    private List<JsonObject> servicesJson = new ArrayList<>();
    private DBConnector connector;
    private BackgroundPoller poller = new BackgroundPoller();

    private String SQL_ALL_SERVICES = "SELECT * FROM service";
    private String SQL_INSERT_SERVICE = "INSERT INTO service (name, url, date) VALUES (?, ?, datetime('now', 'localtime'))";
    private String SQL_DELETE_SERVICE = "DELETE FROM service WHERE url = ?";

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        fetchServices();
        vertx.setPeriodic(URL_POOLING_INTERVAL, timerId -> poller.pollServices(servicesJson));
        setRoutes(router);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());

        router.get("/service").handler(req -> fetchServices().setHandler(done -> req.response()
                .putHeader("content-type", "application/json")
                .end(new JsonArray(servicesJson).encode()))
        );

        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            saveService(jsonBody);
            req.response()
                    .putHeader("Content-Type", "text/plain")
                    .end("OK");
        });

        router.delete("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            deleteService(jsonBody).setHandler(done -> {
                if (done.succeeded()) {
                    req.response()
                            .setStatusCode(202)
                            .putHeader("Content-Type", "text/plain")
                            .end("Accepted");
                } else {
                    req.response()
                            .setStatusCode(406)
                            .putHeader("Content-Type", "text/plain")
                            .end("Not accepted");
                }
            });
        });
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveService(JsonObject json) {
        String url = checkSqlInjection(json.getString("url"));
        if (!isValidUrl(url)) {
            System.out.println("Unable to add new service, the provided url is not valid: " + url);
            return;
        }

        String name = checkSqlInjection(json.getString("name"));

        connector.query(SQL_INSERT_SERVICE, new JsonArray().add(name).add(url)).setHandler(done -> {
            if (done.succeeded()) {
                System.out.println("Service " + url + " saved successfully");
            } else {
                System.out.println("Service could not be saved to the DB: " + done.cause());
            }
        });
    }

    private String checkSqlInjection(String param) {
        return param == null ? null : param.replace(";", "");
    }

    private Future<Boolean> fetchServices() {
        Future<Boolean> done = Future.future();
        connector.query(SQL_ALL_SERVICES).setHandler(res -> {
            if (res.succeeded()) {
                List<JsonObject> storedServices = res.result().getRows();

                List<String> urls = servicesJson.stream()
                        .map(service -> service.getString("url"))
                        .collect(Collectors.toList());

                for (JsonObject service: storedServices) {
                    String url = service.getString("url");
                    if (!urls.contains(url)) {
                        servicesJson.add(service);
                    }
                }
                done.complete();
            } else {
                System.out.println("Fetching services failed: " + res.cause());
                done.failed();
            }
        });

        return done;
    }

    private Future<Boolean> deleteService(JsonObject service) {
        Future<Boolean> done = Future.future();
        String url = checkSqlInjection(service.getString("url"));

        for (JsonObject s: servicesJson) {
            if (s.getString("url").equals(service.getString("url"))) {
                servicesJson.remove(s);
                break;
            }
        }

        connector.query(SQL_DELETE_SERVICE, new JsonArray().add(url)).setHandler(res -> {
            if (res.succeeded()) {
                System.out.println("Successfully deleted service " + url);
                done.complete(true);
            } else {
                System.out.println("Could not remove service " + url + ", cause: " + res.cause());
                done.failed();
            }
        });

        return done;
    }
}
