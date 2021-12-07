#! /bin/bash

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q10.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=15
      else 
            java -jar ISWC2015-CityBench.jar query=Q10.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=15
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q10_5.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=15
      else 
            java -jar ISWC2015-CityBench.jar query=Q10_5.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=15
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=15
      else 
            java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=1 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=15
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=2 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=15
      else 
            java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=2 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=15
      fi
done

for (( i = 1 ; i <= 3 ; i++ ))  do
      if [[ "$i" -eq 3 ]]; then
            java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=5 queryDuplicates=1 duration=600s rdfoxLicense=./RDFox.lic queryInterval=15
      else 
            java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=5 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=15
      fi
done

# for (( i = 1 ; i <= 3 ; i++ ))  do
#       if [[ "$i" -eq 3 ]]; then
#             java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=10 queryDuplicates=1 duration=300s rdfoxLicense=./RDFox.lic queryInterval=15
#       else 
#             java -jar ISWC2015-CityBench.jar query=Q10_8.txt engine=rdfox startDate=2014-08-11T11:00:00 endDate=2014-08-31T11:00:00 frequency=10 queryDuplicates=1 duration=130s rdfoxLicense=./RDFox.lic queryInterval=15
#       fi
# done
