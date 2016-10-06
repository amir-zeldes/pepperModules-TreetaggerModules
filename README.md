![SaltNPepper project](./gh-site/img/SaltNPepper_logo2010.png)
# pepperModules-TreetaggerModules
This project provides an im- and an exporter to support the TreeTagger format in the linguistic converter framework Pepper (see https://u.hu-berlin.de/saltnpepper). The [TreeTagger](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/) is a natural language processing tool, to annotate text with part-of-speech and lemma annotations. A detailed description of the importer can be found in section [TreeTaggerImporter](#details_im) and a description for the exporter can be found [TreeTaggerExporter](#details_ex).

Pepper is a pluggable framework to convert a variety of linguistic formats (like [TigerXML](http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/TIGERSearch/doc/html/TigerXML.html), the [EXMARaLDA format](http://www.exmaralda.org/), [PAULA](http://www.sfb632.uni-potsdam.de/paula.html) etc.) into each other. Furthermore Pepper uses Salt (see https://github.com/korpling/salt), the graph-based meta model for linguistic data, which acts as an intermediate model to reduce the number of mappings to be implemented. That means converting data from a format _A_ to format _B_ consists of two steps. First the data is mapped from format _A_ to Salt and second from Salt to format _B_. This detour reduces the number of Pepper modules from _n<sup>2</sup>-n_ (in the case of a direct mapping) to _2n_ to handle a number of n formats.

![n:n mappings via SaltNPepper](./gh-site/img/puzzle.png)

In Pepper there are three different types of modules:
* importers (to map a format _A_ to a Salt model)
* manipulators (to map a Salt model to a Salt model, e.g. to add additional annotations, to rename things to merge data etc.)
* exporters (to map a Salt model to a format _B_).

For a simple Pepper workflow you need at least one importer and one exporter.

## Requirements
Since the here provided module is a plugin for Pepper, you need an instance of the Pepper framework. If you do not already have a running Pepper instance, click on the link below and download the latest stable version (not a SNAPSHOT):

> Note:
> Pepper is a Java based program, therefore you need to have at least Java 7 (JRE or JDK) on your system. You can download Java from https://www.oracle.com/java/index.html or http://openjdk.java.net/ .


## Install module
If this Pepper module is not yet contained in your Pepper distribution, you can easily install it. Just open a command line and enter one of the following program calls:

**Windows**
```
pepperStart.bat 
```

**Linux/Unix**
```
bash pepperStart.sh 
```

Then type in command *is* and the path from where to install the module:
```
pepper> update de.hu_berlin.german.korpling.saltnpepper::pepperModules-${project.artifactId}::https://korpling.german.hu-berlin.de/maven2/
```

## Usage
To use this module in your Pepper workflow, put the following lines into the workflow description file. Note the fixed order of xml elements in the workflow description file: &lt;importer/>, &lt;manipulator/>, &lt;exporter>. The TreetaggerImporter is an importer module, which can be addressed by one of the following alternatives.
A detailed description of the Pepper workflow can be found on the [Pepper project site](https://github.com/korpling/pepper). 

### a) Identify the module by name

```xml
<importer name="TreetaggerImporter" path="PATH_TO_CORPUS"/>
```
or
```xml
<exporter name="TreetaggerExporter" path="PATH_TO_CORPUS"/>
```

### b) Identify the module by formats
```xml
<importer formatName="treetagger" formatVersion="1.0" path="PATH_TO_CORPUS"/>
```
or
```xml
<exporter formatName="treetagger" formatVersion="1.0" path="PATH_TO_CORPUS"/>
```

### c) Use properties
```xml
<importer name="TreetaggerImporter" path="PATH_TO_CORPUS">
  <property key="PROPERTY_NAME">PROPERTY_VALUE</property>
</importer>
```
or
```xml
<exporter name="TreetaggerExporter" path="PATH_TO_CORPUS">
  <property key="PROPERTY_NAME">PROPERTY_VALUE</property>
</exporter>
```

## Contribute
Since this Pepper module is under a free license, please feel free to fork it from github and improve the module. If you even think that others can benefit from your improvements, don't hesitate to make a pull request, so that your changes can be merged.
If you have found any bugs, or have some feature request, please open an issue on github. If you need any help, please write an e-mail to saltnpepper@lists.hu-berlin.de .

## Funders
This project has been funded by the [department of corpus linguistics and morphology](https://www.linguistik.hu-berlin.de/institut/professuren/korpuslinguistik/) of the Humboldt-Universität zu Berlin, the Institut national de recherche en informatique et en automatique ([INRIA](www.inria.fr/en/)) and the [Sonderforschungsbereich 632](https://www.sfb632.uni-potsdam.de/en/). 

## License
  Copyright 2009 Humboldt-Universität zu Berlin, INRIA.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.


#<a name="details_im"/>TreetaggerImporter
Input Data
----------

An input file for the TreetaggerImporter is a tab separated file, having the ending 'tt', 'TreeTagger' or 'tab'. The text overlapped by the current token and its annotations are separated by one tab (columns). Each token description gets its own row. The first column is mandatory and contains the token´s form. Any further column is optional and can be declared in the properties file. Each column is required to have a distinct name. By default, i.e. when there are no column declarations in the properties file, the second and third column are considered the part-of-speech annotation and the lemma annotation respectively. Note that this default is overridden if there is any declaration of columns in the properties file.

It is possible to use SGML tags to mark spans of tokens. Invalid SGML elements (e.g. missing opening or closing tag) will be ignored.

A whole TreeTagger document may be marked by a surrounding SGML element, which is required to have a certain name. This name is definable in the properties file and defaults to "meta".

The expected input file encoding defaults to "UTF-8" and is also definable in the properties file. Any input file´s name is required to end on ".tab" or ".tt".

    <meta someDocumentAttribute="someDocumentValue">
        <someSpan someSpanAttribute="someSpanValue">
            TOKEN_1 POS_1 LEMMA_1
            TOKEN_2 POS_2 LEMMA_2
        </someSpan>
        <someMoreSpan someMoreSpanAttribute="someMoreSpanValue">
            TOKEN_3 POS_3 LEMMA_3
            TOKEN_4 POS_4 LEMMA_4
            TOKEN_5 POS_5 LEMMA_5
        </someMoreSpan>
    </meta>
                    

### Creating TreeTagger Representation

The file´s content is converted to a TreeTagger document containing a list of tokens and spans. If there is a document marking SGML element in the input file, an annotation for each of it´s attribute-value-pairs is added to the TreeTagger document. For all other SGML elements, spans are created and annotations according to the element´s attribute-value-pairs are added. For each data row, a token is created. The token´s text attribute is set to the first column´s content. For each additional column, an annotation is created and added to the token. There are three different types of annotations: The POSAnnotation, used for part-of-speech annotations, the LemmaAnnotation, used for lemma annotations, and the AnyAnnotation, used for all user-defined annotations.

If the default settings for columns are used, a POSAnnotation for the second column´s content and a LemmaAnnotation for the third column´s content are created and added to the token.

If user-defined settings for columns are used, a POSAnnotation will be created for a column named "pos", a LemmaAnnotation will be created for a column named "lemma", and an AnyAnnotation will be created for each other column.

> **Note**
>
> All names have to be distinct, and that a token can only have one POSAnnotation and one LemmaAnnotation, but any number of AnyAnnotations.

### Mapping to Salt

#### Document Annotations

When converting a TreeTagger document to Salt, a SDocument and it´s proper SDocumentGraph will be created. If there is a meta tag for the whole document, all it´s attributes will be added to the SDocument as SMetaAnnotations.

#### Tokens

Each token will be mapped to an SToken, which is added to the SDocumentGraph. If a token has a POSAnnotation, the SToken will get a corresponding SPOSAnnotation. If a token has a LemmaAnnotation, the SToken will get a corresponding SLemmaAnnotation.

All the token´s forms, separated by space characters, will be contained in the SText attribute of one STextualDS, which also is also added to the SDocumentGraph.

#### Spans

Each TreeTagger span is mapped to a SSpan, which is added to the SDocumentGraph. The SSpan´s name is set to the TreeTagger span´s name. The annotations on the TreeTagger span are mapped to SAnnotations and added to the SSpan. For each span, a SSpanningRelation between the span and all contained tokens is created. The SSpanningRelation is added to the SDocumentGraph as well.

There are two switches concerning the annotations on the TreeTagger spans. The one of them concerns spans without any annotations and will add a SAnnotation, having the span´s name as name and as value, to the SSpan. The other one will do the same, but applies to all spans, regardless of the presence of annotations.

Properties
----------

The following table contains an overview of all usable properties to customize the behavior of this pepper module. The following section contains a brief description to each single property and describes the resulting differences in the mapping to the Salt model.

<table>
<caption>properties to customize importer behavior</caption>
<col width="32%" />
<col width="29%" />
<col width="22%" />
<col width="14%" />
<thead>
<tr class="header">
<th align="left">Name of property</th>
<th align="left">Type of property</th>
<th align="left">optional/ mandatory</th>
<th align="left">default value</th>
</tr>
</thead>
<tbody>
<tr class="even">
<td align="left">treetagger.input.metaTag</td>
<td align="left">String</td>
<td align="left">optional</td>
<td align="left">--</td>
</tr>
<tr class="even">
<td align="left">treetagger.input.annotateUnannotatedSpans</td>
<td align="left">Boolean</td>
<td align="left">optional</td>
<td align="left">false</td>
</tr>
<tr class="odd">
<td align="left">treetagger.input.annotateAllSpansWithSpanName</td>
<td align="left">Boolean</td>
<td align="left">optional</td>
<td align="left">--</td>
</tr>
<tr class="even">
<td align="left">treetagger.input.separatorAfterToken</td>
<td align="left">String</td>
<td align="left">optional</td>
<td align="left">&quot; &quot;</td>
</tr>
</tbody>
</table>

### treetagger.input.metaTag

States the meta tag used to mark the TreeTagger document in the input file(s).

### treetagger.input.annotateUnannotatedSpans

If set true, this switch will cause the module to annotate all spans without attributes with their name as attribute and value, i.e. \<MyTag\> will be treated as \<MyTag mytag="mytag"\>

### treetagger.input.annotateAllSpansWithSpanName

If set true, this switch will cause the module to annotate all spans with their name as attribute and value, i.e. \<MyTag attribute="value"\> will be treated as \<MyTag attribute="value" mytag="mytag"\>

### treetagger.input.separatorAfterToken

Determines the separator which should be artificially added after a token, when mapping TreeTagger token to STextualDS in Salt. The default separator is a whitespace given by the character sequence \\" \\".

> **Note**
>
> The separator sequence, must be surrounded by double quotes. To shut of the adding of a separator, just this property value to "".


#<a name="details_ex"/>TreetaggerExporter
Mapping to TreeTagger format
----------------------------

### Document Annotations

When converting from Salt to TreeTagger, a TreeTagger document will be created, and all SMetaAnnotations of the SDocument will be mapped to Annotations of that document.

### Tokens

Each SToken will be mapped to a TreeTagger token. The token´s text comes from the STextualDS of the SDocumentGraph. If there is a SPOSAnnotation for the SToken, or if there is a SAnnotation named "pos", "part-of-speech" or "partofspeech" (all case insensitive), it will be mapped to the POSAannotation of the token. If there is a SLemmaAnnotation for the SToken, or if there is a SAnnotation named "lemma", "lemmatisation", "lemmatization" or "lemmata" (all case insensitive), it will be mapped to the LemmaAnnotation of the token. All other SAnnotations will be mapped to an "AnyAnnotation" of the TreeTagger token.

### Spans

The SSpans from all SSpanningRelations in the SDocumentGraph will be mapped to spans of the TreeTagger document. There is a switch in the properties file for the processing of SSpans with generic names ("sSpan", followed by a number). If this switch is set "true", these names will be replaced by the name of the first annotation found on the span.

### Output file

An output file from the TreetaggerExporter always contains a SGML element marking the whole document. The tag for the element defaults to "meta". This tag is definable in the properties file. All the annotations on the TreeTagger document will be added to the SGML element as attribute-valuepairs.

The output file contains one tab separated row per token. The first column contains the token´s form. If there is a POSAnnotation for the token, the second column contains it´s value, else it remains empty. If there is a LemmaAnnotation for the token, the third column contains it´s value, else it remains empty. The output of AnyAnnotations can be set in the properties file. If it is set "true", a column for each distinctly named AnyAnnotation will appears in the output file, sorted alphabetically by the AnyAnnotations´ names. Note that these names do not appear in the output file. However, the names and the order of the columns will be logged on the info-level of the conversion process.

All Spans will appear as SGML elements in the output file. The SGML element´s name is the Spans name, and all it´s annotations will be added to the element as attribute-value-pairs. In the properties file, the renaming of generically named Spans

The default encoding for output files is "UTF-8". This also is definable in the properties file. All output files´ names end on ".tt".

Properties
----------

The following table contains an overview of all usable properties to customize the behavior of this pepper module. The following section contains a brief description to each single property and describes the resulting differences in the mapping to the salt model.

<table>
<caption>properties to customize exporter behavior</caption>
<col width="32%" />
<col width="29%" />
<col width="22%" />
<col width="14%" />
<thead>
<tr class="header">
<th align="left">Name of property</th>
<th align="left">Type of property</th>
<th align="left">optional/ mandatory</th>
<th align="left">default value</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left">treetagger.fileExtension</td>
<td align="left">String</td>
<td align="left">optional</td>
<td align="left">utf-8</td>
</tr>
<tr class="even">
<td align="left">treetagger.output.metaTag</td>
<td align="left">String</td>
<td align="left">optional</td>
<td align="left">--</td>
</tr>
<tr class="odd">
<td align="left">treetagger.output.exportAnyAnnotation</td>
<td align="left">Boolean</td>
<td align="left">optional</td>
<td align="left">false</td>
</tr>
<tr class="even">
<td align="left">treetagger.output.replaceGenericSpanNames</td>
<td align="left">Boolean</td>
<td align="left">optional</td>
<td align="left">false</td>
</tr>
<tr class="odd">
<td align="left">treetagger.output.flatten</td>
<td align="left">Boolean</td>
<td align="left">optional</td>
<td align="left">false</td>
</tr>
</tbody>
</table>

### treetagger.fileExtension

This property determines the ending of TreeTagger files, which are exported. The default value is '.tt'.

### treetagger.output.metaTag

Sets the meta tag used to mark the TreeTagger document in the output file(s).

### treetagger.output.exportAnyAnnotation

If set true, each AnyAnnotation of tokens will appear in the output file.

### treetagger.output.replaceGenericSpanNames

If set true, generic span names like sSpan123 will be replaced with the first annotation of the span found. If the span has no annotations, the generic name will not be replaced.

### treetagger.output.flatten

If set true, the output directory structure is flat: all documents are put in the output root directory.
