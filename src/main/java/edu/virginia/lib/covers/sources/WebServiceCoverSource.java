package edu.virginia.lib.covers.sources;

import edu.virginia.lib.covers.CoverImage;
import edu.virginia.lib.covers.CoverSource;
import edu.virginia.lib.covers.Identifier;
import edu.virginia.lib.covers.ImageMetadata;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class WebServiceCoverSource implements CoverSource {

    private static final Logger LOGGER = getLogger(WebServiceCoverSource.class);

    private HttpClient httpClient;

    public WebServiceCoverSource() {
        httpClient = HttpClientBuilder.create().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
    }

    public void setHttpClient(HttpClient client) {
        this.httpClient = client;
    }

    protected InputStream getResponseBody(String url) throws IOException {
        final HttpGet getMethod = new HttpGet(url);
        final HttpResponse response = httpClient.execute(getMethod);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            LOGGER.debug(response.getStatusLine().getStatusCode() + " response from " + url);
            return null;
        } else {
            return response.getEntity().getContent();
        }
    }

    protected class WebServiceCoverImage implements CoverImage {

        private HttpUriRequest request;

        private Identifier id;

        public WebServiceCoverImage(HttpUriRequest request, Identifier id) {
            this.request = request;
            this.id = id;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return httpClient.execute(request).getEntity().getContent();
        }

        @Override
        public ImageMetadata getMetadata() {
            return null;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}
