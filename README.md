# clj.donttellmybroproxy

The goal is for an interactive proxy. You can currently create routes and associate to a route

## Building and running
If you use leiningen https://leiningen.org then `lein uberjar` will build the source 
and `java -jar donttellmybroproxy/target/uberjar/donttellmybro-proxy.jar` should run it
If you prefer the docker way then build the container with :
`docker build -t donttelmybroproxy . `

Check if the build went well, meaning it did not error out.

To run `docker run -p 3000:3000 -p 3001:3001 donttelmybroproxy `

**Note** that by default the proxy is not running you must hit the play button to run it

## Usage

go to http://localhost:3000 start the proxy server and associate routes by providing a unique id a route and a
desired destination for example
id: yahoo
route: /yahoo
destination: http://yahoo.com

After clicking play you should be able to go to localhost:3001/yahoo and see yahoo
you can add new routes with the running server no need to re-start

## Current state of art
> You can only associate routes to hosts.

> Add additional response and request headers for an existing route

## License

Copyright © 2019 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
