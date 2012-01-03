/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.treetagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Document;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.TreetaggerFactory;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.resources.TabResource;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.resources.TabResourceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperConvertException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.FormatDefinition;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperExporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperInterfaceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperExporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.treetagger.mapper.Salt2TreetaggerMapper;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * This class exports data from Salt to Treetagger format
 * @author hildebax
 *
 */
@Component(name="TreetaggerExporterComponent", factory="PepperExporterComponentFactory")
@Service(value=PepperExporter.class)
public class TreetaggerExporter extends PepperExporterImpl implements PepperExporter
{
	public TreetaggerExporter()
	{
		super();
		this.name= "TreetaggerExporter";
		//for testing the symbolic name has to be set without osgi
		if (	(this.getSymbolicName()==  null) ||
				(this.getSymbolicName().equalsIgnoreCase("")))
			this.setSymbolicName("de.hu_berlin.german.korpling.saltnpepper.pepperModules-treeTaggerModules");
		if (this.getLogService()!= null)
			this.getLogService().log(LogService.LOG_DEBUG,this.getName()+" is created...");
		this.init();
	}

	protected void init()
	{
		this.supportedFormats= new BasicEList<FormatDefinition>();
		FormatDefinition formatDef= PepperInterfaceFactory.eINSTANCE.createFormatDefinition();
		formatDef.setFormatName("treetagger");
		formatDef.setFormatVersion("1.0");
		this.supportedFormats.add(formatDef);
	}
	
	protected void activate(ComponentContext componentContext) 
	{
		this.setSymbolicName(componentContext.getBundleContext().getBundle().getSymbolicName());
		if (this.getLogService()!= null)
			this.getLogService().log(LogService.LOG_DEBUG,this.getName()+" is activated...");
	}

	/**
	 * Wird von der Service Component Runtime vor der Deaktivierung der Komponente
	 * aufgerufen und gibt noch eine Abschiedsbotschaft aus
	 * 
	 * @param componentContext
	 *          Der Kontext der Komponente
	 */
	protected void deactivate(ComponentContext componentContext) 
	{
		if (this.getLogService()!= null)
			this.getLogService().log(LogService.LOG_DEBUG,this.getName()+" is deactivated...");

	}
	
	private Properties props= null;

	/**
	 * Setter for Properties
	 * @param props the Properties
	 */
	public void setProps(Properties props) {
		this.props = props;
	}

	/**
	 * Getter for Properties
	 * @return the Properties
	 */
	public Properties getProps() {
		return props;
	}
	
	/**
	 * Keyword for property
	 */
	private String PROP_FILE_EXTENSION= "treetagger.fileExtension";
	/**
	 * Keyword for property
	 */
	private String PROP_EXPORT_ANNOS= "treetagger.exportAnnotations";
	
	/**
	 * Keyword for property
	 */
	private String  PROP_FLATTEN = "treetagger.output.flatten";
	
	/**
	 * Extension for export file. default= tt.
	 */
	private String fileExtension= "tt";
	
	/**
	 * List of annotation names to export. 
	 */
	private EList<String> exportAnnoNames= null;
	
	/**
	 * starts the conversion of the element corresponding to the ID   
	 */
	@Override
	public void start(SElementId sElementId) throws PepperModuleException 
	{
		if (	(sElementId!= null) &&
				(sElementId.getSIdentifiableElement()!= null) &&
				(sElementId.getSIdentifiableElement() instanceof SDocument))
		{	
			{//load props, if exists
				if (this.getSpecialParams()!= null)
				{
					this.setProps(new Properties());
					try {
						this.getProps().load(new InputStreamReader(new FileInputStream(this.getSpecialParams().toFileString())));
					} catch (FileNotFoundException e) {
						
					} catch (IOException e) {
						throw new PepperModuleException("Cannot start converting, because can not read the given file for special parameters: "+ this.getSpecialParams());
					}
				}
			}
			{//create file extension, if it is given by prop
				if (	(this.getProps()!= null) &&
						(this.getProps().getProperty(PROP_FILE_EXTENSION)!= null))
					this.fileExtension= this.getProps().getProperty(PROP_FILE_EXTENSION);
			}
			{//create list of annotations to export
				if (	(this.getProps()!= null) &&
						(this.getProps().getProperty(PROP_EXPORT_ANNOS)!= null))
				{
					this.exportAnnoNames= new BasicEList<String>();
					String[]exportAnnos= this.getProps().getProperty(PROP_EXPORT_ANNOS).split(",");
					for (String exportAnno: exportAnnos)
					{
						this.exportAnnoNames.add(exportAnno.trim());
					}
				}
			}
			//set "flatten output"
			boolean flattenOutput = false;
			if (	(this.getProps()!= null) &&
					(this.getProps().getProperty(PROP_FLATTEN)!= null))
				flattenOutput = this.getProps().getProperty(PROP_FLATTEN).equalsIgnoreCase("true");
			
			if (((SDocument)sElementId.getSIdentifiableElement()).getSDocumentGraph()!= null)
			{
				SDocument sDocument = (SDocument)sElementId.getSIdentifiableElement();
				Document tDocument = TreetaggerFactory.eINSTANCE.createDocument();

				Salt2TreetaggerMapper mapper = new Salt2TreetaggerMapper();
				mapper.setProperties(this.getProps());
				mapper.setLogService(this.getLogService());

				mapper.map(sDocument,tDocument);
				
				String corpusPath = this.getCorpusDefinition().getCorpusPath().toFileString();
				String docPath    = sElementId.getSElementPath().toString();
				String docName    = tDocument.getName()+ "." + fileExtension;

				File path = null;
				if (flattenOutput) { path = new File(corpusPath);           }
				else               { path = new File(corpusPath + docPath); }
				
				path.mkdirs();
				URI uri = URI.createFileURI(path.toString() + '/' + docName);				
				
				try {
					this.saveToFile(uri, tDocument);
				} catch (IOException e) {
					throw new PepperConvertException("Cannot write document with id: '"+sElementId.getSElementPath().lastSegment()+"' into uri: '"+uri+"'.", e);
				}
			}	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void saveToFile(URI uri, Document tDocument) throws IOException
	{
		// create resource set and resource 
		ResourceSet resourceSet = new ResourceSetImpl();

		// Register XML resource factory
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("treetagger",new XMIResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension,new TabResourceFactory());
		//load resource 
		Resource resource = resourceSet.createResource(uri);
		
		if (resource== null)
			throw new PepperConvertException("Cannot save treetagger file, the resource '"+uri+"'is null.");
		
		resource.getContents().add(tDocument);

		@SuppressWarnings("rawtypes")
		//options map for resource.load
		Map options = new HashMap();
		//put logService for TabResource loading into options
		options.put(TabResource.logServiceKey, this.getLogService());
		//put properties for TabResource loading into options
		options.put(TabResource.propertiesKey, this.getProps());

		resource.save(options);
	}
}
