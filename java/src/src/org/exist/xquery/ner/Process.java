package org.exist.xquery.ner;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
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
import java.util.List;

public class Process extends BasicFunction {

    public final static FunctionSignature signatures[] = {
            new FunctionSignature(
                new QName("process", StanfordNERModule.NAMESPACE_URI, StanfordNERModule.PREFIX),
                "Logs a message to the logger using the template given in the first parameter and " +
                        "the 'default' channel.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("classifier", Type.ANY_URI, Cardinality.EXACTLY_ONE,
                        "The path to the serialized classifier to load. Should point to a binary resource " +
                        "stored within the database"),
                        new FunctionParameterSequenceType("text", Type.STRING, Cardinality.EXACTLY_ONE,
                                "String of text to analyze.")
                },
                new FunctionReturnSequenceType(Type.ELEMENT, Cardinality.EXACTLY_ONE, "")
            )
    };
    private static final QName PERSON_QNAME = new QName("person");
    private static final QName LOCATION_QNAME = new QName("location");
    private static final QName ORGANIZATION_QNAME = new QName("organization");

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
            for (List<CoreLabel> sentence : out) {
                for (CoreLabel word : sentence) {
                    String wordText = word.word();
                    String annotation = word.get(CoreAnnotations.AnswerAnnotation.class);
                    int node;
                    if (annotation.equals("PERSON")) {
                        writeText(builder, buf, result);
                        node = builder.startElement(PERSON_QNAME, null);
                        builder.characters(wordText);
                        builder.endElement();
                        result.add(builder.getDocument().getNode(node));
                    } else if (annotation.equals("LOCATION")) {
                        writeText(builder, buf, result);
                        node = builder.startElement(LOCATION_QNAME, null);
                        builder.characters(wordText);
                        builder.endElement();
                        result.add(builder.getDocument().getNode(node));
                    } else if (annotation.equals("ORGANIZATION")) {
                        writeText(builder, buf, result);
                        node = builder.startElement(ORGANIZATION_QNAME, null);
                        builder.characters(wordText);
                        builder.endElement();
                        result.add(builder.getDocument().getNode(node));
                    } else {
                        buf.append(wordText).append(' ');
                    }
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
            result.add(builder.getDocument().getNode(node));
            buf.setLength(0);
        }
    }
}