package mtymes.api;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;

import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.ExecutionException;

public class BaseApi {

    private DefaultAsyncHttpClient client = new DefaultAsyncHttpClient(
            new DefaultAsyncHttpClientConfig.Builder()
                    .setKeepEncodingHeader(true)
                    .setReadTimeout(10_000)
                    .setRequestTimeout(10_000)
                    .build()
    );
    private final String hostName;
    private final int port;

    public BaseApi(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    protected ResponseWrapper get(UriBuilder uri) {
        try {
            return wrap(client.prepareGet(uri.build().toString()).execute().get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected ResponseWrapper post(UriBuilder uri) {
        try {
            return wrap(client.preparePost(uri.build().toString()).execute().get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected UriBuilder path(String path) {
        return new ResteasyUriBuilder().path(hostUri()).path(path);
    }

    protected String hostUri() {
        return "http://" + hostName + ":" + port;
    }

    private ResponseWrapper wrap(Response response) {
        return new ResponseWrapper(response);
    }
}
