# ixa-pipe-wikify-ukb

This repository contains the Wikification tool based on
[UKB](http://ixa2.si.ehu.es/ukb/). *ixa-pipe-wikify-ukb* is part of
IXA pipes, a [multilingual NLP
pipeline](http://ixa2.si.ehu.es/ixa-pipes) developed by the [IXA NLP
Group](http://ixa.si.ehu.es/Ixa).

The *ixa-pipe-wikify-ukb* module takes a [NAF
document](http://wordpress.let.vupr.nl/naf/) containing *wf*, *term*
and *entity* elements as input, performs Wikification for your
language of choice, and outputs a NAF document with references to
Wikipedia on *markables* element (`<markables source="ukb_wsd_wikify">`).


## TABLE OF CONTENTS

1. [Overview of ixa-pipe-wikify-ukb](#overview)
2. [Installation](#installation)
3. [Usage of ixa-pipe-wikify-ukb](#usage)



## OVERVIEW


### Module contents

The contents of the module are the following:

    + scripts/	    	 perl scripts of the module
    + src/   	    	 java source code of the module
    + config.properties  configuration file
    + pom.xml 	    	 maven pom file wich deals with everything related to compilation and execution of the module
    + COPYING	    	 license file
    + README.md	    	 this README file
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/	         it contains binary executable and other directories




## INSTALLATION

Installing the *ixa-pipe-wikify-ukb* requires the following steps:

*If you already have installed in your machine the Java 1.7+ and MAVEN
3, please go to [step 3](#3-download-and-install-UKB-and-its-resources)
directly. Otherwise, follow the detailed steps*

### 1. Install JDK 1.7 or JDK 1.8

If you do not install JDK 1.7+ in a default location, you will probably
need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java17
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java17
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

you should now see that your JDK is 1.7 or 1.8.


### 2. Install MAVEN 3

Download MAVEN 3 from

````shell
wget http://apache.rediris.es/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/yourpath/local/apache-maven-3.0.4
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.4
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK that is using.


### 3. Download and install UKB and its resources

Download [UKB](http://ixa2.si.ehu.es/ukb/) and unpack it:

````shell
wget http://ixa2.si.ehu.es/ukb/ukb_2.2.tgz
tar xzvf ukb_2.2.tgz
````

If you are using a x86-64 Linux platform, you can use the already
compiled *ukb_wsd* binary in *bin* folder. If not, follow the
installation instructions in *src/INSTALL* file.

Download and compile a graph derived from Wikipedia.
For example, to get the Basque Wikipedia graph, first download the
following source files and unpack them:

````shell
wget http://ixa2.si.ehu.es/ukb/graphs/wikipedia_eu_2013.tar.bz2
bunzip2 wikipedia_eu_2013.tar.bz2
tar xvf wikipedia_eu_2013.tar
````

Next, compile it following the installation instructions in *src/README* file.


### 4. Download the SQLite database derived from Wikipedia

Download and unpack the required SQLite database:

   - Basque wikipedia: 

````shell
wget http://ixa2.si.ehu.es/ixa-pipes/models/2013Dec_wiki_eu.db.tgz
tar xzvf 2013Dec_wiki_eu.db.tgz
````


### 5. Get module source code

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-wikify-ukb
````

### 6. Compile

````shell
cd ixa-pipe-wikify-ukb
mvn clean package
````

This step will create a directory called 'target' which contains
various directories and files. Most importantly, there you will find
the module executable:

````shell
ixa-pipe-wikify-ukb-${version}.jar
````

This executable contains every dependency the module needs, so it is
completely portable as long as you have a JVM 1.7 installed.


## USAGE

The *ixa-pipe-wikify-ukb* requires a NAF document (with *wf*, *term*
and *entity* elements) as standard input and outputs NAF through
standard output. You can get the necessary input for *ixa-pipe-wikify-ukb*
by piping *[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok)*,
*[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos)* and
*[ixa-pipe-nerc](https://github.com/ixa-ehu/ixa-pipe-nerc)* as shown
in the example below.

First, configure the module updating the paths in the
*config.properties* configuration file.

There are several parameters:
+ **-c**: path to the configuration file.
+ **-t** (optional): use this parameter to set a threshold to filter out some wikifications (default value is 0.0).

You can call to *ixa-pipe-wikify-ukb* module as follows:

````shell
cat text.txt | ixa-pipe-tok | ixa-pipe-pos | ixa-pipe-nerc | java -jar ixa-pipe-wikify-ukb-${version}.jar -c config.properties
````

or

````shell
cat text.txt | ixa-pipe-tok | ixa-pipe-pos | ixa-pipe-nerc | java -jar ixa-pipe-wikify-ukb-${version}.jar -c config.properties -t 0.5
````

When the language is other than English, the module offers an
additional feature. It is possible to set the corresponding English
entry also. To use this option, specify the *CrossWikipediaIndex*
parameter in the *config.properties* file.

So far, you can download and untar the following package for Basque
crosslingual links:

````shell
wget http://ixa2.si.ehu.es/ixa-pipes/models/wikipedia-eu2en-db.tar.gz
tar xzvf wikipedia-eu2en-db.tar.gz
````


For more options running *ixa-pipe-wikify-ukb*:

````shell
java -jar ixa-pipe-wikify-ukb-${version}.jar -h
````


#### Contact information

    Arantxa Otegi
    arantza.otegi@ehu.es
    IXA NLP Group
    University of the Basque Country (UPV/EHU)
    E-20018 Donostia-San Sebastián


