exist-stanford-ner
==================

Integrate the Stanford Named Entity Recognizer into eXist-db.

Demo and documentation are included in the package.

## Compile and install

1. clone the github repository: https://github.com/wolfgangmm/exist-stanford-ner
2. edit build.properties and set exist.dir to point to your eXist install directory
3. call "ant" in the directory to create a .xar
4. upload the xar into eXist using the dashboard

## Functions

There are only three functions:

ner:classify-string($classifier as xs:anyURI, $text as xs:string) - processes a single string of text and returns a sequence of text nodes and elements (person, location, organization)

ner:classify-node($classifier as xs:anyURI, $node as node()) as node() - returns an in-memory copy of $node with all named entities wrapped into inline elements.

ner:classify-node($classifier as xs:anyURI, $node as node(), $callback as function(xs:string, xs:string) as item()*) as node() - returns an in-memory copy of $node. Calls the callback function for every entity found and replaces it with the return value of the function.

For Chinese text use the variants: ner:classify-string-cn and ner:classify-node-cn.

Extended documentation can be found after installing the package.

## Usage example

```xquery
xquery version "3.0";

import module namespace ner="http://exist-db.org/xquery/stanford-ner";

let $classifier := xs:anyURI("/db/apps/stanford-ner/resources/classifiers/english.all.3class.distsim.crf.ser.gz")
let $text := <p>The fate of Lehman Brothers, the beleaguered investment bank,
   hung in the balance on Sunday as Federal Reserve officials and the leaders
   of major financial institutions continued to gather in emergency meetings
   trying to complete a plan to rescue the stricken bank.  Several possible
   plans emerged from the talks, held at the Federal Reserve Bank of New York
   and led by Timothy R. Geithner, the president of the New York Fed, and
   Treasury Secretary Henry M. Paulson Jr.</p>
return
 ner:classify-node($classifier, $text)
```

## Support for Chinese

To recognize entities in Chinese texts, you need to obtain the Chinese classifier and segmenter. Before you build the .xar to install, download the classifier and word segmenter using the following links:

* http://nlp.stanford.edu/software/stanford-ner-2012-11-11-chinese.zip
* http://nlp.stanford.edu/software/stanford-segmenter-2013-06-20.zip

From the first package, copy chinese.misc.distsim.crf.ser.gz into resources/classifiers. From the second zip, copy

* data/dict-chris6.ser.gz
* data/norm.simp.utf8
* data/ctb.gz

and everything inside data/dict into the resources/classifiers directory.