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
package org.corpus_tools.peppermodules.treetagger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.pepper.modules.exceptions.PepperModulePropertyException;
import org.corpus_tools.salt.common.SToken;

public class TreetaggerImporterProperties extends PepperModuleProperties {
	private static final long serialVersionUID = -7528434389946019271L;

	public static final String PREFIX = "treetagger.input.";

	public static final String PROP_ANNOTATE_UNANNOTATED_SPANS = PREFIX + "annotateUnannotatedSpans";

	public static final String PROP_ANNOTATE_ALL_SPANS_WITH_NAME = PREFIX + "annotateAllSpansWithSpanName";
	/**
	 * States the meta tag used to mark the TreeTagger document in the input
	 * file(s).
	 **/
	public static final String PROP_META_TAG = PREFIX + "metaTag";
	/**
	 * property key for the encoding of input file
	 */
	public static final String PROP_FILE_ENCODING = "treetagger.input.fileEncoding";
	/**
	 * Name of property to determine the separator which should be artificially
	 * added after a token, when mapping treetagger token to STextualDS in Salt.
	 * The default separator is a whitespace given by the character sequence "
	 * ". Note, the separator sequence, must be surrounded by double quotes. To
	 * shut of the adding of a separator, just this property value to "".
	 **/
	public static final String PROP_SEPARATOR_AFTER_TOKEN = PREFIX + "separatorAfterToken";

	private static final Pattern PATTERN_PROP_INPUT_COLUMNS = Pattern.compile("treetagger\\.input\\.column");
	private static final String DEFAULT_POS_NAME = "pos";
	private static final String DEFAULT_LEMMA_NAME = "lemma";

	public TreetaggerImporterProperties() {
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_ANNOTATE_UNANNOTATED_SPANS, Boolean.class,
				"If set true, this switch will cause the module to annotate all spans without attributes with their name as attribute and value.",
				false, false));
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_ANNOTATE_ALL_SPANS_WITH_NAME, Boolean.class,
				"If set true, this switch will cause the module to annotate all spans with their name as attribute and value.",
				false, false));
		this.addProperty(new PepperModuleProperty<String>(PROP_META_TAG, String.class,
				"States the meta tag used to mark the TreeTagger document in the input file(s).", "meta", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_SEPARATOR_AFTER_TOKEN, String.class,
				"Determines the separator which should be artificially added after a token, when mapping treetagger token to STextualDS in Salt. The default separator is a whitespace given by the character sequence \" \". Note, the separator sequence, must be surrunded by double quotes. To shut of the adding of a separator, just this property value to \"\"",
				" ", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_FILE_ENCODING, String.class,
				"Determines the encoding of the input files. ", "UTF-8", false));
	}

	public Boolean getAnnotateUnannotatedSpans() {
		return ((Boolean) this.getProperty(PROP_ANNOTATE_UNANNOTATED_SPANS).getValue());
	}

	public Boolean getAnnotateAllSpansWithName() {
		return ((Boolean) this.getProperty(PROP_ANNOTATE_ALL_SPANS_WITH_NAME).getValue());
	}

	/**
	 * Returns the separator to be used to separate the text covered by
	 * {@link SToken}.
	 * 
	 * @return
	 */
	public String getSeparatorAfterToken() {
		String separator = (String) this.getProperty(PROP_SEPARATOR_AFTER_TOKEN).getValue();
		separator.replace("\"", "");
		return (separator);
	}

	/**
	 * validates and return the input columns definition from the properties
	 * file
	 */
	public Map<Integer, String> getColumns() {
		Map<Integer, String> retVal = new HashMap<>();
		for (Map.Entry<Object, Object> property : getProperties().entrySet()) {
			String key = property.getKey().toString();
			if (PATTERN_PROP_INPUT_COLUMNS.matcher(key).find()) {

				// try to extract the number at the end of the key
				String indexStr = key.substring("treetagger.input.column".length());
				String name = property.getValue().toString();
				Integer index = null;

				try {
					index = Integer.valueOf(indexStr);
				} catch (NumberFormatException e) {
					throw new PepperModulePropertyException(
							"Invalid property name '" + key + "': " + indexStr + " is not a valid number!", e);
				}

				// minimal index is 1
				if (index <= 0) {
					throw new PepperModulePropertyException(
							"Invalid settings in properties file: no column index less than 1 allowed!");
				}

				// with the standard Properties class, this can never happen...
				if (retVal.containsKey(index)) {
					throw new PepperModulePropertyException(
							"Invalid settings in properties file:  More than one column is defined for index '" + index
									+ "'");
				}

				if (retVal.containsValue(name)) {
					throw new PepperModulePropertyException(
							"Invalid settings in properties file:  More than one column is defined for name '" + name
									+ "'");
				}

				retVal.put(index, name);
			}
		}

		// return defaults if nothing is set in the properties file
		if (retVal.size() == 0) {
			retVal.put(1, DEFAULT_POS_NAME);
			retVal.put(2, DEFAULT_LEMMA_NAME);
			return retVal;
		}

		// check consecutivity of indexes
		for (int expectedColumnNo = 1; expectedColumnNo <= retVal.size(); expectedColumnNo++) {
			if (!retVal.containsKey(expectedColumnNo)) {
				throw new PepperModulePropertyException(
						"Invalid settings in properties file: column indexes are not consecutive, column" + expectedColumnNo
								+ " missing!");
			}
		}
		return retVal;
	}
}
