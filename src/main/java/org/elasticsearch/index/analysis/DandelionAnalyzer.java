package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;


public class DandelionAnalyzer extends Analyzer {

    private String auth_token;
    private String lang;
    private String multilang;

    public DandelionAnalyzer(String auth_token, String lang, String multilang) {
        super();
        this.auth_token = auth_token;
        this.lang = lang;
        this.multilang = multilang;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new DandelionTokenizer(auth_token,lang);
        TokenStream result = new DandelionTokenFilter(source,multilang);
        return new TokenStreamComponents(source, result);
    }
}
