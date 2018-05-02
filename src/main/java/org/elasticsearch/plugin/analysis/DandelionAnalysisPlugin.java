package org.elasticsearch.plugin.analysis;

import org.elasticsearch.common.settings.SecureSetting;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.*;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.apache.lucene.analysis.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static java.util.Collections.singletonMap;

public class DandelionAnalysisPlugin extends Plugin implements AnalysisPlugin {

    public static final List<String> ALLOWED_LANGUAGES = Arrays.asList("auto","de","en","es","fr","it","pt","ru","af",
        "sq","ar","bn","bg","hr","cs","da","nl","et","fi","el","gu","he","hi","hu","id","ja","kn","ko","lv","lt",
        "mk","ml","mr","ne","no","pa","fa","pl","ro","sk","sl","sw","sv","tl","ta","te","th","tr","uk","ur","vi");

    private final Setting<SecureString> ACCESS_KEY_SETTING = SecureSetting.secureString("dandelion.auth", null);

    public DandelionAnalysisPlugin(Settings settings){
        String auth_token = ACCESS_KEY_SETTING.get(settings).toString();
        if(auth_token == null || auth_token.isEmpty()){
            System.err.println(
                "\n\n-------------------------------------------------------------------------------------------------------------------------------------------\n"+
                "WARNING: no auth token specified in elasticsearch-keystore!\n"+
                "Therefore, you will have to set auth parameter in all requests (analyze,index...) to take advantage of dandelion's functionalities.\n"+
                "It's not recommended for ES instances that are not local.\n"+
                "-------------------------------------------------------------------------------------------------------------------------------------------\n\n"
            );
        }
        DandelionAnalyzerProvider.setKeystoreAuthToken(auth_token);
        DandelionTokenizerFactory.setKeystoreAuthToken(auth_token);
    }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<>();
        settings.add(ACCESS_KEY_SETTING);
        return settings;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("dandelion-t", DandelionTokenizerFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        return singletonMap("dandelion-tf", DandelionTokenFilterFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("dandelion-a", DandelionAnalyzerProvider::new);
    }
}
