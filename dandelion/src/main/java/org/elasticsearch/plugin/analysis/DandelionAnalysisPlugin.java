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


import java.util.Map;
import static java.util.Collections.singletonMap;

public class DandelionAnalysisPlugin extends Plugin implements AnalysisPlugin {
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
