package org.elasticsearch.index.analysis;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.index.Index;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugin.analysis.DandelionAnalysisPlugin;
import org.apache.lucene.analysis.Analyzer;
import org.hamcrest.MatcherAssert;
import org.junit.Before;

import java.io.IOException;
import static org.hamcrest.Matchers.instanceOf;

public class DandelionAnalysisTests extends ESTestCase {

    private TestAnalysis analysis;

    @Before
    public void setup() throws IOException {
        Settings plugin_settings = Settings.builder().build();
        Settings index_settings  = Settings.builder().build();

        this.analysis = createTestAnalysis(new Index("test", "_na_"), index_settings, new DandelionAnalysisPlugin(plugin_settings));
    }

    public void testDandelionFactories() throws IOException {
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("dandelion-t");
        MatcherAssert.assertThat(tokenizerFactory, instanceOf(DandelionTokenizerFactory.class));

        TokenFilterFactory filterFactory = analysis.tokenFilter.get("dandelion-tf");
        MatcherAssert.assertThat(filterFactory, instanceOf(DandelionTokenFilterFactory.class));

        Analyzer analyzer = analysis.indexAnalyzers.get("dandelion-a").analyzer();
        MatcherAssert.assertThat(analyzer, instanceOf(DandelionAnalyzer.class));
    }

}
