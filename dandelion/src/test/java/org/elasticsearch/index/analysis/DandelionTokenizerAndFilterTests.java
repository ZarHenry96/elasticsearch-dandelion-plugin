package org.elasticsearch.index.analysis;

import static org.mockito.BDDMockito.*;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenFilter;
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

public class DandelionTokenizerAndFilterTests extends ESTestCase {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    private static HttpsUrlStreamHandler httpsUrlStreamHandler;
    private HttpURLConnection httpUrlConnection;
    private String params_expected = "";
    private String params_sent= "";

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
        params_expected = "";
        params_sent = "";
    }

    private void configMockResponse(String text, String auth_token, String lang, int responseCode, String data) throws IOException {
        try{
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    String href = "https://api.dandelion.eu/datatxt/nex/v1";
                    params_expected = "text=" + URLEncoder.encode(text, "utf-8") + "&token=" + auth_token + "&lang=" + lang;

                    httpUrlConnection = mock(HttpURLConnection.class);
                    httpsUrlStreamHandler.addConnection(new URL(href), httpUrlConnection);

                    switch(responseCode){
                        case -1:
                            given(httpUrlConnection.getOutputStream()).willThrow(new IOException(data));
                            return null;
                        case HttpURLConnection.HTTP_OK:
                            byte[] expectedDataBytes = data.getBytes();
                            InputStream dataInputStream = new ByteArrayInputStream(expectedDataBytes);
                            given(httpUrlConnection.getInputStream()).willReturn(dataInputStream);
                            break;
                        default:
                            byte[] expectedErrorBytes = data.getBytes();
                            InputStream errorInputStream = new ByteArrayInputStream(expectedErrorBytes);
                            given(httpUrlConnection.getErrorStream()).willReturn(errorInputStream);
                    }

                    given(httpUrlConnection.getOutputStream()).willReturn(new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            params_sent += (char) b;
                        }
                    });

                    given(httpUrlConnection.getResponseCode()).willReturn(responseCode);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    @Test
    public void testTokenizerShouldThrowExceptionIfServerNotAnswers() throws IOException {
        String text = "La Gioconda è un quadro.";
        String auth_token = "token";
        String lang = "auto";
        int responseCode = -1;
        String exceptionMessage = "failed to connect to api.dandelion.eu";

        configMockResponse(text, auth_token, lang, responseCode, exceptionMessage);

        thrown.expect(IOException.class);
        thrown.expectMessage(exceptionMessage);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,"");
        dandelionTokenizer.setReader(new StringReader(text));
        dandelionTokenizer.reset();
    }

    @Test
    public void testTokenizerShouldThrowExceptionIfAuthTokenIsNotSpecified() throws IOException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No authorization token (auth field) specified!");

        Tokenizer dandelionTokenizer = new DandelionTokenizer("","");
    }

    @Test
    public void testTokenizerShouldThrowExceptionIfAuthTokenNotExists() throws IOException{
        String text = "La Gioconda è un quadro.";
        String auth_token = "nonexistentToken";
        String lang = "auto";
        int responseCode = 401;
        String errorData = "{\"error\":true,\"status\":401,\"code\":\"error.invalidParameter\",\"message\":\"no such token 'nonexistentToken'\",\"data\":{\"parameter\":\"token\"}}";
        String exceptionMessage = "no such token 'nonexistentToken' , if you have any problem please contact us at sales@spaziodati.eu";

        configMockResponse(text, auth_token, lang, responseCode, errorData);

        try{
            Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,"");
            dandelionTokenizer.setReader(new StringReader(text));
            dandelionTokenizer.reset();
        } catch (IOException exception){
            assertEquals(exception.getMessage(), exceptionMessage);
        }

        verify(httpUrlConnection,times(1)).setRequestMethod("POST");
        verify(httpUrlConnection,times(1)).getOutputStream();
        assertEquals(params_expected, params_sent);

        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getErrorStream();
    }

    @Test
    public void testTokenizerWithAuthorizedToken() throws IOException {
        String text = "La Gioconda è un quadro.";
        String auth_token = "token";
        String lang = "auto";
        int responseCode = HttpURLConnection.HTTP_OK;
        String responseData = "{\"time\":1,\"annotations\":[{\"start\":3,\"end\":11,\"spot\":\"Gioconda\",\"confidence\":0.907,\"id\":10664,\"title\":\"Gioconda\",\"uri\":\"http://it.wikipedia.org/wiki/Gioconda\",\"label\":\"Gioconda\"},{\"start\":17,\"end\":23,\"spot\":\"quadro\",\"confidence\":0.74,\"id\":3197,\"title\":\"Pittura\",\"uri\":\"http://it.wikipedia.org/wiki/Pittura\",\"label\":\"Pittura\"}],\"lang\":\"it\",\"langConfidence\":1.0,\"timestamp\":\"2018-03-14T12:47:46.572\"}";
        configMockResponse(text,auth_token,lang,responseCode,responseData);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,lang);
        dandelionTokenizer.setReader(new StringReader(text));
        BaseTokenStreamTestCase.assertTokenStreamContents(dandelionTokenizer,
            new String[] {"La ","Gioconda"," è un ","quadro","."},
            new int[] {0,3,11,17,23},
            new int[] {3,11,17,23,24},
            new String[] {"","https://it.wikipedia.org/wiki/Gioconda","","https://it.wikipedia.org/wiki/Pittura",""},
            new int[] {1,1,1,1,1}
        );

        verify(httpUrlConnection,times(1)).setRequestMethod("POST");
        verify(httpUrlConnection,times(1)).getOutputStream();
        assertEquals(params_expected, params_sent);

        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getInputStream();
    }

    @Test
    public void testTokenizerShouldThrowExceptionIfPayloadSizeIsExcessive() throws IOException{
        String text = new String(new char[1048577]);
        String auth_token = "token";
        String exceptionMessage = "request body too large, the current limit is set to 1MiB";

        thrown.expect(IOException.class);
        thrown.expectMessage(exceptionMessage);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,"");
        dandelionTokenizer.setReader(new StringReader(text));
        dandelionTokenizer.reset();
    }

    @Test
    public void testTokenizerShouldThrowExceptionIfTextIsEmptyAndLangNotSpecified() throws IOException{
        String text = "";
        String auth_token = "token";
        String lang = "auto";
        int responseCode = 400;
        String errorData = "{\"message\":\"Cannot detect language:text is empty or null\",\"code\":\"error.cannotDetectLanguage\",\"data\":{},\"error\":true}";
        String exceptionMessage = "Cannot detect language:text is empty or null";

        configMockResponse(text, auth_token, lang, responseCode, errorData);

        try{
            Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,"");
            dandelionTokenizer.setReader(new StringReader(text));
            dandelionTokenizer.reset();
        } catch (IOException ex){
            assertEquals(ex.getMessage(), exceptionMessage);
        }

        verify(httpUrlConnection,times(1)).setRequestMethod("POST");
        verify(httpUrlConnection,times(1)).getOutputStream();
        assertEquals(params_expected, params_sent);

        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getErrorStream();
    }

    @Test
    public void testTokenizerShouldThrowExceptionIfLangNotExists() throws IOException{
        String auth_token = "token";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal language (lang) parameter! Check on dandelion.eu the possible values; if not specified auto will be used.");

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,"nonexlang");
    }

    @Test
    public void testTokenizerWithAuthorizedTokenAndExistingLang() throws IOException {
        String text = "Mona Lisa.";
        String auth_token = "token";
        String lang = "en";
        int responseCode = HttpURLConnection.HTTP_OK;
        String responseData = "{\"time\":1,\"annotations\":[{\"start\":0,\"end\":9,\"spot\":\"Mona Lisa\",\"confidence\":0.7962,\"id\":70889,\"title\":\"Mona Lisa\",\"uri\":\"http://en.wikipedia.org/wiki/Mona_Lisa\",\"label\":\"Mona Lisa\"}],\"lang\":\"en\",\"timestamp\":\"2018-03-14T12:45:44.766\"}";

        configMockResponse(text,auth_token,lang,responseCode,responseData);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,lang);
        dandelionTokenizer.setReader(new StringReader(text));
        BaseTokenStreamTestCase.assertTokenStreamContents(dandelionTokenizer,
            new String[] {"Mona Lisa","."},
            new int[] {0,9},
            new int[] {9,10},
            new String[] {"https://en.wikipedia.org/wiki/Mona_Lisa",""},
            new int[] {1,1}
        );

        verify(httpUrlConnection,times(1)).setRequestMethod("POST");
        verify(httpUrlConnection,times(1)).getOutputStream();
        assertEquals(params_expected, params_sent);

        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getInputStream();
    }

    @Test
    public void testTokenFilterWithoutEntityTokens() throws IOException{
        String text = "Di a da in con su per tra fra.";
        String auth_token = "token";
        String lang = "auto";
        int responseCode = HttpURLConnection.HTTP_OK;
        String responseData = "{\"time\":0,\"annotations\":[],\"lang\":\"it\",\"timestamp\":\"2018-03-14T14:17:34.315\"}";
        configMockResponse(text,auth_token,lang,responseCode,responseData);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,lang);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,null);

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {},
            new int[] {},
            new int[] {},
            new String[] {},
            new int[] {}
        );

        verify(httpUrlConnection,times(1)).setRequestMethod("POST");
        verify(httpUrlConnection,times(1)).getOutputStream();
        assertEquals(params_expected, params_sent);

        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getInputStream();
    }

    @Test
    public void testTokenFilterWithEntityTokens() throws IOException{
        String text = "La Torre Eiffel si trova a Parigi.";
        String auth_token = "token";
        String lang = "auto";
        int responseCode = HttpURLConnection.HTTP_OK;
        String responseData = "{\"time\":2,\"annotations\":[{\"start\":3,\"end\":15,\"spot\":\"Torre Eiffel\",\"confidence\":0.9219,\"id\":10357,\"title\":\"Torre Eiffel\",\"uri\":\"http://it.wikipedia.org/wiki/Torre_Eiffel\",\"label\":\"Torre Eiffel\"},{\"start\":27,\"end\":33,\"spot\":\"Parigi\",\"confidence\":0.899,\"id\":3198,\"title\":\"Parigi\",\"uri\":\"http://it.wikipedia.org/wiki/Parigi\",\"label\":\"Parigi\"}],\"lang\":\"it\",\"timestamp\":\"2018-03-14T14:07:49.927\"}";
        configMockResponse(text,auth_token,lang,responseCode,responseData);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,lang);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,"");

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {"https://it.wikipedia.org/wiki/Torre_Eiffel","https://it.wikipedia.org/wiki/Parigi"},
            new int[] {3,27},
            new int[] {15,33},
            new String[] {"word","word"},
            new int[] {2,2}
        );

        verify(httpUrlConnection,times(1)).setRequestMethod("POST");
        verify(httpUrlConnection,times(1)).getOutputStream();
        assertEquals(params_expected, params_sent);

        verify(httpUrlConnection,times(1)).getResponseCode();
        verify(httpUrlConnection,times(1)).getInputStream();
    }

}
