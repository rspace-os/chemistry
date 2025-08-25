FROM postgres:17

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies for Bingo
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    build-essential \
    postgresql-server-dev-17 \
    python3 \
    python3-pip \
    libstdc++6 \
    libc6-dev \
    file \
    && rm -rf /var/lib/apt/lists/*

# Download and install Bingo PostgreSQL extension
WORKDIR /tmp
RUN wget https://lifescience.opensource.epam.com/downloads/bingo-1.32.0/bingo-postgres-17-linux-x86_64.zip \
    && unzip bingo-postgres-17-linux-x86_64.zip \
    && tar -xzf bingo-postgres17-linux-x86_64-1.9.1.tgz \
    && cd bingo-postgres17-linux-x86_64-1.9.1 \
    && chmod +x bingo-pg-install.sh \
    && ./bingo-pg-install.sh -y -pglibdir \
    && cp lib/libbingo-postgres.so $(pg_config --pkglibdir)/ \
    && ls -la $(pg_config --pkglibdir)/libbingo-postgres.so \
    && file $(pg_config --pkglibdir)/libbingo-postgres.so \
    && uname -m \
    && mkdir -p $(pg_config --sharedir)/extension \
    && echo "# bingo extension" > $(pg_config --sharedir)/extension/bingo.control \
    && echo "comment = 'Bingo chemical search cartridge'" >> $(pg_config --sharedir)/extension/bingo.control \
    && echo "default_version = '1.32.0'" >> $(pg_config --sharedir)/extension/bingo.control \
    && echo "relocatable = false" >> $(pg_config --sharedir)/extension/bingo.control \
    && echo "schema = bingo" >> $(pg_config --sharedir)/extension/bingo.control \
    && sed "s|/tmp/bingo-postgres17-linux-x86_64-1.9.1/lib/|\$libdir/|g" bingo_install.sql | sed '/^BEGIN;$/d' | sed '/^COMMIT;$/d' | sed '/^CREATE SCHEMA bingo;$/d' > $(pg_config --sharedir)/extension/bingo--1.32.0.sql

# Set environment variables
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres
ENV POSTGRES_DB=bingo

# Copy initialization scripts
COPY init-scripts/ /docker-entrypoint-initdb.d/

# Expose PostgreSQL port
EXPOSE 5432
