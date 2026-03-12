package com.trivine.llc.api.config;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Properties;

@Configuration
public class NlpPipelineConfig {
    @Bean @Lazy
    public StanfordCoreNLP stanfordCoreNLP() {
        Properties p = new Properties();
        p.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        p.setProperty("ner.useSUTime", "false");
        p.setProperty("ner.applyNumericClassifiers", "false");
        return new StanfordCoreNLP(p);
    }
}
