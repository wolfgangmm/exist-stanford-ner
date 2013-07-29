/*
 *   exist-stanford-ner: XQuery module to integrate the stanford named entity
 *   extraction library with eXist-db.
 *   Copyright (C) 2013 Wolfgang Meier and contributors
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        new FunctionDef(Classify.signatures[3], Classify.class),
        new FunctionDef(Classify.signatures[4], Classify.class),
        new FunctionDef(Classify.signatures[5], Classify.class)
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
