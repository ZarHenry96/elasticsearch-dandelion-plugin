package org.elasticsearch.index.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;


public class DandelionAnalyzer extends Analyzer {

    private String auth_token;
    private String lang;

    public DandelionAnalyzer(String auth_token, String lang) {
        super();
        this.auth_token = auth_token;
        this.lang = lang;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new DandelionTokenizer(auth_token,lang);
        TokenStream result = new DandelionTokenFilter(source);
        return new TokenStreamComponents(source, result);
    }
}
