package org.exist.xquery.ner;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import org.exist.dom.BinaryDocument;
import org.exist.dom.DocumentImpl;
import org.exist.dom.QName;
import org.exist.memtree.DocumentBuilderReceiver;
import org.exist.memtree.MemTreeBuilder;
import org.exist.security.PermissionDeniedException;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.value.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class Classify extends BasicFunction {

    public final static FunctionSignature signatures[] = {
            new FunctionSignature(
                new QName("classify-string", StanfordNERModule.NAMESPACE_URI, StanfordNERModule.PREFIX),
                "Classify the provided text string. Returns a sequence of text nodes and elements for " +
                "recognized entities.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("classifier", Type.ANY_URI, Cardinality.EXACTLY_ONE,
                        "The path to the serialized classifier to load. Should point to a binary resource " +
                        "stored within the database"),
                        new FunctionParameterSequenceType("text", Type.STRING, Cardinality.EXACTLY_ONE,
                                "String of text to analyze.")
                },
                new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE,
                    "Sequence of text nodes and elements denoting recognized entities in the text")
            ),
            new FunctionSignature(
                new QName("classify-string-cn", StanfordNERModule.NAMESPACE_URI, StanfordNERModule.PREFIX),
                "Classify the provided text string. Returns a sequence of text nodes and elements for " +
                        "recognized entities.",
                new SequenceType[] {
                        new FunctionParameterSequenceType("classifier", Type.ANY_URI, Cardinality.EXACTLY_ONE,
                                "The path to the serialized classifier to load. Should point to a binary resource " +
                                        "stored within the database"),
                        new FunctionParameterSequenceType("text", Type.STRING, Cardinality.EXACTLY_ONE,
                                "String of text to analyze.")
                },
                new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE,
                        "Sequence of text nodes and elements denoting recognized entities in the text")
            ),
            new FunctionSignature(
                new QName("classify-node", StanfordNERModule.NAMESPACE_URI, StanfordNERModule.PREFIX),
                "Mark up named entities in a node and all its sub-nodes. Returns a new in-memory document. " +
                "Recognized entities are enclosed in inline elements.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("classifier", Type.ANY_URI, Cardinality.EXACTLY_ONE,
                            "The path to the serialized classifier to load. Should point to a binary resource " +
                            "stored within the database"),
                    new FunctionParameterSequenceType("node", Type.NODE, Cardinality.EXACTLY_ONE,
                        "The node to process.")
                },
                new FunctionReturnSequenceType(Type.NODE, Cardinality.EXACTLY_ONE,
                        "An in-memory node")
            ),
            new FunctionSignature(
                new QName("classify-node-cn", StanfordNERModule.NAMESPACE_URI, StanfordNERModule.PREFIX),
                "Mark up named entities in a node and all its sub-nodes. Returns a new in-memory document. " +
                "Recognized entities are enclosed in inline elements.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("classifier", Type.ANY_URI, Cardinality.EXACTLY_ONE,
                            "The path to the serialized classifier to load. Should point to a binary resource " +
                                    "stored within the database"),
                    new FunctionParameterSequenceType("node", Type.NODE, Cardinality.EXACTLY_ONE,
                            "The node to process.")
                },
                new FunctionReturnSequenceType(Type.NODE, Cardinality.EXACTLY_ONE,
                        "An in-memory node")
            )
    };

    private static String classifierSource = null;
    private static File dataDir = null;
    private static AbstractSequenceClassifier<CoreLabel> cachedClassifier = null;

    public Classify(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        String classifierPath = args[0].getStringValue();

        context.pushDocumentContext();
        try {
            if (classifierSource == null || !classifierPath.equals(classifierSource)) {
                classifierSource = classifierPath;
                DocumentImpl doc = (DocumentImpl) context.getBroker().getXMLResource(XmldbURI.createInternal(classifierPath));
                if (doc.getResourceType() != DocumentImpl.BINARY_FILE) {
                    throw new XPathException(this, "Classifier path does not point to a binary resource");
                }
                BinaryDocument binaryDocument = (BinaryDocument)doc;
                File classifierFile = context.getBroker().getBinaryFile(binaryDocument);
                dataDir = classifierFile.getParentFile();
                cachedClassifier = CRFClassifier.getClassifier(classifierFile);
            }

            ChineseSegmenter segmenter = null;
            if (isCalledAs("classify-node-cn")) {
                segmenter = ChineseSegmenter.getInstance(dataDir);
            }
            if (isCalledAs("classify-string")) {
                String text = args[1].getStringValue();
                if (segmenter != null) {
                    text = segmenter.segment(text);
                }
                return classifyString(text);
            } else {
                NodeValue nv = (NodeValue) args[1].itemAt(0);
                return classifyNode(nv, segmenter);
            }
        } catch (PermissionDeniedException e) {
            throw new XPathException(this, "Permission denied to read classifier resource", e);
        } catch (IOException e) {
            throw new XPathException(this, "Error while reading classifier resource: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new XPathException(this, "Error while reading classifier resource: " + e.getMessage(), e);
        } finally {
            context.popDocumentContext();
        }
    }

    private Sequence classifyNode(NodeValue node, ChineseSegmenter segmenter) throws XPathException {
        final Properties serializeOptions = new Properties();

        try {
            final MemTreeBuilder builder = context.getDocumentBuilder();
            final DocumentBuilderReceiver receiver = new NERDocumentReceiver(builder, segmenter);

            final int nodeNr = builder.getDocument().getLastNode();

            node.toSAX(context.getBroker(), receiver, serializeOptions);

            return builder.getDocument().getNode(nodeNr + 1);
        } catch (SAXException e) {
            throw new XPathException(this, e);
        }
    }

    private Sequence classifyString(String text) {
        MemTreeBuilder builder = context.getDocumentBuilder();
        ValueSequence result = new ValueSequence();
        classifyText(text, builder, result);
        return result;
    }

    private void classifyText(String text, MemTreeBuilder builder, ValueSequence result) {
        StringBuilder buf = new StringBuilder();
        String background = SeqClassifierFlags.DEFAULT_BACKGROUND_SYMBOL;
        String prevTag = background;
        int nodeNr = 0;
        List<List<CoreLabel>> out = cachedClassifier.classify(text);
        for (List<CoreLabel> sentence : out) {
            for (Iterator<CoreLabel> wordIter = sentence.iterator(); wordIter.hasNext(); ) {
                CoreLabel word = wordIter.next();
                final String current = word.get(CoreAnnotations.OriginalTextAnnotation.class);
                final String tag = word.get(CoreAnnotations.AnswerAnnotation.class);
                final String before = word.get(CoreAnnotations.BeforeAnnotation.class);
                final String after = word.get(CoreAnnotations.AfterAnnotation.class);
                if (!tag.equals(prevTag)) {
                    if (!prevTag.equals(background) && !tag.equals(background)) {
                        writeText(builder, buf, null);
                        builder.endElement();
                        if (result != null) {
                            result.add(builder.getDocument().getNode(nodeNr));
                        }
                        if (before != null)
                            buf.append(before);
                        writeText(builder, buf, result);
                        final String name = tag.toLowerCase();
                        nodeNr = builder.startElement("", name, name, null);
                    } else if (!prevTag.equals(background)) {
                        writeText(builder, buf, null);
                        builder.endElement();
                        if (result != null) {
                            result.add(builder.getDocument().getNode(nodeNr));
                        }
                        if (before != null)
                            buf.append(before);
                    } else if (!tag.equals(background)) {
                        if (before != null)
                            buf.append(before);
                        writeText(builder, buf, result);
                        final String name = tag.toLowerCase();
                        nodeNr = builder.startElement("", name, name, null);
                    }
                } else {
                    if (before != null)
                        buf.append(before);
                }
                buf.append(current);

                if (!tag.equals(background) && !wordIter.hasNext()) {
                    writeText(builder, buf, result);
                    builder.endElement();
                    prevTag = background;
                } else {
                    prevTag = tag;
                }
                if (after != null)
                    buf.append(after);
            }
        }
        writeText(builder, buf, result);
    }

    private void writeText(MemTreeBuilder builder, StringBuilder buf, ValueSequence result) {
        if (buf.length() > 0) {
            int node = builder.characters(buf.toString());
            if (result != null) {
                result.add(builder.getDocument().getNode(node));
            }
            buf.setLength(0);
        }
    }

    private class NERDocumentReceiver extends DocumentBuilderReceiver {

        private MemTreeBuilder builder;
        private ChineseSegmenter segmenter;

        public NERDocumentReceiver(MemTreeBuilder builder, ChineseSegmenter segmenter) {
            super(builder, true);
            this.builder = builder;
            this.segmenter = segmenter;
        }

        @Override
        public void characters(CharSequence seq) throws SAXException {
            String s = seq.toString();
            if (segmenter != null) {
                s = segmenter.segment(s);
            }
            classifyText(s, builder, null);
        }

        @Override
        public void characters(char[] ch, int start, int len) throws SAXException {
            String s = new String(ch, start, len);
            if (segmenter != null) {
                s = segmenter.segment(s);
            }
            classifyText(s, builder, null);
        }
    }
}