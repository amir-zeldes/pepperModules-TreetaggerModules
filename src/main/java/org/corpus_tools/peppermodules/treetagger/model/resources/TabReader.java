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
package org.corpus_tools.peppermodules.treetagger.model.resources;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.treetagger.TreetaggerImporterProperties;
import org.corpus_tools.peppermodules.treetagger.model.AnnotatableElement;
import org.corpus_tools.peppermodules.treetagger.model.Annotation;
import org.corpus_tools.peppermodules.treetagger.model.Document;
import org.corpus_tools.peppermodules.treetagger.model.LemmaAnnotation;
import org.corpus_tools.peppermodules.treetagger.model.POSAnnotation;
import org.corpus_tools.peppermodules.treetagger.model.Span;
import org.corpus_tools.peppermodules.treetagger.model.Token;
import org.corpus_tools.peppermodules.treetagger.model.TreetaggerFactory;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource for loading and saving of treetagger data
 * 
 * @author hildebax
 * 
 */
public class TabReader {
	private static final Logger logger = LoggerFactory.getLogger(TabReader.class);
	private static final String COLUMN_SEPARATOR = "\t";
	private static final String NAME_POS = "pos";
	private static final String NAME_LEMMA = "lemma";
	private static final Character utf8BOM = new Character((char) 0xFEFF);
	private URI location = null;
	private TreetaggerImporterProperties properties = null;
	private List<Document> documents = new ArrayList<>();
	private Document currentDocument = null;
	private List<Span> openSpans = new ArrayList<>();
	private int fileLineCount = 0;
	private boolean xmlDocumentOpen = false;
	private Map<Integer, String> columnMap = null;
	private List<Integer> dataRowsWithTooMuchColumns = new ArrayList<>();
	private List<Integer> dataRowsWithTooLessColumns = new ArrayList<>();

