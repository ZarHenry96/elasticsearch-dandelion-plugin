package org.elasticsearch.index.analysis.mock;

import java.net.URL;
import java.net.URLStreamHandlerFactory;

import static org.elasticsearch.mock.orig.Mockito.mock;
import static org.mockito.BDDMockito.given;

public class URLStreamHandlerFactoryUtils {

    private static HttpsUrlStreamHandler httpsUrlStreamHandler = null;

    public static HttpsUrlStreamHandler getHttpsUrlStreamHandler (){
        if(httpsUrlStreamHandler == null){
            URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
            httpsUrlStreamHandler = new HttpsUrlStreamHandler();

            URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);
            given(urlStreamHandlerFactory.createURLStreamHandler("https")).willReturn(httpsUrlStreamHandler);
        }
        return httpsUrlStreamHandler;
    }

}
