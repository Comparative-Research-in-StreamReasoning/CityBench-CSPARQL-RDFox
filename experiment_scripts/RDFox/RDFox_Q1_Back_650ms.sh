#! /bin/bash

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q1.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=650
      else 
            java -jar ISWC2015-CityBench.jar query=Q1.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=650
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q1_20MB.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=650
      else 
            java -jar ISWC2015-CityBench.jar query=Q1_20MB.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=650
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q1_30MB.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=650
      else 
            java -jar ISWC2015-CityBench.jar query=Q1_30MB.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=650
      fi
done