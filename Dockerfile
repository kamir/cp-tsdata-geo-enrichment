FROM gradle:jdk11

RUN apt-get update
RUN apt-get install -y git
RUN apt-get install -y maven

## Get source from git repo https://github.com/christophschubert/kafka-clusterstate-tools.git
RUN git clone -b https://github.com/kamir/cp-tsdata-geo-enrichment /app/cp-tsdata-geo-enrichment

RUN cd /app/cp-tsdata-geo-enrichment && mvn package