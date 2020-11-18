FROM gradle:jdk11

RUN apt-get update
RUN apt-get install -y git
RUN apt-get install -y maven

RUN mkdir /app
RUN mkdir /app/cp-tsdata-geo-enrichment

RUN git clone https://github.com/kamir/cp-tsdata-geo-enrichment.git /app/cp-tsdata-geo-enrichment

RUN cd /app/cp-tsdata-geo-enrichment && mvn clean compile package -U
