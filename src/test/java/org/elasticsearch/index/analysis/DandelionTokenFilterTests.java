package org.elasticsearch.index.analysis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.index.analysis.mock.HttpsUrlStreamHandler;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalAnswers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.mock.orig.Mockito.*;
import static org.mockito.BDDMockito.given;
import org.elasticsearch.index.analysis.mock.URLStreamHandlerFactoryUtils;

public class DandelionTokenFilterTests extends ESTestCase {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    private static HttpsUrlStreamHandler httpsUrlStreamHandler;
    private HttpURLConnection tokenizerHttpUrlConnection;
    private HttpURLConnection tokenFilterHttpUrlConnection;
    private String params_expected = "";
    private String params_sent= "";

    @BeforeClass
    public static void setupURLStreamHandlerFactory() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                httpsUrlStreamHandler = URLStreamHandlerFactoryUtils.getHttpsUrlStreamHandler();
                return null;
            }
        });
    }

    @Before
    public void reset() {
        httpsUrlStreamHandler.resetConnections();
        tokenizerHttpUrlConnection = null;
        tokenFilterHttpUrlConnection = null;
        params_expected = "";
        params_sent = "";
    }

    private void configTokenizerMockResponse(String data) throws IOException {
        try{
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    String href = "https://api.dandelion.eu/datatxt/nex/v1";

                    tokenizerHttpUrlConnection = mock(HttpURLConnection.class);
                    httpsUrlStreamHandler.addConnection(new URL(href), tokenizerHttpUrlConnection);

                    byte[] expectedDataBytes = data.getBytes();
                    InputStream dataInputStream = new ByteArrayInputStream(expectedDataBytes);
                    given(tokenizerHttpUrlConnection.getInputStream()).willReturn(dataInputStream);

                    given(tokenizerHttpUrlConnection.getOutputStream()).willReturn(new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            // do nothing
                        }
                    });

                    given(tokenizerHttpUrlConnection.getResponseCode()).willReturn(HttpURLConnection.HTTP_OK);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    private void configTokenFilterMockResponse(String lang, int status, String[] reqTitles, String[] data) throws IOException {
        try{
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    String href = "https://"+lang+".wikipedia.org/w/api.php";

                    tokenFilterHttpUrlConnection = mock(HttpURLConnection.class);
                    httpsUrlStreamHandler.addConnection(new URL(href), tokenFilterHttpUrlConnection);

                    if(status == -1){
                        given(tokenFilterHttpUrlConnection.getOutputStream()).willThrow(new IOException("failed to connect to "+lang+".wikipedia.org"));
                        return null;
                    }

                    List<InputStream> responses = new ArrayList<>();
                    for(int i = 0; i< reqTitles.length; i++) {
                        String title = reqTitles[i];
                        params_expected+="action=query&titles="+title+"&prop=langlinks&lllimit=500&llprop=url&format=json";

                        String response_data = data[i];
                        byte[] expectedDataBytes = response_data.getBytes();
                        responses.add(new ByteArrayInputStream(expectedDataBytes));
                    }

                    when(tokenFilterHttpUrlConnection.getInputStream()).thenAnswer(AdditionalAnswers.returnsElementsOf(responses));

                    given(tokenFilterHttpUrlConnection.getOutputStream()).willReturn(new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            params_sent+=(char) b;
                        }
                    });

                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    @Test
    public void testTokenFilterWithoutEntityTokens() throws IOException{
        String text = "Di a da in con su per tra fra.";
        String auth_token = "token";
        String langT = "auto";
        String responseDataT = "{\"time\":0,\"annotations\":[],\"lang\":\"it\",\"timestamp\":\"2018-03-14T14:17:34.315\"}";
        configTokenizerMockResponse(responseDataT);

        String multilang = null;

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,langT);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,multilang);

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {},
            new int[] {},
            new int[] {},
            new String[] {},
            new int[] {}
        );
    }

    @Test
    public void testTokenFilterWithDifferentTokenizer() throws IOException{
        String text = "Trento è una città del Nord Italia.";
        String multilang = "false";

        Tokenizer tokenizer = new MockTokenizer(); //whitespace
        tokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(tokenizer,multilang);

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {},
            new int[] {},
            new int[] {},
            new String[] {},
            new int[] {}
        );
    }

    @Test
    public void testTokenFilterWithEntityTokensAndStandardSettings() throws IOException{
        String text = "Trento è una città del Nord Italia.";
        String auth_token = "token";
        String langT = "auto";
        String responseDataT = "{\"time\":2,\"annotations\":[{\"start\":0,\"end\":6,\"spot\":\"Trento\",\"confidence\":0.8641,\"id\":100137,\"title\":\"Trento\",\"uri\":\"http://it.wikipedia.org/wiki/Trento\",\"label\":\"Trento\"},{\"start\":13,\"end\":18,\"spot\":\"città\",\"confidence\":0.7667,\"id\":2419919,\"title\":\"L\\u0027Aquila\",\"uri\":\"http://it.wikipedia.org/wiki/L%27Aquila\",\"label\":\"L\\u0027Aquila\"},{\"start\":23,\"end\":34,\"spot\":\"Nord Italia\",\"confidence\":0.7928,\"id\":1079578,\"title\":\"Italia settentrionale\",\"uri\":\"http://it.wikipedia.org/wiki/Italia_settentrionale\",\"label\":\"Italia settentrionale\"}],\"lang\":\"it\",\"langConfidence\":1.0,\"timestamp\":\"2018-04-12T08:55:36.282\"}";
        configTokenizerMockResponse(responseDataT);

        String multilang = null;

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,langT);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,multilang);

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {"https://it.wikipedia.org/wiki/Trento","https://it.wikipedia.org/wiki/L%27Aquila","https://it.wikipedia.org/wiki/Italia_settentrionale"},
            new int[] {0,13,23},
            new int[] {6,18,34},
            new String[] {"word","word","word"},
            new int[] {1,2,2}
        );
    }

    @Test
    public void testTokenFilterShouldThrowExceptionIfMultilangValueIsInvalid() throws IOException{
        String text = "La Gioconda è un dipinto di Leonardo.";
        String multilang = "Ciao";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal multilang parameter value: only true/false are allowed!");

        Tokenizer tokenizer = new MockTokenizer(); //whitespace
        tokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(tokenizer,multilang);
    }

    @Test
    public void testTokenFilterShouldThrowExceptionIfWikipediaServerNotAnswers() throws IOException {
        String text = "In Inghilterra spesso piove.";
        String auth_token = "token";
        String langT = "auto";
        String multilang = "true";

        String responseDataT = "{\"time\":1,\"annotations\":[{\"start\":3,\"end\":14,\"spot\":\"Inghilterra\",\"confidence\":0.7891,\"id\":713477,\"title\":\"Inghilterra\",\"uri\":\"http://it.wikipedia.org/wiki/Inghilterra\",\"label\":\"Inghilterra\"}],\"lang\":\"it\",\"langConfidence\":1.0,\"timestamp\":\"2018-04-12T09:08:41.798\"}";
        configTokenizerMockResponse(responseDataT);

        String langTF= "it";
        int status = -1;
        String [] reqTitles = new String[] {"Inghilterra"};
        String [] responseDataTF = new String[] {};
        configTokenFilterMockResponse(langTF,status,reqTitles,responseDataTF);

        String exceptionMessage = "failed to connect to "+langTF+".wikipedia.org";

        thrown.expect(IOException.class);
        thrown.expectMessage(exceptionMessage);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,langT);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,multilang);

        //It's needed to start the analysis process
        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {},
            new int[] {},
            new int[] {},
            new String[] {},
            new int[] {}
        );
    }

    @Test
    public void testTokenFilterWithMultiLangEnabledAndNegativeWikipediaResponse() throws IOException {
        String text = "La Torre Eiffel è altissima.";
        String auth_token = "token";
        String langT = "auto";
        String multilang = "true";

        String responseDataT = "{\"time\":1,\"annotations\":[{\"start\":3,\"end\":15,\"spot\":\"Torre Eiffel\",\"confidence\":0.9219,\"id\":10357,\"title\":\"Torre Eiffel\",\"uri\":\"http://it.wikipedia.org/wiki/Torre_Eifel\",\"label\":\"Torre Eiffel\"}],\"lang\":\"it\",\"langConfidence\":0.5001,\"timestamp\":\"2018-04-12T07:23:44.406\"}";
        configTokenizerMockResponse(responseDataT);

        String langTF= "it";
        int status = 200;
        String [] reqTitles = new String[] {"Torre_Eifel"};
        String [] responseDataTF = new String[] {"{\"batchcomplete\":\"\",\"query\":{\"normalized\":[{\"from\":\"Torre_Eifel\",\"to\":\"Torre Eifel\"}],\"pages\":{\"-1\":{\"ns\":0,\"title\":\"Torre Eifel\",\"missing\":\"\"}}}}"};
        configTokenFilterMockResponse(langTF,status,reqTitles,responseDataTF);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,langT);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,multilang);

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {"https://it.wikipedia.org/wiki/Torre_Eifel"},
            new int[] {3},
            new int[] {15},
            new String[] {"word"},
            new int[] {2}
        );

        int numberOfTimes = reqTitles.length;
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).setRequestMethod("POST");
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).getOutputStream();
        assertEquals(params_expected, params_sent);
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).getInputStream();
    }

    @Test
    public void testTokenFilterWithMultiLangEnabledAndWithoutLanglinks() throws IOException {
        String text = "La Torre Eiffel è altissima.";
        String auth_token = "token";
        String langT = "auto";
        String multilang = "true";

        String responseDataT = "{\"time\":1,\"annotations\":[{\"start\":3,\"end\":15,\"spot\":\"Torre Eiffel\",\"confidence\":0.9219,\"id\":10357,\"title\":\"Torre Eiffel\",\"uri\":\"http://it.wikipedia.org/wiki/Torre_Eiffel\",\"label\":\"Torre Eiffel\"}],\"lang\":\"it\",\"langConfidence\":0.5001,\"timestamp\":\"2018-04-12T07:23:44.406\"}";
        configTokenizerMockResponse(responseDataT);

        String langTF= "it";
        int status = 200;
        String [] reqTitles = new String[] {"Torre_Eiffel"};
        String [] responseDataTF = new String[] {"{\"batchcomplete\":\"\",\"query\":{\"normalized\":[{\"from\":\"Torre_Eiffel\",\"to\":\"Torre Eiffel\"}],\"pages\":{\"10357\":{\"pageid\":10357,\"ns\":0,\"title\":\"Torre Eiffel\"}}}}"};
        configTokenFilterMockResponse(langTF,status,reqTitles,responseDataTF);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,langT);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,multilang);

        BaseTokenStreamTestCase.assertTokenStreamContents(tokenFilter,
            new String[] {"https://it.wikipedia.org/wiki/Torre_Eiffel"},
            new int[] {3},
            new int[] {15},
            new String[] {"word"},
            new int[] {2}
        );

        int numberOfTimes = reqTitles.length;
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).setRequestMethod("POST");
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).getOutputStream();
        assertEquals(params_expected, params_sent);
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).getInputStream();
    }

    @Test
    public void testTokenFilterWithMultiLangEnabled() throws IOException{
        InputStream inputStream = DandelionTokenFilterTests.class.getResourceAsStream("multiLangEnabledSettings.json");
        Reader reader = new InputStreamReader(inputStream);
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(reader, JsonObject.class);

        String text = json.get("text").getAsString();
        String auth_token = json.get("auth_token").getAsString();
        String langT = json.get("langT").getAsString();
        String multilang = json.get("multilang").getAsString();

        String responseDataT = json.get("dandelionResponse").getAsJsonObject().toString();
        configTokenizerMockResponse(responseDataT);

        String langTF = json.get("langTF").getAsString();
        int status = json.get("wikiStatus").getAsInt();

        JsonArray jsonArrayWRT = json.getAsJsonArray("wikiReqTitles");
        JsonArray jsonArrayWR = json.getAsJsonArray("wikiResponses");

        String [] reqTitles = new String[jsonArrayWRT.size()];
        String [] responseDataTF = new String[jsonArrayWR.size()];

        for(int i = 0; i < jsonArrayWRT.size(); i++){
            reqTitles[i] = jsonArrayWRT.get(i).getAsString();
            responseDataTF[i] = jsonArrayWR.get(i).getAsJsonObject().toString();
        }

        configTokenFilterMockResponse(langTF, status, reqTitles, responseDataTF);

        Tokenizer dandelionTokenizer = new DandelionTokenizer(auth_token,langT);
        dandelionTokenizer.setReader(new StringReader(text));
        TokenFilter tokenFilter = new DandelionTokenFilter(dandelionTokenizer,multilang);

        JsonArray jsonArrayT = json.getAsJsonArray("tokens");
        JsonArray jsonArraySO = json.getAsJsonArray("startOffsets");
        JsonArray jsonArrayEO = json.getAsJsonArray("endOffsets");
        JsonArray jsonArrayTY = json.getAsJsonArray("types");
        JsonArray jsonArrayPI = json.getAsJsonArray("positionIncrements");

        String [] tokens = new String[jsonArrayT.size()];
        int [] startOffsets = new int[jsonArraySO.size()];
        int [] endOffsets = new int[jsonArrayEO.size()];
        String [] types = new String[jsonArrayTY.size()];
        int [] positionIncrements = new int[jsonArrayPI.size()];

        for(int i = 0; i < jsonArrayT.size(); i++){
            tokens[i] = jsonArrayT.get(i).getAsString();
            startOffsets[i] = jsonArraySO.get(i).getAsInt();
            endOffsets[i] = jsonArrayEO.get(i).getAsInt();
            types[i] = jsonArrayTY.get(i).getAsString();
            positionIncrements[i] = jsonArrayPI.get(i).getAsInt();
        }

        BaseTokenStreamTestCase.assertTokenStreamContents(
            tokenFilter,
            tokens,
            startOffsets,
            endOffsets,
            types,
            positionIncrements
        );

        int numberOfTimes = reqTitles.length;
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).setRequestMethod("POST");
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).getOutputStream();
        assertEquals(params_expected, params_sent);
        verify(tokenFilterHttpUrlConnection,times(numberOfTimes)).getInputStream();
    }

}