	/**
	 * Loads a resource into treetagger model from tab separated file.
	 * 
	 * @param options
	 *            a map that may contain an instance of LogService and an
	 *            instance of Properties, with {@link #logServiceKey} and
	 *            {@link #propertiesKey} respectively as keys
	 */
	public List<Document> load(URI location, TreetaggerImporterProperties properties) {
		if (location == null) {
			throw new PepperModuleException("Cannot load any resource, because no uri is given.");
		}
		this.location = location;
		this.properties = properties;

		String metaTag = properties.getProperty(TreetaggerImporterProperties.PROP_META_TAG).getValue().toString();
		logger.info("using meta tag '{}'", metaTag);

		String fileEncoding = properties.getProperty(TreetaggerImporterProperties.PROP_FILE_ENCODING).getValue()
				.toString();
		logger.info("using input file encoding '{}'", fileEncoding);

		columnMap = properties.getColumns();

		try (BufferedReader fileReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(location.toFileString()), fileEncoding));) {
			String line = null;
			fileLineCount = 0;
			while ((line = fileReader.readLine()) != null) {
				if (line.trim().length() > 0) {
					// delete BOM if exists
					if ((fileLineCount == 0) && (line.startsWith(utf8BOM.toString()))) {
						line = line.substring(utf8BOM.toString().length());
						logger.info("BOM recognised and ignored");
					}
					fileLineCount++;
					if (XMLUtils.isProcessingInstructionTag(line)) {
						// do nothing; ignore processing instructions
					} else if (XMLUtils.isStartTag(line)) {
						String startTagName = XMLUtils.getName(line);
						if (startTagName.equalsIgnoreCase(metaTag)) {
							beginDocument(line);
						} else {
							beginSpan(startTagName, line);
						}
					} else if (XMLUtils.isEndTag(line)) {
						String endTagName = XMLUtils.getName(line);
						if (endTagName.equalsIgnoreCase(metaTag)) {
							xmlDocumentOpen = false;
							endDocument();
						} else {
							endSpan(endTagName);
						}
					} else {
						addDataRow(line);
					}
				}
			}
			endDocument();
		} catch (IOException e) {
			throw new PepperModuleException("Cannot read treetagger file '" + location + "'. ", e);
		}

		setDocumentNames();

		if (dataRowsWithTooLessColumns.size() > 0) {
			logger.warn(String.format("%s rows in input file had less data columns than expected! (Rows %s)",
					dataRowsWithTooLessColumns.size(), dataRowsWithTooLessColumns.toString()));
		}
		if (dataRowsWithTooMuchColumns.size() > 0) {
			logger.warn(String.format(
					"%s rows in input file had more data columns than expected! Additional data was ignored! (Rows %s)",
					dataRowsWithTooMuchColumns.size(), dataRowsWithTooMuchColumns.toString()));
		}
		return documents;
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void addAttributesAsAnnotations(String tag, AnnotatableElement annotatableElement) {
		List<SimpleEntry<String, String>> attributeValueList = XMLUtils.getAttributeValueList(tag);
		for (int i = 0; i < attributeValueList.size(); i++) {
			SimpleEntry<String, String> entry = attributeValueList.get(i);
			Annotation annotation = TreetaggerFactory.eINSTANCE.createAnnotation();
			annotation.setName(entry.getKey());
			annotation.setValue(entry.getValue());
			annotatableElement.getAnnotations().add(annotation);
		}
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void beginDocument(String startTag) {
		if (currentDocument != null) {
			endDocument();
		}
		currentDocument = TreetaggerFactory.eINSTANCE.createDocument();
		xmlDocumentOpen = (startTag != null);
		if (xmlDocumentOpen) {
			addAttributesAsAnnotations(startTag, currentDocument);
		}
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void endDocument() {
		if (currentDocument != null) {
			if (!openSpans.isEmpty()) {
				String openSpanNames = "";
				for (int spanIndex = 0; spanIndex < openSpans.size(); spanIndex++) {
					Span span = openSpans.get(spanIndex);
					openSpanNames += ",</" + span.getName() + ">";
					for (int tokenIndex = span.getTokens().size() - 1; tokenIndex >= 0; tokenIndex--) {
						Token token = span.getTokens().get(tokenIndex);
						if (token.getSpans().contains(span)) {
							token.getSpans().remove(span);
						} else {
							break;
						}
					}
				}
				logger.warn(String.format("input file '%s' (line %d): missing end tag(s) '%s'. tag(s) will be ignored!",
						location.lastSegment(), fileLineCount, openSpanNames.substring(1)));
			}
			if (xmlDocumentOpen) {
				logger.warn(
						String.format("input file '%s' (line %d): missing document end tag. document will be ignored!",
								location.lastSegment(), fileLineCount));
			} else {
				documents.add(currentDocument);
			}

			currentDocument = null;
			xmlDocumentOpen = false;
		}
		openSpans.clear();
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void beginSpan(String spanName, String startTag) {
		if (currentDocument == null) {
			beginDocument(null);
		}
		Span span = TreetaggerFactory.eINSTANCE.createSpan();
		openSpans.add(0, span);
		span.setName(spanName);
		addAttributesAsAnnotations(startTag, span);
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void endSpan(String spanName) {
		if (currentDocument == null) {
			logger.warn(
					String.format("input file '%s' (line '%d'): end tag '</%s>' out of nowhere. tag will be ignored!",
							location.lastSegment(), fileLineCount, spanName));
		} else {
			boolean matchingStartTagExists = false;
			for (int i = 0; i < openSpans.size(); i++) {
				Span openSpan = openSpans.get(i);
				if (openSpan.getName().equalsIgnoreCase(spanName)) {
					matchingStartTagExists = true;
					if (openSpan.getTokens().isEmpty()) {
						logger.warn(String.format(
								"input file '%s' (line %d): no tokens contained in span '<%s>'. span will be ignored!",
								location.lastSegment(), fileLineCount, openSpan.getName()));
					}
					openSpans.remove(i);
					break;
				}
			}
			if (!matchingStartTagExists) {
				logger.warn(String.format(
						"input file '%s' (line %d): no corresponding opening tag found for end tag '</%s>'. tag will be ignored!",
						location.lastSegment(), fileLineCount, spanName));
			}
		}
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void addDataRow(String row) {
		if (currentDocument == null) {
			beginDocument(null);
		}
		String[] tuple = row.split(COLUMN_SEPARATOR);
		Token token = TreetaggerFactory.eINSTANCE.createToken();
		currentDocument.getTokens().add(token);
		token.setText(tuple[0]);
		for (int i = 0; i < openSpans.size(); i++) {
			Span span = openSpans.get(i);
			token.getSpans().add(span);
			span.getTokens().add(token);
		}

		if (tuple.length > columnMap.size() + 1) {
			dataRowsWithTooMuchColumns.add(fileLineCount);
		} else if (tuple.length <= columnMap.size()) {
			dataRowsWithTooLessColumns.add(fileLineCount);
		}

		for (int index = 1; index < Math.min(columnMap.size() + 1, tuple.length); index++) {
			Annotation anno = null;
			String columnName = columnMap.get(index);
			if (columnName.equalsIgnoreCase(NAME_POS)) {
				anno = TreetaggerFactory.eINSTANCE.createPOSAnnotation();
				token.setPosAnnotation((POSAnnotation) anno);
			} else if (columnName.equalsIgnoreCase(NAME_LEMMA)) {
				anno = TreetaggerFactory.eINSTANCE.createLemmaAnnotation();
				token.setLemmaAnnotation((LemmaAnnotation) anno);
			} else {
				anno = TreetaggerFactory.eINSTANCE.createAnyAnnotation();
				anno.setName(columnName);
				token.getAnnotations().add(anno);
			}
			anno.setValue(tuple[index]);
		}
	}

	/*
	 * auxilliary method for processing input file
	 */
	private void setDocumentNames() {
		String documentBaseName = location.lastSegment().split("[.]")[0];
		int documentCount = documents.size();

		switch (documentCount) {
		case 0:
			logger.warn(String.format("no valid document data contained in file '%s'", location.toFileString()));
			break;
		case 1:
			// set simple document name
			documents.get(0).setName(documentBaseName);
			break;
		default:
			// set document names with leading zeros for number extensions
			int documentCountDigits = String.valueOf(documentCount).length();
			for (int docIndex = 0; docIndex < documentCount; docIndex++) {
				String docNumber = Integer.toString(docIndex);
				while (docNumber.length() < documentCountDigits) {
					docNumber = "0" + docNumber;
				}
				documents.get(docIndex).setName(documentBaseName + "_" + docNumber);
			}
			break;
		}
	}
}