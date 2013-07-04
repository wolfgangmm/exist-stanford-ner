package org.exist.xquery.ner;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import org.exist.dom.BinaryDocument;
import org.exist.dom.DocumentImpl;
import org.exist.dom.QName;
import org.exist.memtree.MemTreeBuilder;
import org.exist.security.PermissionDeniedException;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Process extends BasicFunction {

    public final static FunctionSignature signatures[] = {
            new FunctionSignature(
                new QName("process", StanfordNERModule.NAMESPACE_URI, StanfordNERModule.PREFIX),
                "Process the provided text string. Returns a sequence of text nodes and elements for " +
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
            )
    };

    private static String classifierSource = null;
    private static AbstractSequenceClassifier<CoreLabel> cachedClassifier = null;

    public Process(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        String classifierPath = args[0].getStringValue();
        String text = args[1].getStringValue();

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

                cachedClassifier = CRFClassifier.getClassifier(classifierFile);
            }
            List<List<CoreLabel>> out = cachedClassifier.classify(text);

            MemTreeBuilder builder = context.getDocumentBuilder();
            ValueSequence result = new ValueSequence();
            StringBuilder buf = new StringBuilder();
            String background = SeqClassifierFlags.DEFAULT_BACKGROUND_SYMBOL;
            String prevTag = background;
            int nodeNr = 0;
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
                            result.add(builder.getDocument().getNode(nodeNr));
                            if (before != null)
                                buf.append(before);
                            writeText(builder, buf, result);
                            final String name = tag.toLowerCase();
                            nodeNr = builder.startElement("", name, name, null);
                        } else if (!prevTag.equals(background)) {
                            writeText(builder, buf, null);
                            builder.endElement();
                            result.add(builder.getDocument().getNode(nodeNr));
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
            return result;
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

    private void writeText(MemTreeBuilder builder, StringBuilder buf, ValueSequence result) {
        if (buf.length() > 0) {
            int node = builder.characters(buf.toString());
            if (result != null) {
                result.add(builder.getDocument().getNode(node));
            }
            buf.setLength(0);
        }
    }
}