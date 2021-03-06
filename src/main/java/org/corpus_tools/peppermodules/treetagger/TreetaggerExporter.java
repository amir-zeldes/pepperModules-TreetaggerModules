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

import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperExporterImpl;
import org.corpus_tools.pepper.modules.PepperExporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.peppermodules.treetagger.mapper.Salt2TreetaggerMapper;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

/**
 * This class exports data from Salt to Treetagger format
 * 
 * @author hildebax
 * @author Florian Zipser
 * 
 */
@Component(name = "TreetaggerExporterComponent", factory = "PepperExporterComponentFactory")
public class TreetaggerExporter extends PepperExporterImpl implements PepperExporter {
	public TreetaggerExporter() {
		super();
		// setting name of module
		setName("TreetaggerExporter");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-TreetaggerModules"));
		setDesc("This exporter transforms a Salt model into the TreeTagger format produced by the TreeTagger tool (see http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/). ");
		// set list of formats supported by this module
		addSupportedFormat("treetagger", "1.0", null);
		setProperties(new TreetaggerExporterProperties());
		setExportMode(EXPORT_MODE.DOCUMENTS_IN_FILES);
		setDocumentEnding("tt");

	}

	/**
	 * Creates a mapper of type {@link PAULA2SaltMapper}. {@inheritDoc
	 * PepperModule#createPepperMapper(Identifier)}
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		Salt2TreetaggerMapper mapper = new Salt2TreetaggerMapper();
		if (sElementId.getIdentifiableElement() instanceof SDocument)
			mapper.setResourceURI(getIdentifier2ResourceTable().get(sElementId));
		return (mapper);
	}
}
