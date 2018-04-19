package org.elasticsearch.index.analysis.mock;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

public class HttpsUrlStreamHandler extends URLStreamHandler {

    private Map<URL, URLConnection> connections = new HashMap<>();

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        URLConnection  conn = connections.get(url);
        if(conn != null) {
            return conn;
        } else {
            throw new IOException("failed to create URLConnection object for url: "+url.getHost());
        }
    }

    public void resetConnections() {
        connections = new HashMap();
    }

    public HttpsUrlStreamHandler addConnection(URL url, URLConnection urlConnection) {
        connections.put(url, urlConnection);
        return this;
    }
}
