/*
 * Copyright (C) 2016 IXA Taldea, University of the Basque Country UPV/EHU

   This file is part of ixa-pipe-wikify-ukb.

   ixa-pipe-wikify-ukb is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   ixa-pipe-wikify-ukb is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with ixa-pipe-wikify-ukb.  If not, see <http://www.gnu.org/licenses/>.

*/

package ixa.pipe.wikify_ukb;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import ixa.kaflib.Term;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.Entity;
import ixa.kaflib.Mark;


public class Annotate {

    boolean cross = false;
    DictManager crosslinkMappingIndex;
    String crosslinkMappingHashName;
    String language;
    String resourceMapping;


    public Annotate(String crosslinkMappingIndexFile, String language) throws Exception{
	this.language = language;
	this.crosslinkMappingHashName = language + "En";
	if((!language.equals("en")) && (crosslinkMappingIndexFile != null) && (!crosslinkMappingIndexFile.equals("none"))){
	    if(! new File(crosslinkMappingIndexFile).exists()) {
		throw new Exception("The following database specified by \"CrossWikipediaIndex\" not found: " + crosslinkMappingIndexFile);
	    }
	    crosslinkMappingIndex = new DictManager(crosslinkMappingIndexFile, this.crosslinkMappingHashName);
	    this.cross = true;
	    this.resourceMapping = crosslinkMappingIndexFile.substring(crosslinkMappingIndexFile.lastIndexOf("/") + 1);
	}
    }
    

