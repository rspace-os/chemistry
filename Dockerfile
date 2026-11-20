# Ubuntu only packages OpenBabel 3.1.1, so build the current release from source
FROM ubuntu:24.04 AS openbabel-builder
ARG OPENBABEL_VERSION=3.2.1
ARG OPENBABEL_SHA256=e140c25480fe1678d00b9a52462368fa4e7805fba67b12ee496784437f3e239e
RUN apt-get update \
  && apt-get install --yes --quiet --no-install-recommends \
    ca-certificates curl cmake make g++ \
    libxml2-dev zlib1g-dev libeigen3-dev libcairo2-dev rapidjson-dev \
    libboost-filesystem-dev libboost-iostreams-dev libboost-program-options-dev libboost-test-dev \
    libmaeparser-dev \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /tmp/openbabel
RUN tag="openbabel-$(echo "${OPENBABEL_VERSION}" | tr . -)" \
  && curl -fsSL -o openbabel.tar.gz "https://github.com/openbabel/openbabel/archive/refs/tags/${tag}.tar.gz" \
  && echo "${OPENBABEL_SHA256}  openbabel.tar.gz" | sha256sum -c - \
  && tar -xzf openbabel.tar.gz --strip-components=1 \
  && cmake -S . -B build \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX=/opt/openbabel \
    -DBUILD_GUI=OFF \
    -DENABLE_TESTS=OFF \
  && cmake --build build --parallel "$(nproc)" \
  && cmake --install build

FROM maven:3.9.16-eclipse-temurin-25 AS builder
WORKDIR /home/app
ADD . /home/app/chemistry
RUN cd chemistry && mvn -B -ntp -Dmaven.test.skip=true -Djar.finalName=chemistry clean package

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
  && apt-get install --yes --quiet --no-install-recommends \
    libxml2 zlib1g libcairo2 libmaeparser1 libboost-iostreams1.83.0 \
  && apt-get clean && rm -rf /var/lib/apt/lists/*
COPY --from=openbabel-builder /opt/openbabel /opt/openbabel
ENV PATH="/opt/openbabel/bin:${PATH}"
COPY --from=builder /home/app/chemistry/target/chemistry.jar /opt/chemistry/chemistry.jar
EXPOSE 8090
WORKDIR /home/app/chemistry
ENTRYPOINT ["java", "-jar", "/opt/chemistry/chemistry.jar"]
