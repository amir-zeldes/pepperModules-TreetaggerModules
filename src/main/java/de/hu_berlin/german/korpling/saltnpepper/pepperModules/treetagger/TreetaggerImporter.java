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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.resources.TabResource;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.resources.TabResourceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.FormatDefinition;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperImporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperInterfaceFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.RETURNING_MODE;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperImporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.treetagger.exceptions.TreetaggerImporterException;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.treetagger.mapper.Treetagger2SaltMapper;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * This class imports data from Treetagger format to Salt
 * @author hildebax
 *
 */
@Component(name="TreetaggerImporterComponent", factory="PepperImporterComponentFactory")
@Service(value=PepperImporter.class)
public class TreetaggerImporter extends PepperImporterImpl implements PepperImporter
{

	//---------------------------------------------------------------------------------------
	private void log(int logLevel, String logText) {
		if (this.getLogService()!=null) {
			this.getLogService().log(logLevel, "<TreetaggerImporter>: " + logText);
		}
	}

	@SuppressWarnings("unused")
	private void logError  (String logText) { this.log(LogService.LOG_ERROR,   logText); }
	private void logWarning(String logText) { this.log(LogService.LOG_WARNING, logText); }
	@SuppressWarnings("unused")
	private void logInfo   (String logText) { this.log(LogService.LOG_INFO,    logText); }
	private void logDebug  (String logText) { this.log(LogService.LOG_DEBUG,   logText); }

	//---------------------------------------------------------------------------------------
	private Properties properties = null;
	
	/**
	 * Getter for Properties
	 * @return the Properties
	 */
	public Properties getProperties() {
		return this.properties;
	}

	/**
	 * Setter for Properties
	 * @param properties the Properties
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	//---------------------------------------------------------------------------------------
	
	public TreetaggerImporter()
	{
		super();
		this.name= "TreetaggerImporter";
		//for testing the symbolic name has to be set without osgi
		if (	(this.getSymbolicName()==  null) ||
				(this.getSymbolicName().equalsIgnoreCase("")))
			this.setSymbolicName("de.hu_berlin.german.korpling.saltnpepper.pepperModules-treeTaggerModules");
		this.init();
		logDebug(this.getName()+" is created...");
	}

	protected void init()
	{
		this.supportedFormats= new BasicEList<FormatDefinition>();
		FormatDefinition formatDef= PepperInterfaceFactory.eINSTANCE.createFormatDefinition();
		formatDef.setFormatName("treetagger");
		formatDef.setFormatVersion("1.0");
		this.supportedFormats.add(formatDef);
	}
	
	private EList<FormatDefinition> supportedFormats= null;
	
	@Override
	public EList<FormatDefinition> getSupportedFormats() 
	{
		return(this.supportedFormats);
	}

	/**
	 * Stores relation between documents and their resource 
	 */
	private Map<SElementId, URI> documentResourceTable= null;
	
	@Override
	public void importCorpusStructure(SCorpusGraph corpusGraph)
			throws PepperModuleException
	{
		this.setSCorpusGraph(corpusGraph);
		if (this.getSCorpusGraph()== null)
			throw new PepperModuleException(this.name+": Cannot start with importing corpus, because salt project isn�t set.");
		
		if (this.getCorpusDefinition()== null)
			throw new PepperModuleException(this.name+": Cannot start with importing corpus, because no corpus definition to import is given.");
		if (this.getCorpusDefinition().getCorpusPath()== null)
			throw new PepperModuleException(this.name+": Cannot start with importing corpus, because the path of given corpus definition is null.");
		if (this.getCorpusDefinition().getCorpusPath().isFile())
		{
			if (	(this.getCorpusDefinition().getCorpusPath().toFileString().endsWith("/")) || 
					(this.getCorpusDefinition().getCorpusPath().toFileString().endsWith("\\")))
			{//clean uri in corpus path (if it is a folder and ends with/, / has to be removed)
				this.getCorpusDefinition().setCorpusPath(this.getCorpusDefinition().getCorpusPath().trimSegments(1));
			}//clean uri in corpus path (if it is a folder and ends with/, / has to be removed)
			
			try {
				this.documentResourceTable= this.createCorpusStructure(this.getCorpusDefinition().getCorpusPath(), null, null);
			} catch (IOException e) {
				throw new PepperModuleException(this.name+": Cannot start with importing corpus, because some exception occurs: ",e);
			}
		}	
	}
	
	/**
	 * Starts the conversion of the element corresponding to the ID 
	 */
	@Override
	public void start(SElementId sElementId) throws PepperModuleException 
	{
		if (sElementId.getSIdentifiableElement()!= null)
		{	
			if (sElementId.getSIdentifiableElement() instanceof SDocument)
			{	
				this.returningMode= RETURNING_MODE.PUT;
				URI uri= this.documentResourceTable.get(sElementId);
				if (uri== null)
					throw new TreetaggerImporterException("Cannot import document '"+sElementId+"', because no corresponding uri was found.");
				
				if (this.getSpecialParams()!=null) {
					String propertyFileName = this.getSpecialParams().toFileString();
					try {
						this.setProperties(new Properties());
						this.getProperties().load(new FileInputStream(propertyFileName));
					} catch (IOException e) {
						logWarning(String.format("couldn´t load properties file '%s'. using default values.",propertyFileName));
					}
				}

				Treetagger2SaltMapper mapper = new Treetagger2SaltMapper();
				mapper.setProperties(this.getProperties());
				mapper.setLogService(this.getLogService());

				//this importer can find multiple documents in the input file,
				//but only one SDocument can be handled
				ArrayList<Document> tDocuments = this.loadFromFile(uri);
							
				if (tDocuments.size()>1) {
					logWarning(String.format("Multiple documents found in file '%s'. At this time, TreetaggerImporter is restricted to handling one document per file.",uri.toFileString()));
				}
				//map the first tDocument to the SDocument 
				mapper.map(tDocuments.get(0), this.getSCorpusGraph().getSDocument(sElementId));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Document> loadFromFile(URI uri)
	{
		ArrayList<Document> docList = new ArrayList<Document>();
		if (uri!= null)
		{
			// create resource set and resource 
			ResourceSet resourceSet = new ResourceSetImpl();
	
			// Register XML resource factory
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("treetagger",new XMIResourceFactoryImpl());
			TabResourceFactory tabResourceFactory = new TabResourceFactory();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("tab",tabResourceFactory);
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("tt",tabResourceFactory);
			Resource resource= null;
			try {
				//load resource 
				resource = resourceSet.createResource(uri);

				if (resource== null)
					throw new TreetaggerImporterException("Cannot load The resource is null.");
				
				@SuppressWarnings("rawtypes")
				//options map for resource.load
				Map options = new HashMap();
				//put logService for TabResource loading into options
				options.put(TabResource.logServiceKey, this.getLogService());
				//put properties for TabResource loading into options
				options.put(TabResource.propertiesKey, this.getProperties());

				resource.load(options);
			} 
			catch (IOException e) 
			{	throw new TreetaggerImporterException("Cannot load resource '"+uri+"'.");	}
			catch (NullPointerException e) 
			{	throw new TreetaggerImporterException("Cannot load resource '"+uri+"'.");	}
			for (Object content : resource.getContents()) {
				docList.add((Document)content);
			}
		}
		return docList;
	}
	
	protected void activate(ComponentContext componentContext) 
	{
		this.setSymbolicName(componentContext.getBundleContext().getBundle().getSymbolicName());
	}

	protected void deactivate(ComponentContext componentContext) {
		logDebug(this.getName()+" is deactivated...");
	}
}