    public void wikificationToKAF(KAFDocument kaf, String scripts, String ukbExec, String ukbKb, String ukbDict, String wikiDb, Float threshold) throws Exception {

	String sourceMarkable = ukbExec.substring(ukbExec.lastIndexOf("/") + 1) + "_wikify";
	String resourceExternalRef = ukbKb.substring(ukbKb.lastIndexOf("/") + 1);

	String formsContext2Match = "";
	String lemmasContext2Match = "";
	String entitiesContext2Match = "";

	List<Term> terms = kaf.getTerms();
	for (Term term : terms) {
	    if(!(term.getForm().contains("@@")) && !(term.getForm().contains(" "))){
		formsContext2Match += term.getForm().toLowerCase() + "@@" + term.getWFs().get(0).getOffset() + " ";
		lemmasContext2Match += term.getLemma().toLowerCase() + "@@" + term.getWFs().get(0).getOffset() + " ";
	    }
	}

	List<Entity> entities = kaf.getEntities();
	for(Entity entity : entities) {
	    entitiesContext2Match += entity.getStr().toLowerCase().replace(" ", "_") + "@@" + entity.getTerms().get(0).getWFs().get(0).getOffset() + " ";
	}


	// create UKB context
	String ukbContext = "naf\n";
	String[] cmdMatch1 = {
	    "perl",
	    scripts + "/merge_match.pl",
	    "-d",
	    wikiDb,
	    "--t1",
	    formsContext2Match,
	    "--t2",
	    lemmasContext2Match
	};
	Process pMatch1 = Runtime.getRuntime().exec(cmdMatch1);

	String matchedContext = "";
	String matchedContext2 = "";
	String outputLineContext = "";
	BufferedReader outputContextStream = new BufferedReader(new InputStreamReader(pMatch1.getInputStream(), "UTF-8"));
	while((outputLineContext = outputContextStream.readLine()) != null){
	    matchedContext += outputLineContext + "\n";
	}
	outputContextStream.close();

	String errorContext = "";
	BufferedReader errorContextStream = new BufferedReader(new InputStreamReader(pMatch1.getErrorStream()));
	while(( errorContext = errorContextStream.readLine()) != null){
	    System.err.println("MERGE_MATCH ERROR: " + errorContext);
	}
	errorContextStream.close();

	pMatch1.waitFor();


	if(!entitiesContext2Match.equals("")){
	    String[] cmdMerge = {
		"perl",
		scripts + "/merge.pl",
		"-d",
		wikiDb,
		"--t1",
		entitiesContext2Match,
		"--t2",
		matchedContext
	    };
	    
	    Process pMerge = Runtime.getRuntime().exec(cmdMerge);
	    
	    String outputLineContext2 = "";
	    BufferedReader outputContextStream2 = new BufferedReader(new InputStreamReader(pMerge.getInputStream(), "UTF-8"));
	    while((outputLineContext2 = outputContextStream2.readLine()) != null){
		matchedContext2 += outputLineContext2 + "\n";
	    }
	    outputContextStream2.close();
	    
	    String errorContext2 = "";
	    BufferedReader errorContextStream2 = new BufferedReader(new InputStreamReader(pMerge.getErrorStream()));
	    while(( errorContext2 = errorContextStream2.readLine()) != null){
		System.err.println("MERGE_MATCH ERROR: " + errorContext2);
	    }
	    errorContextStream2.close();
	    
	    pMerge.waitFor();
	}

	if(matchedContext.equals("")){
	    return;
	}
	if(!matchedContext2.equals("")){
	    matchedContext = matchedContext2;
	}

	Map<String, String> contextSpots = new HashMap<String, String>();     
	String[] contextStrings = matchedContext.split(" ");
	for(String contextString : contextStrings){
	    contextString = contextString.trim();

	    //ContextString = spot_string@@spot_offset
	    String[] contextWordOffset = contextString.split("@@");
	    ukbContext += contextWordOffset[0] + "##" + contextWordOffset[1] + "#1 ";
	    contextSpots.put(contextWordOffset[1],contextWordOffset[0]);
	}

	
	File contextTempFile = File.createTempFile("context", ".tmp");
	contextTempFile.deleteOnExit();
	String contextTempFileName = contextTempFile.getAbsolutePath();

	BufferedWriter contextFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(contextTempFile), "UTF-8"));
	try {
	    contextFile.write(ukbContext);
	} finally {
	    contextFile.close();
	}

	
	// run UKB
	String cmdUkb = ukbExec + " --prank_damping 0.90 --prank_iter 15 --allranks --minput --nopos --ppr_w2w --dict_weight -K " + ukbKb + " -D " + ukbDict + " " + contextTempFileName;
	
	Process pUkb = Runtime.getRuntime().exec(cmdUkb);

	String outputUkb = "";
	String outputLineUkb = "";
	BufferedReader outputUkbStream = new BufferedReader(new InputStreamReader(pUkb.getInputStream(), "UTF-8"));
	while((outputLineUkb = outputUkbStream.readLine()) != null){
	    outputUkb += outputLineUkb + "\n";
	}
	outputUkbStream.close();

	String errorUkb = "";
	BufferedReader errorUkbStream = new BufferedReader(new InputStreamReader(pUkb.getErrorStream()));
	while((errorUkb = errorUkbStream.readLine()) != null){
	    System.err.println("UKB ERROR: " + errorUkb);
	}
	errorUkbStream.close();

	pUkb.waitFor();
	
	// UKB output (one line): context_id word_id (concept_id(/weight)?)+ !! lemma   (there are 2 spaces after word_id)
	// UKB output example:    naf e12  Norvegia/0.999998 Norvegiako_bandera/2.25207e-06 !! norvegia
	String ukbDisambiguations[] = outputUkb.split("\n");
	for( String ukbDisambiguation : ukbDisambiguations){
	    if(ukbDisambiguation.startsWith("!! -v")) continue;
	    String ukbLine[] = ukbDisambiguation.split(" ");
	    String offset = ukbLine[1];
	    String[] firstDisambiguation = ukbLine[3].split("/");
	    // The reference could have "/" on it
	    String reference = "";
	    for(int u=0; u<firstDisambiguation.length-1;u++){
		if(!reference.equals("")){
		    reference += "/";
		}
		reference += firstDisambiguation[u];
	    }
	    
	    // FILTERS
	    // FILTER #1: threshold

	    // get linked probability
	    String cmdProb = "perl " + scripts + "/get_linked_prob.pl -d " + wikiDb;
	    Process pProb = Runtime.getRuntime().exec(cmdProb);

	    OutputStream stdinProb = pProb.getOutputStream();
	    stdinProb.write(contextSpots.get(offset).getBytes());
	    stdinProb.flush();
	    stdinProb.close();

	    String outputProb = "";
	    BufferedReader outputProbStream = new BufferedReader(new InputStreamReader(pProb.getInputStream(), "UTF-8"));
	    outputProb = outputProbStream.readLine();
	    outputProbStream.close();

	    String errorProb = "";
	    BufferedReader errorProbStream = new BufferedReader(new InputStreamReader(pProb.getErrorStream()));
	    while(( errorProb = errorProbStream.readLine()) != null){
		System.err.println("GET_LINKED_PROB ERROR: " + errorProb);
	    }
	    errorProbStream.close();
	    
	    pProb.waitFor();

	    if(!outputProb.equals("NILL")){
		if(Float.valueOf(outputProb) < threshold){
		    contextSpots.remove(offset);
		    continue;
		}
	    }


	    // FILTER #2: UKB's confidence = 0.0
	    Float confidence = Float.parseFloat(firstDisambiguation[firstDisambiguation.length-1]);
	    /*
	    if(confidence == 0.0){
		contextSpots.remove(offset);
		continue;
	    }
	    */

	    // FILTER #3: "*_zerrenda", "list_of_*"
	    if(reference.contains("_zerrenda")) continue;

	    // END FILTERS

	    List<Term> spotTerms = getSpotTermsGivenOffset(kaf, Integer.parseInt(offset), contextSpots.get(offset));
	    contextSpots.remove(offset);
	    if(!spotTerms.isEmpty()){
		boolean noun = false;
		String markableLemma = "";
		
		if(spotTerms.get(0).getForm().contains("@")) continue;
		if(spotTerms.get(0).getForm().contains("http://")) continue;
		if(Character.isDigit(spotTerms.get(0).getForm().charAt(0))) continue;

		for (Term t : spotTerms) {
		    if(markableLemma.length() != 0){
			markableLemma += "_";
		    }
		    markableLemma += t.getLemma();
		    if((t.getPos().compareTo("N") == 0) || (t.getPos().compareTo("R") == 0)){
			noun = true;
		    }
		}
		if(noun){ // at least one term of the spot has to be a noun
		    List<WF> spotWFs = new ArrayList<WF>();
		    for(Term t : spotTerms){
			List<WF> wfs = t.getWFs();
			for(WF wf : wfs){
			    spotWFs.add(wf);
			}
		    }

		    Mark markable = kaf.newMark(kaf.newWFSpan(spotWFs), sourceMarkable);
		    markable.setLemma(markableLemma);
		    reference = "http://" + language + ".wikipedia.org/wiki/" + reference;
		    ExternalRef externalRef = kaf.newExternalRef(resourceExternalRef,reference);
		    externalRef.setConfidence(confidence);
		    externalRef.setSource(language);
		    externalRef.setReftype(language);
		    markable.addExternalRef(externalRef);
		    
		    if(cross){
			String mappingRef = getMappingRef(reference);
			if(mappingRef != null){
			    ExternalRef enRef = kaf.newExternalRef(this.resourceMapping, mappingRef);
			    enRef.setConfidence(confidence);
			    enRef.setSource(language);
			    enRef.setReftype("en");
			    externalRef.addExternalRef(enRef);
			}
		    }			
		}
	    }
	    
	}
	
	// UKB didn't assign any link to these spots. Try with MFS 
	for(String spotOffset : contextSpots.keySet()){
	    String cmdMfs = "perl " + scripts + "/mfs.pl -d " + wikiDb;
	    Process pMfs = Runtime.getRuntime().exec(cmdMfs);

	    List<Term> spotTerms = getSpotTermsGivenOffset(kaf, Integer.parseInt(spotOffset), contextSpots.get(spotOffset));
	    if(!spotTerms.isEmpty()){
		boolean noun = false;
		String spotLemma = "";
		for (Term t : spotTerms) {
		    if(spotLemma.length() != 0){
			spotLemma += "_";
		    }
		    spotLemma += t.getLemma(); 
		    if((t.getPos().compareTo("N") == 0) || (t.getPos().compareTo("R") == 0)){
			noun = true;
		    }
		}

                if(noun){ // at least one term of the spot has to be a noun        
		    OutputStream stdinMfs = pMfs.getOutputStream();
		    stdinMfs.write(spotLemma.getBytes());
		    stdinMfs.flush();
		    stdinMfs.close();

		    String outputMfs = "";
		    BufferedReader outputMfsStream = new BufferedReader(new InputStreamReader(pMfs.getInputStream(), "UTF-8"));
		    outputMfs = outputMfsStream.readLine();
		    outputMfsStream.close();

		    String errorMfs = "";
		    BufferedReader errorMfsStream = new BufferedReader(new InputStreamReader(pMfs.getErrorStream()));
		    while((errorMfs = errorMfsStream.readLine()) != null){
			System.err.println("MFS ERROR: " + errorMfs);
		    }
		    errorMfsStream.close();

		    pMfs.waitFor();
		    if(!outputMfs.equals("NILL")){
			List<WF> spotWFs = new ArrayList<WF>();
			for(Term t : spotTerms){     
			    List<WF> wfs = t.getWFs();
			    for(WF wf : wfs){
				spotWFs.add(wf);
			    }
			}                  

			String reference = outputMfs;
			String confidence = "1";
			reference = "http://" + language + ".wikipedia.org/wiki/" + reference;
			ExternalRef externalRef = kaf.newExternalRef("MFS_" + resourceExternalRef,reference);
			externalRef.setConfidence(Float.parseFloat(confidence));
			externalRef.setSource(language);
			externalRef.setReftype(language);
			Mark markable = kaf.newMark(kaf.newWFSpan(spotWFs), sourceMarkable);
			markable.setLemma(spotLemma);
			markable.addExternalRef(externalRef);     
			if(cross){
			    String mappingRef = getMappingRef(reference);
			    if(mappingRef != null){
				ExternalRef enRef = kaf.newExternalRef(this.resourceMapping, mappingRef);
				enRef.setConfidence(Float.parseFloat(confidence));
				enRef.setSource(language);
				enRef.setReftype("en");
				externalRef.addExternalRef(enRef);
			    }
			} 
		    }
		}
	    }   
	}
    }


    private List<Term> getSpotTermsGivenOffset(KAFDocument kaf, int offset, String surfaceForm){
	List<Term> spotTerms = new ArrayList<Term>();
	int sfLength = surfaceForm.length();
	List<Term> docTerms = kaf.getTerms();
	int nSurfaceFormTokens = surfaceForm.split("_").length;
	int nSpotTokens = 0;
	boolean spotMW = false;
	for (Term t : docTerms){
	    WF wf = t.getWFs().get(0);
	    if(spotMW || wf.getOffset() == offset){
		spotTerms.add(t);
		nSpotTokens++;
		spotMW = true;
		if(nSpotTokens == nSurfaceFormTokens){
		    spotMW = false;
		    break;
		}
	    }
	}
	return spotTerms;
    }


    private String getMappingRef(String ref){
	String[] info = ref.split("/");
	int pos = info.length - 1;
	String entry = info[pos];
	String url = "http://en.wikipedia.org/wiki/";
	String value = crosslinkMappingIndex.getValue(entry);
	if (value != null){
	    value = value.replace(" ","_");
	    return url + value;
	}
	return null;
    }

}
