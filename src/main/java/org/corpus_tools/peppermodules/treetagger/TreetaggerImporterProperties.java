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

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.salt.common.SToken;

public class TreetaggerImporterProperties extends PepperModuleProperties {
	public static final String PREFIX = "treetagger.input.";

	public static final String PROP_ANNOTATE_UNANNOTATED_SPANS = PREFIX + "annotateUnannotatedSpans";

	public static final String PROP_ANNOTATE_ALL_SPANS_WITH_NAME = PREFIX + "annotateAllSpansWithSpanName";
	/**
	 * States the meta tag used to mark the TreeTagger document in the input
	 * file(s).
	 **/
	public static final String PROP_META_TAG = PREFIX + "metaTag";
	/**
	 * Name of property to determine the separator which should be artificially
	 * added after a token, when mapping treetagger token to STextualDS in Salt.
	 * The default separator is a whitespace given by the character sequence
	 * " ". Note, the separator sequence, must be surrounded by double quotes.
	 * To shut of the adding of a separator, just this property value to "".
	 **/
	public static final String PROP_SEPARATOR_AFTER_TOKEN = PREFIX + "separatorAfterToken";

	public TreetaggerImporterProperties() {
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_ANNOTATE_UNANNOTATED_SPANS, Boolean.class, "If set true, this switch will cause the module to annotate all spans without attributes with their name as attribute and value.", false, false));
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_ANNOTATE_ALL_SPANS_WITH_NAME, Boolean.class, "If set true, this switch will cause the module to annotate all spans with their name as attribute and value.", false, false));
		this.addProperty(new PepperModuleProperty<String>(PROP_META_TAG, String.class, "States the meta tag used to mark the TreeTagger document in the input file(s).", "meta", false));
		this.addProperty(new PepperModuleProperty<String>(PROP_SEPARATOR_AFTER_TOKEN, String.class, "Determines the separator which should be artificially added after a token, when mapping treetagger token to STextualDS in Salt. The default separator is a whitespace given by the character sequence \" \". Note, the separator sequence, must be surrunded by double quotes. To shut of the adding of a separator, just this property value to \"\"", " ", false));
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

}
