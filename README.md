# Chemistry Service
Back-end component of chemistry functionality in Rspace.

Provides functionality for converting chemical formats, extracting information from molecules and reactions,
exporting to image and chemical search.

## Dependencies
The app has been developed with Java 21.

It is recommended to run with Docker, as the Docker image already has OpenBabel installed.

If running via maven, OpenBabel also needs to be installed on the system. 
[OpenBabel installation](https://openbabel.org/docs/Installation/install.html)

## Test
Run all tests: `mvn clean test`

## Run
#### Docker (docker-compose.yaml - production ready)

Create a new directory for this docker container (Eg. /home/user/rspace-chemistry-docker) and have the docker-compose.yaml file (from this repo) and the chemistry.jar file (from this release section) in the root of that directory. Once you have these files you can start the container.

You can start the container with: docker compose up -d
You can stop the container with: docker compose down
You can view the container logs with: docker logs <container name>
You can get the container name with: docker ps

The container stores some data that needs to be kept on container restarts, therefore there is a docker volume mounted to keep this data persistent.

As the container is designed for production use, the 8090 port is binded only to localhost

#### Docker (Build image manually - not recommended for production use )
From the root of the repo, run: `docker build -t chemistry .` to create the image named `chemistry` locally. 

Run a container on port 8090: `docker run -p8090:8090 chemistry`. In production the `/home/app/data` directory in the
container should be mounted to persist the chemical search data between container restarts.

#### Maven/Spring Boot
Run: `mvn spring-boot:run` to run the app on port 8090. Ensure OpenBabel is installed as listed above.

Chemical data for the search functionality is stored in the `./data` directory, which is created on startup if it
doesn't exist.

## Chemistry Libraries
The Indigo java library and OpenBabel linux library provide the chemistry functionality of this app.

Indigo is used for conversion, extraction, and image generation, and OpenBabel is used for conversion and search.

## Functionality
The app provides the following functionality:
- conversion between chemical formats
- extraction of information from molecules and reactions
- exporting to image
- chemical search

### Conversion
Conversion is supported from any format supported by Indigo, to the formats listed in 
`util.com.researchspace.IndigoFacade.convert`

### Extraction
Basic information is extracted from chemicals using the Indigo library. Information currently extracted is:
- atom count
- bond count
- mass

### Image Export
Images of chemical structures can be generated from any format supported by Indigo. The image is returned as a byte
array in one of the following formats:
- jpg
- png
- svg

### Search
The search functionality uses the OpenBabel linux library, which is installed in the Docker image, and if running without
Docker, should be installed on the system. The search functionality is file-based, and works by first storing any chemicals
to be searched via the `/chemical/save` endpoint. Chemicals are stored in smiles format along with their id. 

When a search is performed, OpenBabel checks the chemistry file for any structure or substructure matches, and returns a
list of ids for those matches.

## Swagger
Auto-generated swagger documentation is available at (by default) `http://localhost:8090/swagger-ui/index.html`


