/*
 * Copyright (C) 2016 IXA Taldea, University of the Basque Country UPV/EHU

   This file is part of ixa-pipe-wikify-ukb.

   ixa-pipe-wikify-ukb is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   ixa-pipe-wikify-ukb is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with ixa-pipe-wikify-ukb. If not, see <http://www.gnu.org/licenses/>.

*/


package ixa.pipe.wikify_ukb;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;
import ixa.kaflib.Term;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class CLI {

    /**
     * Get dynamically the version of ixa-pipe-wikify-ukb by looking at the MANIFEST
     * file.
     */
    private final String version = CLI.class.getPackage().getImplementationVersion();
    private final String commit = CLI.class.getPackage().getSpecificationVersion();

    private String crosslinkMappingIndexFile;


    public CLI(){
    }


    public static void main(String[] args) throws Exception {
	CLI cmdLine = new CLI();
	cmdLine.parseCLI(args);
    }


    public final void parseCLI(final String[] args) throws Exception{
    	
    	Namespace parsedArguments = null;

        // create Argument Parser
        ArgumentParser parser = ArgumentParsers.newArgumentParser(
            "ixa-pipe-wikify-ukb-" + version + ".jar").description(
            "ixa-pipe-wikify-ukb-" + version + " is a multilingual Wikification module "
                + "based on UKB and developed by IXA NLP Group.\n");

	parser
	    .addArgument("-t", "--threshold")
	    .setDefault("0.0")
	    .help("Threshold to filter out some wikifications\n");

	parser
	    .addArgument("-c", "--config")
	    .required(false)
	    .setDefault("config.properties")
	    .help("Path to the config file. Default \"config.properties\"\n");



        /*
         * Parse the command line arguments
         */

        // catch errors and print help
        try {
	    parsedArguments = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
	    parser.handleError(e);
	    System.out
		.println("Run java -jar target/ixa-pipe-wikify-ukb-" + version + ".jar -help for details");
	    System.exit(1);
        }

	Float threshold = Float.valueOf(parsedArguments.getString("threshold"));
	String config = parsedArguments.getString("config");

	    
	// Input
	BufferedReader stdInReader = null;
	// Output
	BufferedWriter w = null;

	stdInReader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
	w = new BufferedWriter(new OutputStreamWriter(System.out,"UTF-8"));
	KAFDocument kaf = KAFDocument.createFromStream(stdInReader);
	
	String lang = kaf.getLang();

	KAFDocument.LinguisticProcessor lp = kaf.addLinguisticProcessor("markables", "ixa-pipe-wikify-ukb-" + lang, version + "-" + commit);
	lp.setBeginTimestamp();

       	
	try{
	
	    InputStream configStream = new FileInputStream(config);
	    Properties prop = new Properties();
	    prop.load(configStream);


	    String ukbExec = prop.getProperty("UKBExecutable");
	    if(ukbExec == null || ukbExec.equals("")){
		throw new Exception("\"UKBExecutable\" must be specified in the configuration file");
	    }
	    else if(! new File(ukbExec).exists()) {
		throw new Exception("The following executable file specified by \"UKBExecutable\" not found: " + ukbExec);
	    }
	    String ukbKb = prop.getProperty("UKBKnowledgeBaseBin");
	    if(ukbKb == null || ukbKb.equals("")){
		throw new Exception("\"UKBKnowledgeBaseBin\" must be specified in the configuration file");
	    }
	    else if(! new File(ukbKb).exists()) {
		throw new Exception("The following binary file specified by \"UKBKnowledgeBaseBin\" not found: " + ukbKb);
	    }
	    String ukbDict = prop.getProperty("UKBDictionary");
	    if(ukbDict == null || ukbDict.equals("")){
		throw new Exception("\"UKBDictionary\" must be specified in the configuration file");
	    }
	    else if(! new File(ukbDict).exists()) {
		throw new Exception("The following dictionary specified by \"UKBDictionary\" not found: " + ukbDict);
	    }
	    String scripts = prop.getProperty("Scripts");
	    if(scripts == null || scripts.equals("")){
		throw new Exception("\"Scripts\" must be specified in the configuration file");
	    }
	    else if(! new File(scripts).exists()) {
		throw new Exception("The follwoing folder specified by \"Scripts\" not found: " + scripts);
	    }
	    String wikiDb = prop.getProperty("WikipediaDb" + lang.substring(0,1).toUpperCase() + lang.substring(1));
	    if(wikiDb == null || wikiDb.equals("")){
		throw new Exception("\"WikipediaDb" + lang.substring(0,1).toUpperCase() + lang.substring(1) + "\" must be specified in the configuration file");
	    }
	    else if(! new File(wikiDb).exists()) {
		throw new Exception("The follwoing database specified by \"WikipediaDb" + lang.substring(0,1).toUpperCase() + lang.substring(1) + "\" not found: " + wikiDb);
	    }

	    String crossWikiIndex = prop.getProperty("CrossWikipediaIndex" + lang.substring(0,1).toUpperCase() + lang.substring(1) + "En");
	    Annotate annotator = new Annotate(crossWikiIndex, lang);

	    List<WF> wordForms = kaf.getWFs();
	    List<Term> terms = kaf.getTerms();
	    if (!wordForms.isEmpty() && !terms.isEmpty()){
		annotator.wikificationToKAF(kaf, scripts, ukbExec, ukbKb, ukbDict, wikiDb, threshold);
	    }
	}
	catch (Exception e){
	    System.err.println("ixa-pipe-wikify-ukb failed: ");
	    e.printStackTrace();
	}
	finally {
	    lp.setEndTimestamp();
	    w.write(kaf.toString());
	    w.close();
	}
    } 

}
