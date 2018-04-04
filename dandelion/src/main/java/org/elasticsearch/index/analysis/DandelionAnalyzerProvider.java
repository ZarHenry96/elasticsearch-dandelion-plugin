package org.elasticsearch.index.analysis;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;

public class DandelionAnalyzerProvider extends AbstractIndexAnalyzerProvider<DandelionAnalyzer> {

    private final DandelionAnalyzer dandelionAnalyzer;

    public DandelionAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name, settings);
        dandelionAnalyzer = new DandelionAnalyzer(settings.get("auth"),settings.get("lang"),settings.get("multilang"));
    }

    @Override
    public DandelionAnalyzer get() {
        return this.dandelionAnalyzer;
    }
}
