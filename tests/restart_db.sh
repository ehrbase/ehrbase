# restart postgresql db container

## 1. Remove all containers and volumes
docker container rm --force $(docker container ls -q)
docker wait $(docker container ls -q)
docker system prune --volumes --force

## 2. then restart DB
docker run -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 ehrbaseorg/ehrbase-database-docker:11.5