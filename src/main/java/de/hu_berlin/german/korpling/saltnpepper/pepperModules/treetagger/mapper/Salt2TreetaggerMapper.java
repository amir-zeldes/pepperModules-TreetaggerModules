package de.hu_berlin.german.korpling.saltnpepper.pepperModules.treetagger.mapper;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import org.eclipse.emf.common.util.EList;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Annotation;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Document;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Span;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.Token;
import de.hu_berlin.german.korpling.saltnpepper.misc.treetagger.TreetaggerFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSemantics.SALT_SEMANTIC_NAMES;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSemantics.SLemmaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSemantics.SPOSAnnotation;

/**
  * This class is for mapping Salt to Treetagger
  * @author hildebax
 */
public class Salt2TreetaggerMapper {

	//----------------------------------------------------------
	private LogService logService = null;
	
	/**
	 * Getter for LogService
	 * @return the LogService
	 */
	public LogService getLogService() {
		return logService;
	}

	/**
	 * Setter for LogService
	 * @param logService the LogService
	 */
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	private void log(int logLevel, String logText) {
		if (this.getLogService()!=null) {
			this.getLogService().log(logLevel, "<Salt2TreetaggerMapper>: " + logText);
		}
	}
	
	@SuppressWarnings("unused")
	private void logError  (String logText) { this.log(LogService.LOG_ERROR,   logText); }
	@SuppressWarnings("unused")
	private void logWarning(String logText) { this.log(LogService.LOG_WARNING, logText); }
	@SuppressWarnings("unused")
	private void logInfo   (String logText) { this.log(LogService.LOG_INFO,    logText); }
	@SuppressWarnings("unused")
	private void logDebug  (String logText) { this.log(LogService.LOG_DEBUG,   logText); }

	//---------------------------------------------------------------------------------------------
	
	private String replaceGenericSpanNamesProperty = "treetagger.output.replaceGenericSpanNames"; 

	private Properties properties = null;
	
	/**
	 * Getter for Properties 
	 * @return the Properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Setter for Properties
	 * @param properties the Properties
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	//---------------------------------------------------------------------------------------------
	
	/**
	 * This method maps a Salt document to a Treetagger document  
	 * @param sDocument the Salt document 
	 * @param tDocument the Treetagger document
     */
	public void map(SDocument sDocument, Document tDocument) {
		if (this.getProperties()==null) {
			this.setProperties(new Properties());
		}
		tDocument.setName(sDocument.getSName());
		this.addDocumentAnnotations(sDocument.getSMetaAnnotations(), tDocument);
		this.addTokens(sDocument.getSDocumentGraph(), tDocument);
	}

	/*
	 * auxiliary method
	 */
	protected void addDocumentAnnotations(EList<SMetaAnnotation> sMetaAnnotations, Document tDocument) {
		for (int i=0;i<sMetaAnnotations.size();i++) {
			SMetaAnnotation sAnno = sMetaAnnotations.get(i);
			Annotation tAnno = TreetaggerFactory.eINSTANCE.createAnnotation();
			tAnno.setName(sAnno.getSName());
			tAnno.setValue(sAnno.getSValueSTEXT());
			tDocument.getAnnotations().add(tAnno);
		}
	}
	
