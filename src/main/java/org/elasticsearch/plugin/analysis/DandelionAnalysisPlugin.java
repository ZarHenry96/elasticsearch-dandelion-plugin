package org.elasticsearch.plugin.analysis;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.analysis.DandelionTokenizerFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.analysis.DandelionTokenFilterFactory;
import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.DandelionAnalyzerProvider;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static java.util.Collections.singletonMap;

public class DandelionAnalysisPlugin extends Plugin implements AnalysisPlugin {

    public static final List<String> ALLOWED_LANGUAGES = Arrays.asList("auto","de","en","es","fr","it","pt","ru","af",
        "sq","ar","bn","bg","hr","cs","da","nl","et","fi","el","gu","he","hi","hu","id","ja","kn","ko","lv","lt",
        "mk","ml","mr","ne","no","pa","fa","pl","ro","sk","sl","sw","sv","tl","ta","te","th","tr","uk","ur","vi");

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
