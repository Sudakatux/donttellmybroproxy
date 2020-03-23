# clj.donttellmybroproxy

The goal is for an interactive proxy.
Started writing this to be able to test ui integrations, change headers etc, and ended finding it usefull for other backend integratios as well

Used code from https://github.com/tailrecursion/ring-proxy when migrated from http-kit to support ssl
and streams
 
## Building and running

### Building the UI
This project uses shadow-cljs So you'll need to build the production build so:
> `yarn` to install dependencies
> `npx shadow-cljs release app` to build the source 

### Building serverside
If you use leiningen https://leiningen.org then `lein uberjar` will build the source
 
and `java -jar donttellmybroproxy/target/uberjar/donttellmybro-proxy.jar` should run it

### Alternativly using Docker
If you prefer the docker way then build the container with :
`docker build -t donttelmybroproxy . `

Check if the build went well, meaning it did not error out.

To run `docker run -p 3000:3000 -p 3001:3001 donttelmybroproxy `

**Note** that by default the proxy is not running you must hit the play button to run it

## Usage

Go to http://localhost:3000 start the proxy server and associate routes by providing a unique id a route and a
desired destination for example
id: yahoo
route: /yahoo
destination: http://yahoo.com

After clicking play you should be able to go to localhost:3001/yahoo and see yahoo
you can add new routes with the running server no need to re-start

You can edit the created route, add request response matchers RegEx for urls. For a given matcher
you can add headers that will be used for the request response. Matchers will be applied in order to a given
request response meaning if you have two or more matchers that match for the same url it will merge them together


## Current state of art
> You can associate routes to hosts.

> Add additional response and request headers for a given matcher

> Record traffic for a specific route.

> Create interceptors given a recording

> Respond a specific body for the given matcher

> Stop actual request. Usefull to record and play without making the actual request

## License

Copyright © 2019 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
