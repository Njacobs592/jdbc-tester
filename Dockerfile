FROM docker.io/tomcat:8-jre8-slim

# Remove default ROOT webapp
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR into webapps
COPY target/jdbc-tester.war /usr/local/tomcat/webapps/

# Expose port
EXPOSE 8080

# Run Tomcat
CMD ["catalina.sh", "run"]

