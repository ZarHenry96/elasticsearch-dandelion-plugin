package org.elasticsearch.index.analysis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.elasticsearch.SpecialPermission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import static org.elasticsearch.plugin.analysis.DandelionAnalysisPlugin.ALLOWED_LANGUAGES;

public class DandelionTokenFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    private boolean multilang;

    private ArrayList<String> extraTokens = new ArrayList<>();
    private int startOffset = 0;
    private int endOffset = 0;

    private int skipped_positions = 0;

    public DandelionTokenFilter(TokenStream in, String multilang){
        super(in);
        if(!(in.hasAttribute(CharTermAttribute.class) &&
            in.hasAttribute(OffsetAttribute.class) &&
            in.hasAttribute(PositionIncrementAttribute.class) &&
            in.hasAttribute(TypeAttribute.class))) {
            throw new IllegalArgumentException("The selected tokenizer does not provide all the attributes required by the Dandelion TokenFilter!");
        }

        if(multilang == null || multilang.isEmpty() || multilang.equals("false")){
            this.multilang = false;
        } else if(multilang.equals("true")){
            this.multilang = true;
        }else{
            throw new IllegalArgumentException("Illegal multilang parameter value: only true/false are allowed!");
        }
    }

    private String[] configRequestElements(String entity) throws IOException{
        int lang_begin = 8;
        int lang_end = entity.indexOf('.', lang_begin);
        int title_begin = entity.lastIndexOf('/');

        if(lang_end == -1 || title_begin == -1){
            throw new IOException("DandelionTokenFilter multilang exception: the entity provided by the tokenizer has a wrong format!");
        }

        String lang = entity.substring(lang_begin, lang_end);
        String title = entity.substring(title_begin+1);

        if(!ALLOWED_LANGUAGES.contains(lang)){
            throw new IOException("DandelionTokenFilter multilang exception: the entity provided by the tokenizer has a wrong format in terms of language prefix (unsupported)!");
        }

        String[] res = new String[2];
        res[0] = "https://"+lang+".wikipedia.org/w/api.php";
        res[1] = "action=query&titles="+title+"&prop=langlinks&lllimit=500&llprop=url&format=json";

        return res;
    }

    private void wikiApiCall(String entity) throws IOException{
        String[] config = configRequestElements(entity);
        final String url = config[0];
        final byte[] parametersBytes = config[1].getBytes("UTF-8");

        final String response;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }
        try{
            response = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                public String run() throws IOException {
                    URL urlObj = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                    OutputStream out = connection.getOutputStream();
                    out.write(parametersBytes);
                    out.flush();
                    out.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    connection.disconnect();

                    return response.toString();
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }

        processResponse(response);
    }


    private void processResponse(String response) throws IOException{
        Gson gson = new Gson();
        JsonElement element = gson.fromJson(response, JsonElement.class);
        JsonObject jsonObject = element.getAsJsonObject();

        JsonObject pages = jsonObject.getAsJsonObject("query").getAsJsonObject("pages");

        Set<String> page_ids =  pages.keySet();
        if(page_ids.size() != 1){
            throw new IOException("Wrong number of pages returned by wikipedia langlinks api!");
        }

        Iterator<String> iterator = page_ids.iterator();
        String key = iterator.next();
        JsonObject page = pages.getAsJsonObject(key);
        if(Integer.parseInt(key) < 0 || page.getAsJsonArray("langlinks") == null){
            return;
        }

        JsonArray langlinks = page.getAsJsonArray("langlinks");

        int size = langlinks.size();
        for(int index = 0; index < size; index++){
            JsonObject lang_obj = (JsonObject) langlinks.get(index);
            if(ALLOWED_LANGUAGES.contains(lang_obj.get("lang").getAsString())){
                extraTokens.add(lang_obj.get("url").getAsString());
            }
        }

    }

    public final boolean incrementToken() throws IOException {

        if(!extraTokens.isEmpty()){
            clearAttributes();

            termAtt.setEmpty().append(extraTokens.remove(0));
            offsAtt.setOffset(startOffset, endOffset);
            typeAtt.setType(TypeAttribute.DEFAULT_TYPE);
            posIncrAtt.setPositionIncrement(0);

            return true;
        }

        while (input.incrementToken()) {
            if(typeAtt.type().startsWith("https://") && typeAtt.type().contains("wikipedia.org")){
                if(multilang) {
                    wikiApiCall(typeAtt.type());
                    startOffset = offsAtt.startOffset();
                    endOffset = offsAtt.endOffset();
                }
                termAtt.setEmpty().append(typeAtt.type());
                typeAtt.setType(TypeAttribute.DEFAULT_TYPE);
                posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement()+skipped_positions);
                skipped_positions = 0;
                return true;
            } else {
                skipped_positions+=posIncrAtt.getPositionIncrement();
            }
        }

        return false;
    }

}
