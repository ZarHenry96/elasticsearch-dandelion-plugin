package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenizerFactory;

public class DandelionTokenizerFactory extends AbstractTokenizerFactory {

    private String auth_token;
    private String lang;

    public DandelionTokenizerFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        auth_token = settings.get("auth");
        lang = settings.get("lang");
    }

    @Override
    public Tokenizer create() {
        return new DandelionTokenizer(auth_token,lang);
    }
}
