package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.common.settings.SecureSetting;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.plugin.analysis.DandelionAnalysisPlugin;

public class DandelionTokenizerFactory extends AbstractTokenizerFactory {

    private static String keystore_auth_token = null;

    public static void setKeystoreAuthToken(String keystore_auth_token){
        DandelionTokenizerFactory.keystore_auth_token = keystore_auth_token;
    }

    private String dandelion_auth_token;
    private String lang;

    public DandelionTokenizerFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);

        String index_auth_token = settings.get("auth");
        if(index_auth_token != null && !index_auth_token.isEmpty()) {
            dandelion_auth_token = index_auth_token;
        } else {
            dandelion_auth_token = keystore_auth_token;
        }

        lang = settings.get("lang");
    }

    @Override
    public Tokenizer create() {
        return new DandelionTokenizer(dandelion_auth_token, lang);
    }
}
