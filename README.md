# nax - a command-line tool for processing NAACCR XML Documents

## Installation
#### Windows
1. [Download latest release for Windows](../../releases)
2. Unzip to a directory such as Documents
3. In File Explorer, open the unzipped `naaccr-commandline` directory and double click on the `Launch_nax` batch file.
4. In the command window that opens, type "nax" and hit Enter to see Help.

#### macOS
1. [Download latest release for macOS](../../releases)
2. Unzip to a directory such as Documents
3. Open terminal and change directory to the unzipped `naaccr-commandline` directory. Run the `Install_on_macOS.sh` script.
4. Open a new Terminal window and type "nax" to see Help.

#### Other OS
1. Download and install a Java Runtime version 11 or later, such as [AdoptOpenJDK 11](https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot)
2. [Download the latest jar file release](../../releases)
3. Open a terminal window and run the jar file:
    
    `java -jar naaccr-commandline-<version>.jar`

## Support, Contributions, and Feedback
For support, feature requests, feedback, or to make contributions, please [open an issue](../../issues/new).

# General Usage
This section shows you some common usage scenarios but does not go over all possible command-line options. 

## See all command-line options:
    
`nax -h`
## Getting information about data in a NAACCR XML File
* #### Get basic information about a file including counts of Patient and Tumor elements, the Base Dictionary, and any User Dictionaries
  
     
    `nax <Input NAACCR XML File>`

Returns:
```
    ...
        "naaccrDataAttributes" : {
          "baseDictionaryUri" : "http://naaccr.org/naaccrxml/naaccr-dictionary-180.xml",
          "recordType" : "I",
          "specificationVersion" : "1.4",
          "timeGenerated" : "2019-04-12T11:27:31.2025313-04:00",
          "userDictionaryUri" : "http://datasubmission.org/user-dictionary-180.xml https://www.kcr.uky.edu/xml/kcr-user-dictionary-180.xml"
        },
        "elementCounts" : {
          "Item" : 803,
          "NaaccrData" : 1,
          "Patient" : 9,
          "Tumor" : 10
        },
        "patientCountsPerTumorCount" : {
          "1 Tumor" : 8,
          "2 Tumors" : 1
        }
    ...
```
*  #### Get more detailed counts of elements in a file, such as all naaccrIds along with Item, Patient, and Tumor elements.


`nax <Input NAACCR XML File> -met 2`

