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

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ChineseSegmenter.class);

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
            LOG.warn(e.getMessage(), e);
        }
    }

    public String segment(String input) {
        return classifier.classifyToString(input);
    }
}
