
#export APP_NAME=cp-tsdata-demo-1
export APP_NAME=cp-tsdata-demo-2
export APP_VERSION=v1

cd ..

echo "export APP_NAME=$APP_NAME" > ops/cloud-app-instances/$APP_NAME/released-app/cfg/env.sh

echo "export APP_VERSION=$APP_VERSION" >> ops/cloud-app-instances/$APP_NAME/released-app/cfg/env.sh

mvn clean compile package install

cp target/*.jar ops/cloud-app-instances/$APP_NAME/released-apps

docker build . -t $APP_NAME

#
# Copy the schema files if needed into the "application's
# home repository folder".
#
cp ./src/main/avro/*.avsc ops/cloud-app-instances/$APP_NAME/released-app/kst-context/schemas
