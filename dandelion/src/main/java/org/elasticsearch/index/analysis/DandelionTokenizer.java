package org.elasticsearch.index.analysis;

import java.io.IOException;

import org.elasticsearch.SpecialPermission;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public final class DandelionTokenizer extends Tokenizer {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    private String auth_token;

    private String inputString;
    private JsonArray annotations;
    private Integer size;
    private Integer index;
    private Integer offset;


    public DandelionTokenizer(String auth_token) {
        super();
        this.auth_token = auth_token;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        setInputString();
        dandelionApiCall();
        offset = 0;
    }

    private void setInputString() throws IOException {
        inputString = "";
        int intValueOfChar;
        while ((intValueOfChar = input.read()) != -1) {
            inputString += (char) intValueOfChar;
        }
    }

    @Override
    public boolean incrementToken() throws IOException {

        clearAttributes();

        if(index<size) {
            JsonObject entity = (JsonObject) annotations.get(index);
            String uri = entity.get("uri").getAsString();
            System.out.println(uri);
            /*
            termAtt.setEmpty().append(inputString);
            typeAtt.setType("stringa");
            offsetAtt.setOffset(offset,offset+inputString.length());
            offset += inputString.length();
            */
            index++;
            return true;
        }else{
            return false;
        }

    }

    private void dandelionApiCall() throws IOException {

        String baseUrl = "https://api.dandelion.eu/datatxt/nex/v1";
        final String url = baseUrl + "?text=" + URLEncoder.encode(inputString, "utf-8") + "&token=" + auth_token;

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }
        try{
            annotations = AccessController.doPrivileged(new PrivilegedExceptionAction<JsonArray>() {
                public JsonArray run() throws IOException {
                    URL urlObj = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                    int responseCode = connection.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Gson gson = new Gson();
                    JsonElement element = gson.fromJson(response.toString(), JsonElement.class);
                    JsonObject jsonObject = element.getAsJsonObject();
                    return jsonObject.getAsJsonArray("annotations");
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
        size = annotations.size();
        index = 0;
    }

    public void end() throws IOException {
        super.end();
        offsetAtt.setOffset(offset, offset);
    }

}
