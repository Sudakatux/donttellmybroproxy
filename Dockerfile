FROM clojure
COPY . /usr/src/donttellmybro_proxy
WORKDIR /usr/src/donttellmybro_proxy
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar
EXPOSE 9090
CMD ["java", "-jar", "app-standalone.jar"]