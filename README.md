# How to setup in a docker/podman container
## Creating the Image
1. Use the jdbc-tester.war file found in the target directory
2. Setup the properties file: databases.properties
   - Guideline for the how the file needs to look can be found in the databases.properties file under the resources directory
3. Verify the Dockerfile has what you need and expects in this repo
   - If you need to make changes to the Dockerfile, please create a new branch to not override the template of it here
4. Build the image, make sure you are in the base directory where the Dockerfile is located
   - podman build -t jdbc-tester .
   - docker build -t jdbc-tester .
5. Once built run the image in a container like the following
>**Note:**
> -v sets the path to an external databases.properties file that maps the file within the container
> -e sets the DB_CONFIG_PATH as an environment variable so tomcat knows which path to take for the properties
   - docker run -d --restart=unless-stopped -p 8080:8080 --name jdbc-tester -v /home/$(whoami)/databases.properties:/tmp/databases.properties:ro -e DB_CONFIG_PATH=/tmp/databases.properties jdbc-tester
   - podman run -d --restart=unless-stopped -p 8080:8080 --name jdbc-tester -v /home/$(whoami)/databases.properties:/tmp/databases.properties:ro -e DB_CONFIG_PATH=/tmp/databases.properties jdbc-tester
6. Once the container is running, check the logs to make sure there are no issues
  - docker logs -f jdbc-tester
  - podman logs -f jdbc-tester
