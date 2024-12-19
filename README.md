# Chemistry Service
Back-end component of chemistry functionality in Rspace.

Provides functionality for converting chemical formats, extracting information from molecules and reactions,
exporting to image and chemical search.

## Dependencies
It is recommended to run with Docker, in which case none of the rest of this section applies.

The app has been developed with Java 21.

If running via maven/spring boot, a file needs to be created `src/main/resources/chemical_files/non-indexed-new-chemicals.smi`.

OpenBabel also needs to be installed on the system. [OpenBabel installation](https://openbabel.org/docs/Installation/install.html)

## Test
Run all tests: `mvn clean test`

## Run
#### Docker
From the root of the repo, run: `docker build -t chemistry .` to create the image named `chemistry` locally. 

Start a container on port 8090 from the image with: `docker run -p8090:8090 chemistry`

To persist the chemistry data outside the container lifecycle, mount a volume (which will create a folder `./chemical_files`    
on the host containing the persistent files) with: 
`docker run -p8090:8090 -v ./chemical_files:/home/app/src/main/resources/chemical_files chemistry`

#### Maven/Spring Boot
Run: `mvn spring-boot:run` to run the app on port 8090. Ensure the dependencies listed above are satisfied.


