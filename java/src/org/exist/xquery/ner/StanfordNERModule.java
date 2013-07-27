package org.exist.xquery.ner;

import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;

import java.util.List;
import java.util.Map;

/**
 * Integrates the Stanford named entity recognition system.
 *
 * @author Wolfgang
 */
public class StanfordNERModule extends AbstractInternalModule {

    public final static String NAMESPACE_URI = "http://exist-db.org/xquery/stanford-ner";
    public final static String PREFIX = "ner";

    public final static FunctionDef[] functions = {
        new FunctionDef(Classify.signatures[0], Classify.class),
        new FunctionDef(Classify.signatures[1], Classify.class),
        new FunctionDef(Classify.signatures[2], Classify.class),
        new FunctionDef(Classify.signatures[3], Classify.class)
    };

    public StanfordNERModule(Map<String, List<? extends Object>> parameters) {
        super(functions, parameters, false);
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "Named Entity Recognition module using Stanford NER";
    }

    @Override
    public String getReleaseVersion() {
        return null;
    }
}
