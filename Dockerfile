FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /home/app
ADD . /home/app/chemistry
RUN cd chemistry && ./mvnw -Dmaven.test.skip=true -Djar.finalName=chemistry clean package

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
  && apt-get install --yes --quiet --no-install-recommends openbabel\
  && apt-get clean && rm -rf /var/lib/apt/lists/* \
ENV PATH=$OPENBABEL_HOME/bin:$PATH
COPY --from=builder /home/app/chemistry/target/chemistry.jar /home/app/chemistry.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/home/app/chemistry.jar"]