	/* 
	 * auxiliary method 
	 */
	protected void addTokens(SDocumentGraph sDocumentGraph, Document tDocument) {
		Hashtable<SToken,ArrayList<SSpan>> token2SpansTable = new Hashtable<SToken,ArrayList<SSpan>>();
		for (int i=0;i<sDocumentGraph.getSSpanningRelations().size();i++) {
			SToken sToken = sDocumentGraph.getSSpanningRelations().get(i).getSToken();
			SSpan  sSpan  = sDocumentGraph.getSSpanningRelations().get(i).getSSpan();
			if (!token2SpansTable.containsKey(sToken)) {
				token2SpansTable.put(sToken, new ArrayList<SSpan>());
			}
			token2SpansTable.get(sToken).add(sSpan);
		}
		
		Hashtable<SSpan,Span> sSpan2SpanTable = new Hashtable<SSpan,Span>();
		for (int i=0;i<sDocumentGraph.getSTextualRelations().size();i++) {
			STextualRelation sTexRel = sDocumentGraph.getSTextualRelations().get(i);
			Token token = TreetaggerFactory.eINSTANCE.createToken();
			token.setText(sTexRel.getSTextualDS().getSText().substring(sTexRel.getSStart(), sTexRel.getSEnd()));
			SToken sToken = sTexRel.getSToken();
			addTokenAnnotations(sToken, token);
			tDocument.getTokens().add(token);
			if (token2SpansTable.containsKey(sToken)) {
				for (int j=0;j<token2SpansTable.get(sToken).size();j++) {
					SSpan sSpan = token2SpansTable.get(sToken).get(j);
					if (!sSpan2SpanTable.containsKey(sSpan)) {
						sSpan2SpanTable.put(sSpan, this.createSpan(sSpan));
					}
					Span tSpan = sSpan2SpanTable.get(sSpan);
					token.getSpans().add(tSpan);
					tSpan.getTokens().add(token);
				}
			}
		}
	}
	
	/*
	 * auxiliary method 
	 */
	protected void addTokenAnnotations(SToken sToken, Token tToken) {
		for (int i=0;i<sToken.getSAnnotations().size();i++) {
			SAnnotation sAnno = sToken.getSAnnotations().get(i);
			Annotation tAnno = null;
			if (sAnno instanceof SPOSAnnotation) {
				tAnno = TreetaggerFactory.eINSTANCE.createPOSAnnotation();
			}
			else if (sAnno instanceof SLemmaAnnotation) {
				tAnno = TreetaggerFactory.eINSTANCE.createLemmaAnnotation();
			}
			else {
				//try to set the right type of Annotation by SALT_SEMANTICS
				SALT_SEMANTIC_NAMES currentName = SALT_SEMANTIC_NAMES.getSaltSemanticName(sAnno); 
				if (currentName==null) {
					tAnno = TreetaggerFactory.eINSTANCE.createAnyAnnotation();
				}
				else switch (currentName) {
					case POS:   tAnno = TreetaggerFactory.eINSTANCE.createPOSAnnotation();   break;
					case LEMMA: tAnno = TreetaggerFactory.eINSTANCE.createLemmaAnnotation(); break;
					default:    tAnno = TreetaggerFactory.eINSTANCE.createAnyAnnotation();  
				}
			}
			//setting the name will only affect instances of AnyAnnotation.
			//POSAnnotations get the name "pos", LemmaAnnotations get the name "lemma" 
			tAnno.setName(sAnno.getSName());
			tAnno.setValue(sAnno.getSValueSTEXT());
			tToken.getAnnotations().add(tAnno);
		}
	}

	/*
	 * auxiliary method 
	 */
	protected Span createSpan(SSpan sSpan) {
		Span retVal = TreetaggerFactory.eINSTANCE.createSpan();
		String alternativeSpanName = null;
		for (int i=0;i<sSpan.getSAnnotations().size();i++) {
			SAnnotation sAnno = sSpan.getSAnnotations().get(i);
			Annotation tAnno = TreetaggerFactory.eINSTANCE.createAnnotation();
			if (alternativeSpanName==null) {
				alternativeSpanName=sAnno.getSName(); 
			}
			tAnno.setName(sAnno.getSName());
			tAnno.setValue(sAnno.getSValueSTEXT());
			retVal.getAnnotations().add(tAnno);
		}
		retVal.setName(sSpan.getSName());
		
		if ((sSpan.getSName().startsWith("sSpan"))&&(alternativeSpanName!=null)) {
			if (this.getProperties()!=null) {
				if (this.getProperties().getProperty(replaceGenericSpanNamesProperty, "false").equalsIgnoreCase("true")) {
					retVal.setName(alternativeSpanName);
				}
			}
		}
		return retVal;
	}
	
}