Returns:

        ...
             "naaccrIdCounts" : {
              "abstractedBy" : 958,
              "accessionNumberHosp" : 961,
              "addrAtDxCity" : 961,
              "addrAtDxCountry" : 961,
              "addrAtDxNoStreet" : 961,
              "addrAtDxPostalCode" : 961,
              "addrAtDxState" : 961,
              "grade" : 2272,
              "nameFirst" : 2288,
              "nameLast" : 2288,
        ...
* #### Get detailed value counts for naaccrIds in a file, optionally creating custom bins of the data with Groovy code
Some naaccrIds will contain categorical data suitable for value counts such as `behaviorCodeIcdO3`, `sex`, or `race1`, and some naaccrIds will contain continuous data 
 such as dates that will need custom data binning. For the simplest categorical data, specify the naaccrIds in a comma-separated list with the `-vc` argument:
 
`nax <Input NAACCR XML File> -vc race1,sex`

Returns:

    ...
          "race1" : {
            "01" : 883,
            "02" : 51,
            "15" : 1,
            "16" : 2,
            "96" : 2,
            "98" : 1
          },
          "sex" : {
            "1" : 485,
            "2" : 455
          },
    ...
And for continuous data, specify a single naaccrId followed by a Groovy script that will bin the data:

`nax <Input NAACCR XML File> -vc dateOfDiagnosis="left(dateOfDiagnosis,4)"`

Returns:

    ...
          "dateOfDiagnosis" : {
            "2015" : 291,
            "2016" : 274,
            "2017" : 304,
            "2018" : 88,
            "2019" : 4
          },
    ...
## Changing data in a NAACCR XML File
NOTE: The nax software will never make changes to an existing XML file, instead, it can create a new output file by using the command-line argument ```-o``` or ```--outputfile```. 
If you want to do a dry run of some commands without creating an output file, omit the output file argument.

* #### Remove all specified naaccrIds from an input file


`nax <Input NAACCR XML File> -e nameFirst,nameLast,socialSecurityNumber -o <Output NAACCR XML File>`

* #### Remove all naaccrIds from an input file except for a list of included naaccrIds


`nax <Input NAACCR XML File> -i patientIdNumber,dateOfDiagnosis,primarySite,histologicTypeIcdO3 -o <Output NAACCR XML File>`

* #### Set a constant value for one or more naaccrIds in an input file


`nax <Input NAACCR XML File> -con reportingFacility=0000099999,abstractedBy=TB -o <Output NAACCR XML File>`

* #### Replace Item values based on a lookup table
First, create a CSV file with the lookup table, it must have a header with the names ```naaccrId```, ```itemValue```, and ```newItemValue```. 
For example, if we wanted to replace reportingFacility with new values from a lookup table, we would start by creating a CSV file:


    naaccrId,itemValue,newItemValue
    reportingFacility,0000090201,6000090201
    reportingFacility,0007778880,6007778880

Once we have a CSV file with the replacement values, we would run nax with the ```-rpl``` parameter set to the CSV file name and specify an output filename:

`nax <Input NAACCR XML File> -rpl <CSV file with replacement values> -o <Output NAACCR XML File>`

* #### Replace Item values based on custom script logic
First, create a Groovy script that will change data values as desired. For example, to replace full dates with partial dates that have zeros in the DAY location:
```
    if (naaccrId.startsWith('dateOf')) element.setTextContent(String.format('%s00', left(itemValue, 6)))
```    
Now, we can run nax and specify the `-s` option to run the Groovy script on every element:

`nax <Input NAACCR XML File> -s "if (naaccrId.startsWith('dateOf')) item.setItemValue(String.format('%s00', left(itemValue, 6)))" -o <Output NAACCR XML File>`

* #### Filter out Tumor records based on naaccrId values
First, we need to create a snippet of Groovy code that returns false when the Tumor should be removed. For example, we could remove all benign Tumor records 
where `behaviorCodeIcdO3` is 0 using the following Groovy script:

```
    if (tumor.getItemValue('behaviorCodeIcdO3').equals('0')) return false
```
Now, we can run nax and specify that this Groovy code runs on every Tumor by using the `-ft` option:

`nax <Input NAACCR XML File> -ft "if (tumor.getItemValue('behaviorCodeIcdO3').equals('0')) return false" -o <Output NAACCR XML File>`

---
# nax Groovy scripts
nax uses [Groovy Scripting Language](http://www.groovy-lang.org/) version 3.x

The following variables are pre-defined in every Script instance:
* inputFilename - [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) of the Groovy script filename or an auto-generated synthetic name if the script was a literal String, will always have a value
* elementName - [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) name of the XML element currently being parsed when this script is run (For example, 'Patient', 'Tumor', or 'Item'). Will be prefixed with '\<namespace>:' when external namespaces are used, will always have a value 
* naaccrData - [NaaccrData](src/main/java/edu/uky/kcr/nax/model/NaaccrData.java) object for the current XML parsing context, will always have a value
* patient - [Patient](src/main/java/edu/uky/kcr/nax/model/Patient.java) object for the current XML parsing context, will be null when parsing elements before the first Patient element
* tumor - [Tumor](src/main/java/edu/uky/kcr/nax/model/Tumor.java) object for the current XML parsing context, will be null when not parsing elements inside a Tumor element
* item - [Item](src/main/java/edu/uky/kcr/nax/model/Item.java) object for the current XML parsing context, will be null when not parsing Item elements
* element - [DOM Element](https://docs.oracle.com/en/java/javase/11/docs/api/java.xml/org/w3c/dom/Element.html) object for the currenty parsed XML Element, will always have a value
* naaccrId - [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) name of the naaccrId when parsing an 'Item' element, will be null when not parsing an Item element
* itemValue - [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) value of the Item element specified by the naaccrId, will be null when not parsing an Item element
* \<naaccrId as variable name> - Same value as itemValue, [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) value of the Item element specified by the naaccrId, will be null when not parsing an Item element. This variable will have the variable name of the naaccrId (For example, 'race1' or 'primarySite').

When writing Groovy scripts for nax, the following resources are imported automatically:
* All static methods in [StringUtils](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html)
* The class [SeerSiteRecodeUtils](https://github.com/imsweb/algorithms/blob/master/src/main/java/com/imsweb/algorithms/seersiterecode/SeerSiteRecodeUtils.java)
* The class [IcccRecodeUtils](https://github.com/imsweb/algorithms/blob/master/src/main/java/com/imsweb/algorithms/iccc/IcccRecodeUtils.java)

If you would like to use your own Java libraries in a Groovy script, add the jar files to the \<installation-directory>/user-extensions directory. 

When specifying a Groovy script as a command-line argument, you can specify the actual script as a literal String enclosed in double-quotes or you can specify the file location of a Groovy script. 
