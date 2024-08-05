echo "*************   package app    *************"
mvn clean package
echo "*************   run services   *************"
docker-compose up --build