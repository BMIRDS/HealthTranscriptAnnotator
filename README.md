# Health Transcript Annotator: An annotator of mentions for important health-related items in patient-physician office visit transcripts

By Craig Ganoe and Saeed Hassanpour


![Medication Approach](./figures/MedicationApproach.png)

## Dependencies
* [Java 7+](https://www.tensorflow.org/)
* [Apache cTAKES 4.0.0](https://ctakes.apache.org/)

# Usage

## 1. Dataset
The Health Transcript Annotator (HTA) takes as input a directory of plain text files which each contain a transcript of a patient's visit with their physician.


## 2. cTAKES
The Health Transcript Annotator (HTA) uses Apache cTAKES as a preprocessor of the transcript files. cTAKES outputs data in an XML format (.xmi). 

Installation instructions available [here](https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+4.0+User+Install+Guide).

HTA in our work uses the cTAKES [Default Clinincal Pipeline](https://cwiki.apache.org/confluence/display/CTAKES/Default+Clinical+Pipeline) for its preprocessing.

From an installed cTAKES, the Default Clinical Pipeline is invoked:

`bin/runClinicalPipeline  -i inputDirectory  --xmiOut outputDirectory  --user umlsUsername  --pass umlsPassword`

## 3. Health Transcript Annotator
The Health Transcript Annotator takes a folder of cTAKES output (CAS .XMI files) as its input.



## 4. Evaluation
### Dataset 


## 5. Results



![Medication Results](./figures/MedicationResults.png)