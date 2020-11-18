FROM gradle:jdk8

RUN mkdir /app_tmp

RUN apt-get update
RUN apt-get install -y git
RUN apt-get install -y maven

RUN mkdir /app
RUN mkdir /app/cp-tsdata-geo-enrichment

RUN git clone https://github.com/kamir/cp-tsdata-geo-enrichment.git /app/cp-tsdata-geo-enrichment

# RUN cd /app/cp-tsdata-geo-enrichment && mvn clean compile package install -U

COPY target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar /app/cp-tsdata-geo-enrichment/target/

COPY ccloud.props.secure /app/cp-tsdata-geo-enrichment/ccloud.props

WORKDIR /app/cp-tsdata-geo-enrichment/

CMD exec java -jar /app/cp-tsdata-geo-enrichment/target/cp-tsx-geoenrichment-use-case-1.0-SNAPSHOT.jar /app/cp-tsdata-geo-enrichment/data/in/
