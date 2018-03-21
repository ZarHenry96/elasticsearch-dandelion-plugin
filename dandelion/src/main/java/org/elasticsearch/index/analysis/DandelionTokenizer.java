package org.elasticsearch.index.analysis;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

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

    private final List<String> allowedLanguages = Arrays.asList("auto","de","en","es","fr","it","pt","ru","af",
        "sq","ar","bn","bg","hr","cs","da","nl","et","fi","el","gu","he","hi","hu","id","ja","kn","ko","lv","lt",
        "mk","ml","mr","ne","no","pa","fa","pl","ro","sk","sl","sw","sv","tl","ta","te","th","tr","uk","ur","vi");

    private String auth_token;
    private String lang;

    private String inputString;
    private JsonArray annotations;
    private int size;
    private int index;
    private int offset;


    public DandelionTokenizer(String auth_token, String lang) {
        super();
        if(auth_token == null || auth_token.isEmpty()){
            throw new IllegalArgumentException("No authorization token (auth field) specified!");
        }else {
            this.auth_token = auth_token;
        }
        if(lang == null || lang.isEmpty()){
            this.lang = "auto";
        }else if (allowedLanguages.contains(lang)){
            this.lang = lang;
        }else {
            throw new IllegalArgumentException("Illegal language (lang) parameter! Check on dandelion.eu the possible values; if not specified auto will be used.");
        }
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
            int begin = entity.get("start").getAsInt();
            if (begin == offset) {
                int end = entity.get("end").getAsInt();
                String uri = entity.get("uri").getAsString();
                termAtt.setEmpty().append(inputString.substring(begin, end));
                offsetAtt.setOffset(begin, end);
                offset = end;
                typeAtt.setType(uri);
                index++;
                return true;
            } else if (begin > offset) {
                termAtt.setEmpty().append(inputString.substring(offset, begin));
                offsetAtt.setOffset(offset, begin);
                offset = begin;
                typeAtt.setType("");
                return true;
            } else {
                throw new IOException("Error in entity offsets management!");
            }
        } else if (offset != inputString.length()){
            int end = inputString.length();
            termAtt.setEmpty().append(inputString.substring(offset, end));
            offsetAtt.setOffset(offset, end);
            offset = end;
            typeAtt.setType("");
            return true;
        } else {
            return false;
        }
    }

    private void dandelionApiCall() throws IOException {

        String baseUrl = "https://api.dandelion.eu/datatxt/nex/v1";
        final String url = baseUrl + "?text=" + URLEncoder.encode(inputString, "utf-8") + "&token=" + auth_token + "&lang=" + lang;

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

                    BufferedReader in;
                    if (responseCode == HttpURLConnection.HTTP_OK){
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    }else{
                        in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    }

                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Gson gson = new Gson();
                    JsonElement element = gson.fromJson(response.toString(), JsonElement.class);
                    JsonObject jsonObject = element.getAsJsonObject();

                    switch (responseCode) {
                        case HttpURLConnection.HTTP_OK:
                            return jsonObject.getAsJsonArray("annotations");
                        case HttpURLConnection.HTTP_UNAUTHORIZED:
                        case HttpURLConnection.HTTP_FORBIDDEN:
                            String exMessage = jsonObject.get("message").getAsString() + " , if you have any problem please contact us at sales@spaziodati.eu";
                            throw new IOException(exMessage);
                        default:
                            String message = jsonObject.get("message").getAsString();
                            throw new IOException(message);
                    }
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
