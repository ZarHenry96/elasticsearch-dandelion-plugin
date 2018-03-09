package org.elasticsearch.index.analysis;

import static org.mockito.BDDMockito.*;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.index.analysis.mock.HttpsUrlStreamHandler;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class DandelionTokenizerTests extends ESTestCase {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    private static HttpsUrlStreamHandler httpsUrlStreamHandler;
    private HttpURLConnection httpUrlConnection;

    @BeforeClass
    public static void setupURLStreamHandlerFactory() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                URLStreamHandlerFactory urlStreamHandlerFactory = mock(URLStreamHandlerFactory.class);
                URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

                httpsUrlStreamHandler = new HttpsUrlStreamHandler();
                given(urlStreamHandlerFactory.createURLStreamHandler("https")).willReturn(httpsUrlStreamHandler);
                return null;
            }
        });
    }

    @Before
    public void reset() {
        httpsUrlStreamHandler.resetConnections();
        httpUrlConnection = null;
    }

    private void configMockResponse(String text, String auth_token, String lang, String responseData, Integer responseCode) throws IOException {
        try{
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    String reqParams = "?text=" + URLEncoder.encode(text, "utf-8") + "&token=" + auth_token + "&lang=" + lang;
                    String href = "https://api.dandelion.eu/datatxt/nex/v1"+reqParams;

                    httpUrlConnection = mock(HttpURLConnection.class);
                    httpsUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);

                    if(responseCode.equals(HttpURLConnection.HTTP_OK)) {
                        byte[] expectedDataBytes = responseData.getBytes();
                        InputStream dataInputStream = new ByteArrayInputStream(expectedDataBytes);
                        given(httpUrlConnection.getInputStream()).willReturn(dataInputStream);
                    } else {
                        given(httpUrlConnection.getInputStream()).willThrow(new IOException());
                    }
                    given(httpUrlConnection.getResponseCode()).willReturn(responseCode);

                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    @Test
    public void testRight() throws IOException {
        String text = "La Gioconda è un quadro.";
        String auth_token = "token";
        String lang = "auto";
        String responseData = "{\"lang\":\"it\",\"timestamp\":\"2018-03-09T14:06:56.812\",\"langConfidence\":1.0,\"topEntities\":[{\"score\":0.5,\"id\":3197,\"uri\":\"http://it.wikipedia.org/wiki/Pittura\"},{\"score\":0.5,\"id\":10664,\"uri\":\"http://it.wikipedia.org/wiki/Gioconda\"}],\"time\":2,\"annotations\":[{\"confidence\":0.907,\"end\":11,\"lod\":{\"dbpedia\":\"http://it.dbpedia.org/resource/Gioconda...\",\"wikipedia\":\"http://it.wikipedia.org/wiki/Gioconda\"},\"title\":\"Gioconda\",\"abstract\":\"LaGioconda,notaanchecomeMonnaLisa...\",\"spot\":\"Gioconda\",\"uri\":\"http://it.wikipedia.org/wiki/Gioconda\",\"label\":\"Gioconda\",\"start\":3,\"categories\":[\"DipintidiLeonardo\",\"Ritrattipittoricifemminili\",\"DipintinelLouvre\"],\"id\":10664,\"image\":{\"full\":\"https://commons.wikimedia.org/wiki/Spec...\",\"thumbnail\":\"https://commons.wikimedia.org/wiki/Spec...\"},\"types\":[\"http://dbpedia.org/ontology/A...\",\"http://dbpedia.org/ontology/W...\"]},{\"confidence\":0.74,\"end\":23,\"lod\":{\"dbpedia\":\"http://it.dbpedia.org/resource/Pittura\",\"wikipedia\":\"http://it.wikipedia.org/wiki/Pittura\"},\"title\":\"Pittura\",\"abstract\":\"Lapittura\\u00e8l'artecheconsisten...\",\"spot\":\"quadro\",\"uri\":\"http://it.wikipedia.org/wiki/Pittura\",\"label\":\"Pittura\",\"start\":17,\"categories\":[\"Pittura\",\"Tecnicheartistiche\"],\"id\":3197,\"image\":{\"full\":\"https://commons.wikimedia.org/wiki/Spec...\",\"thumbnail\":\"https://commons.wikimedia.org/wiki/Spec...\"},\"types\":[]}]}";
        Integer responseCode = HttpURLConnection.HTTP_OK;

        configMockResponse(text,auth_token,lang,responseData, responseCode);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,lang);
        dandelionTokenizer.setReader(new StringReader(text));
        BaseTokenStreamTestCase.assertTokenStreamContents(dandelionTokenizer,
            new String[] {"La ","Gioconda"," è un ","quadro","."},
            new int[] {0,3,11,17,23},
            new int[] {3,11,17,23,24},
            new String[] {"","http://it.wikipedia.org/wiki/Gioconda","","http://it.wikipedia.org/wiki/Pittura",""},
            new int[] {1,1,1,1,1}
        );
        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getInputStream();
    }

    @Test
    public void testWithServerDown() throws IOException {
        String text = "La Gioconda è un quadro.";
        String auth_token = "token";
        String lang = "auto";

        thrown.expect(IOException.class);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,lang);
        dandelionTokenizer.setReader(new StringReader(text));
        dandelionTokenizer.reset();
    }

}
