package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

public class BackgroundPoller {

    public static final int CONNECTION_TIMEOUT = 3 * 1000;

    Future<List<String>> pollServices(List<JsonObject> services) {
        services.forEach(s -> s.put("status", pollService(s.getString("url"))));

        return Future.succeededFuture();
    }

    private String pollService(String url) {
        System.out.print("Checking " + url);

        try {
            URL serviceUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.connect();

            System.out.println(" " + connection.getResponseCode());
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return "OK";
            }
        } catch (SocketTimeoutException e) {
            System.out.println(" " + e.getMessage());
            return "FAIL";
        } catch (Exception e) {
            System.out.println(" " + e.getMessage());
            return "FAIL";
        }

        System.out.println(" UNKNOWN");
        return "UNKNOWN";
    }
}
