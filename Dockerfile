FROM maven:3.9.16-eclipse-temurin-25 AS builder
WORKDIR /home/app
ADD . /home/app/chemistry
RUN cd chemistry && mvn -B -ntp -Dmaven.test.skip=true -Djar.finalName=chemistry clean package

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
  && apt-get install --yes --quiet --no-install-recommends openbabel\
  && apt-get clean && rm -rf /var/lib/apt/lists/* \
ENV PATH=$OPENBABEL_HOME/bin:$PATH
COPY --from=builder /home/app/chemistry/target/chemistry.jar /home/app/chemistry.jar
EXPOSE 8090
WORKDIR /home/app/chemistry
ENTRYPOINT ["java", "-jar", "/home/app/chemistry.jar"]
