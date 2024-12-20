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
#### Docker
From the root of the repo, run: `docker build -t chemistry .` to create the image named `chemistry` locally. 

Run a container on port 8090: `docker run -p8090:8090 chemistry`. In production the `/home/app/data` directory in the
container should be mounted to persist the chemical search data between container restarts.

#### Maven/Spring Boot
Run: `mvn spring-boot:run` to run the app on port 8090. Ensure OpenBabel is installed as listed above.

Chemical data for the search functionality is stored in the `./data` directory, which is created on startup if it
doesn't exist.


