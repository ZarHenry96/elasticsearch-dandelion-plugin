package org.elasticsearch.index.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;


public class DandelionAnalyzer extends Analyzer {

    private String auth_token;

    public DandelionAnalyzer(String auth_token) {
        super();
        this.auth_token = auth_token;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new DandelionTokenizer(auth_token);
        TokenStream result = new DandelionTokenFilter(source);
        return new TokenStreamComponents(source, result);
    }
}
