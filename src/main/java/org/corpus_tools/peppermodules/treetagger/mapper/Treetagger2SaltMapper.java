/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.corpus_tools.peppermodules.treetagger.mapper;

import java.util.Hashtable;
import java.util.List;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.peppermodules.treetagger.TreetaggerImporterProperties;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;

import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Annotation;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Document;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.LemmaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.POSAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Span;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Token;

/**
 * This class is for mapping Treetagger to Salt
 * 
 * @author hildebax
 */
public class Treetagger2SaltMapper extends PepperMapperImpl implements PepperMapper {
	/**
	 * Returns the specific {@link TreetaggerImporterProperties} object.
	 * 
	 * @return the Properties
	 */
	public TreetaggerImporterProperties getProps() {
		return (TreetaggerImporterProperties) this.getProperties();
	}

	private Document ttDocument = null;

	public Document getTtDocument() {
		return ttDocument;
	}

	public void setTTDocument(Document ttDocument) {
		this.ttDocument = ttDocument;
	}

	/**
	 * The method maps a Treetagger document to a Salt document
	 * 
	 * @param tDocument
	 *            the Treetagger document
	 * @param sDocument
	 *            the Salt document
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {

		if (getDocument().getDocumentGraph() == null) {
			getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());
		}
		getDocument().getDocumentGraph().setName(getTtDocument().getName() + "_graph");
		getDocument().setName(getTtDocument().getName());
		this.addMetaAnnotation(getTtDocument().getAnnotations(), getDocument());
		this.createSTextualDS(getTtDocument().getTokens(), getDocument());
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/*
	 * auxiliary method
	 */
	protected void addMetaAnnotation(List<Annotation> tAnnotations, SDocument sDocument) {
		for (Annotation tAnno : tAnnotations) {
			sDocument.createMetaAnnotation(null, tAnno.getName(), tAnno.getValue());
		}
	}

	/*
	 * auxiliary method
	 */
	protected STextualDS createSTextualDS(List<Token> tTokens, SDocument sDocument) {
		boolean annotateUnannotatedSpans = this.getProps().getAnnotateUnannotatedSpans();

		boolean annotateAllSpansWithSpanName = this.getProps().getAnnotateAllSpansWithName();

		// creating and adding STextualDS
		STextualDS sText = SaltFactory.createSTextualDS();
		sDocument.getDocumentGraph().addNode(sText);

		Hashtable<Span, SSpan> spanTable = new Hashtable<Span, SSpan>();

		String text = null;
		int start = 0;
		int end = 0;
		// for (Token tToken: tTokens) {
		for (int tokenIndex = 0; tokenIndex < tTokens.size(); tokenIndex++) {
			Token tToken = tTokens.get(tokenIndex);
			if (text == null) {
				start = 0;
				end = tToken.getText().length();
				text = tToken.getText();
			} else {
				start = text.length() + this.getProps().getSeparatorAfterToken().length();
				end = start + tToken.getText().length();
				text = text + this.getProps().getSeparatorAfterToken() + tToken.getText();
			}

			// creating and adding token
			SToken sToken = this.createSToken(tToken);
			sDocument.getDocumentGraph().addNode(sToken);

			// creating and adding spans and spanning relations
			for (int i = 0; i < tToken.getSpans().size(); i++) {
				Span tSpan = tToken.getSpans().get(i);
				SSpan sSpan = null;
				if (!spanTable.containsKey(tSpan)) {
					sSpan = SaltFactory.createSSpan();
					spanTable.put(tSpan, sSpan);
					sSpan.setGraph(sDocument.getDocumentGraph());
					sSpan.setName(tSpan.getName());
					List<Annotation> tAnnotations = tSpan.getAnnotations();
					if ((annotateAllSpansWithSpanName) || ((tAnnotations.size() == 0) && (annotateUnannotatedSpans))) {
						sSpan.createAnnotation(null, tSpan.getName().toLowerCase(), tSpan.getName().toLowerCase());
					}
					for (int j = 0; j < tAnnotations.size(); j++) {
						SAnnotation anno = this.createAnnotation(tSpan.getAnnotations().get(j));
						sSpan.addAnnotation(anno);
					}
				} else {
					sSpan = spanTable.get(tSpan);
				}
				SSpanningRelation sSpanningRelation = SaltFactory.createSSpanningRelation();
				sSpanningRelation.setSource(sSpan);
				sSpanningRelation.setTarget(sToken);
				sSpanningRelation.setGraph(sDocument.getDocumentGraph());
			}

			STextualRelation sTextRel = this.createSTextualRelation(sToken, sText, start, end);
			sDocument.getDocumentGraph().addRelation(sTextRel);
		}
		sText.setText(text);
		return (sText);
	}

	/*
	 * auxiliary method
	 */
	protected SToken createSToken(Token tToken) {
		SToken retVal = SaltFactory.createSToken();
		for (Annotation tAnnotation : tToken.getAnnotations()) {
			retVal.addAnnotation(this.createAnnotation(tAnnotation));
		}
		return (retVal);
	}

	/*
	 * auxiliary method
	 */
	protected SAnnotation createAnnotation(Annotation tAnnotation) {
		SAnnotation retVal = null;
		if (tAnnotation instanceof POSAnnotation)
			retVal = SaltFactory.createSPOSAnnotation();
		else if (tAnnotation instanceof LemmaAnnotation)
			retVal = SaltFactory.createSLemmaAnnotation();
		else {
			retVal = SaltFactory.createSAnnotation();
			retVal.setName(tAnnotation.getName());
		}
		retVal.setValue(tAnnotation.getValue());
		return (retVal);
	}

	/*
	 * auxiliary method
	 */
	protected STextualRelation createSTextualRelation(SToken sToken, STextualDS sText, int start, int end) {
		STextualRelation retVal = null;
		retVal = SaltFactory.createSTextualRelation();
		retVal.setTarget(sText);
		retVal.setSource(sToken);
		retVal.setStart(start);
		retVal.setEnd(end);
		return (retVal);
	}

}
