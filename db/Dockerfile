FROM postgres:14

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive

# Create a directory for Bingo files
RUN mkdir -p /opt/bingo

# Download pre-built Bingo binaries
WORKDIR /opt/bingo
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    wget -q https://github.com/epam/Indigo/releases/download/indigo-1.4.0-beta.r22/indigo-postgres-1.4.0-beta.r22-linux.zip && \
    apt-get install -y --no-install-recommends unzip && \
    unzip indigo-postgres-1.4.0-beta.r22-linux.zip && \
    cp -r indigo-postgres/* /usr/share/postgresql/14/extension/ && \
    rm -rf indigo-postgres* && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres
ENV POSTGRES_DB=bingo

# Copy initialization scripts
COPY init-scripts/ /docker-entrypoint-initdb.d/

# Expose PostgreSQL port
EXPOSE 5432
