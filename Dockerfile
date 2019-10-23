FROM clojure
COPY . /usr/src/donttellmybro_proxy
WORKDIR /usr/src/donttellmybro_proxy
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*donttellmybro-proxy\.jar\)/\1/p')" donttellmybro-proxy.jar
EXPOSE 9090
CMD ["java", "-jar", "donttellmybro-proxy.jar"]