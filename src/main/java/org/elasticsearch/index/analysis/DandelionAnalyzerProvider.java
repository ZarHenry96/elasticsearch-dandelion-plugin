package org.elasticsearch.index.analysis;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.plugin.analysis.DandelionAnalysisPlugin;

public class DandelionAnalyzerProvider extends AbstractIndexAnalyzerProvider<DandelionAnalyzer>{

    private static String keystore_auth_token = null;

    public static void setKeystoreAuthToken(String keystore_auth_token){
        DandelionAnalyzerProvider.keystore_auth_token = keystore_auth_token;
    }

    private final DandelionAnalyzer dandelionAnalyzer;

    public DandelionAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);

        String dandelion_auth_token;
        String index_auth_token = settings.get("auth");
        if(index_auth_token != null && !index_auth_token.isEmpty()) {
            dandelion_auth_token = index_auth_token;
        } else {
            dandelion_auth_token = keystore_auth_token;
        }

        dandelionAnalyzer = new DandelionAnalyzer(dandelion_auth_token,settings.get("lang"),settings.get("multilang"));
    }

    @Override
    public DandelionAnalyzer get() {
        return this.dandelionAnalyzer;
    }
}
