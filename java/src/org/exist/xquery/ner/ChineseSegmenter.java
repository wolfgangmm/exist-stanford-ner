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

import edu.stanford.nlp.ie.crf.CRFClassifier;
import org.exist.xquery.XPathException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Load the word segmenter for Chinese. This is required to achieve acceptable results.
 */
public class ChineseSegmenter {

    private static ChineseSegmenter instance = null;

    public static ChineseSegmenter getInstance(File dataDir) throws XPathException {
        if (instance == null) {
            instance = new ChineseSegmenter(dataDir);
        }
        return instance;
    }

    private CRFClassifier classifier;

    public ChineseSegmenter(File dataDir) throws XPathException {
        // "ctb.gz"
        Properties props = new Properties();
        props.setProperty("NormalizationTable", new File(dataDir, "norm.simp.utf8").getAbsolutePath());
        props.setProperty("normTableEncoding", "UTF-8");
        props.setProperty("sighanCorporaDict", dataDir.getAbsolutePath());
        props.setProperty("sighanPostProcessing", "true");
        props.setProperty("serDictionary", new File(dataDir, "dict-chris6.ser.gz").getAbsolutePath());

        classifier = new CRFClassifier(props);
        try {
            classifier.loadClassifier(new File(dataDir, "ctb.gz"), props);
        } catch (IOException e) {
            throw new XPathException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new XPathException(e.getMessage());
        } catch (Exception e) {
            throw new XPathException(e.getMessage());
        }
    }

    public String segment(String input) {
        return classifier.classifyToString(input);
    }
}
