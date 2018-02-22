package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;


public class DandelionTokenFilterFactory extends AbstractTokenFilterFactory {

    public DandelionTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        //String encodername = settings.get("encoder", "metaphone");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
		return new DandelionTokenFilter(tokenStream);
    }
}
