FROM postgres:15

# Set environment variables for PostgreSQL
ENV POSTGRES_DB=chemistry
ENV POSTGRES_USER=chemistry_user
ENV POSTGRES_PASSWORD=chemistry_password

# Install dependencies for Bingo
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    build-essential \
    postgresql-server-dev-15 \
    python3 \
    python3-pip \
    libstdc++6 \
    libc6-dev \
    file \
    && rm -rf /var/lib/apt/lists/*

# Download and install Bingo PostgreSQL extension
WORKDIR /tmp
RUN wget https://lifescience.opensource.epam.com/downloads/bingo-1.29.0/bingo-postgres-15-linux-x86_64.zip \
    && unzip bingo-postgres-15-linux-x86_64.zip \
    && tar -xzf bingo-postgres15-linux-x86_64-1.29.0.0.tgz \
    && cd bingo-postgres15-linux-x86_64-1.29.0.0 \
    && chmod +x bingo-pg-install.sh \
    && ./bingo-pg-install.sh -y -pglibdir \
    && cp lib/libbingo-postgres.so $(pg_config --pkglibdir)/ \
    && ls -la $(pg_config --pkglibdir)/libbingo-postgres.so \
    && file $(pg_config --pkglibdir)/libbingo-postgres.so \
    && uname -m \
    && mkdir -p $(pg_config --sharedir)/extension \
    && echo "# bingo extension" > $(pg_config --sharedir)/extension/bingo.control \
    && echo "comment = 'Bingo chemical search cartridge'" >> $(pg_config --sharedir)/extension/bingo.control \
    && echo "default_version = '1.29.0'" >> $(pg_config --sharedir)/extension/bingo.control \
    && echo "relocatable = false" >> $(pg_config --sharedir)/extension/bingo.control \
    && echo "schema = bingo" >> $(pg_config --sharedir)/extension/bingo.control \
    && sed "s|/tmp/bingo-postgres15-linux-x86_64-1.29.0.0/lib/|\$libdir/|g" bingo_install.sql > $(pg_config --sharedir)/extension/bingo--1.29.0.sql

# Copy initialization scripts
COPY init-scripts/ /docker-entrypoint-initdb.d/

# Expose PostgreSQL port
EXPOSE 5432

# Use the default PostgreSQL entrypoint
CMD ["postgres"]