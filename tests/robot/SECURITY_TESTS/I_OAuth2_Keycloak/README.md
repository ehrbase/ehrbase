A little documentation for my future me (cause I really don't want to go
through that f:rage: keycloak documentation ever again)


RESTORE KEYCLOAK FROM PREVIOUSLY EXPORTED CONFIGURATION
=======================================================
(run all commands listed below from within this folder)


1) START KC W/ A MOUNTED VOLUME
    ```bash
    docker run -d --name keycloak \
        -p 8081:8080 \
        -v $(pwd)/exported-keycloak-config:/restore-keycloak-config \
        -e KEYCLOAK_USER=admin \
        -e KEYCLOAK_PASSWORD=admin \
        jboss/keycloak:10.0.2
    ```


2) RESTORE CONFIG FROM DIRECTORY
    ```bash
    docker exec -it keycloak /opt/jboss/keycloak/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=100 \
        -Dkeycloak.migration.action=import \
        -Dkeycloak.migration.provider=dir \
        -Dkeycloak.profile.feature.upload_scripts=enabled \
        -Dkeycloak.migration.dir=/restore-keycloak-config \
        -Dkeycloak.migration.strategy=OVERWRITE_EXISTING
    ```

   When the import is complete use Ctrl-C to exit the session.

NOTE: This is a minimal setup using Keycloak's embedded H2 DB which is just enough for testing.
      It's probably a good idea not to use this in production :)


If you ever have to reconfigure Keycloak Docker setup manually and to recreate the export
follow the steps below:




EXPORT COMPLETE KEYCLOAK CONFIGURATION
======================================

1) START KC W/ A MOUNTED VOLUME

    ```bash
    docker run -d --name keycloak \
    -p 8081:8080 \
    -v $(pwd)/exported-keycloak-config:/restore-keycloak-config \
    -e KEYCLOAK_USER=admin \
    -e KEYCLOAK_PASSWORD=admin \
    jboss/keycloak:10.0.2
    ```


2) LOGIN AS ADMIN AND CONFIGURE KC TO YOUR NEEDS
    - a) create realm: ehrbase
    - b) create client: ehrbase-robot
      - IMPORTANT: make sure in client settings
        - 'Access Type' is set to **public**
        - 'Direct Access Grants Enabled' is set to **ON**
      
    - c) create user: robot w/ passwort robot


3) EXPORT CONFIGURATION INTO MULTIPLE FILES WITHIN A DIRECTORY

    ```bash
    docker exec -it keycloak /opt/jboss/keycloak/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=100 \
        -Dkeycloak.migration.action=export \
        -Dkeycloak.migration.provider=dir \
        -Dkeycloak.migration.dir=/opt/jboss/keycloak/export-dir \
        -Dkeycloak.migration.usersPerFile=1000 \
        -Dkeycloak.migration.strategy=OVERWRITE_EXISTING
    ```


   When the export is complete use Ctrl-C to exit the session.
   The export is complete when you see something like

    ```
    Keycloak 10.0.2 (WildFly Core 11.1.1.Final) started in 11390ms - 
    Started 591 of 889 services (606 services are lazy, passive or on-demand)
    ```

4) COPY EXPORTED CONFIGURATION FROM CONTAINER TO YOUR HOST

    ```bash
    docker cp keycloak:/opt/jboss/keycloak/export-dir ./exported-keycloak-config
    ```

   optional
    before copying check the folder exists and contains exported config files:
    ```bash
    docker exec -it keycloak bash
    ls /opt/jboss/keycloak/export-dir
    ```



---

Alternatively (and in case above steps stop to work for what ever reason) it is possible
to export complete KC configuration into a single JSON file:

1) START KEYCLOAK W/ MOUNTED VOLUME

    ```bash
    docker run -d --name keycloak \
        -p 8081:8080 \
        -v $(pwd):/workspace \
        -e KEYCLOAK_USER=admin \
        -e KEYCLOAK_PASSWORD=admin \
        jboss/keycloak:10.0.2
    ```


2) EXPORT (SINGLE FILE)

    Then export your database into a single JSON file:

    ```bash
    docker exec -it keycloak /opt/jboss/keycloak/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=100 \
        -Dkeycloak.migration.action=export \
        -Dkeycloak.migration.provider=singleFile \
        -Dkeycloak.migration.file=/workspace/exported-kc-config-single-file/keycloak-export.json
        -Dkeycloak.migration.strategy=OVERWRITE_EXISTING
    ```


3) IMPORT FROM THE COMMAND LINE

    Start with a blank canvas ...

    ```bash
    docker container stop keycloak
    docker container rm keycloak

    docker run -d --name keycloak \
        -p 8081:8080 \
        -v $(pwd):/workspace \
        -e KEYCLOAK_USER=admin \
        -e KEYCLOAK_PASSWORD=admin \
        jboss/keycloak:10.0.2
    ```

    To import from a (previously exported) file into your database ...

    ```bash
    docker exec -it keycloak /opt/jboss/keycloak/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=100 \
        -Dkeycloak.migration.action=import \
        -Dkeycloak.migration.provider=singleFile \
        -Dkeycloak.migration.file=/workspace/exported-kc-config-single-file/keycloak-export.json
    ```

    When the import is complete use Ctrl-C to exit the session.




---

        ** WARNING **                                 
        DO NOT TRY TO RESTORE KEYCLOAK W/ `-e KEYCLOAK_IMPORT=/path-to/exported-config.json`
        APPROACH AS DOCUMENTED ON KEYCLOAK'S DOCKER IMAGE DISCRIBTION ON DOCKER HUB.
        /////////////////////////////////////////////////////////////////                 
        ////                      THAT DOES NOT WORK!                ////
        /////////////////////////////////////////////////////////////////       
        DON'T WASTE YOUR TIME! I'VE BEEN THERE, I'VE DONE THAT!
