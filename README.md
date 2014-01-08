# thumbnail-service

This is a simple RESTful web service that serves as an intermediary between an
application and any number of web services that provide book, album, DVD or
other publication cover images.  Several higher level functions are provided.

## Aggregation

Several sources may be queried to respond to a single request.  This service
has the potential to make intelligent decisions about ordering, concurrency
or routing of particular requests.

## Caching

Until the cache is manually cleared, requests are made only once for a
particular resource to a particular service (whether the content is found to
be provided by that service or not).

# Getting Started

## Requirements

* ImageMagick must be installed and the commands "identify" and "convert" must be available
* Java 6
* A web application container (Tomcat, jetty, etc.)
* Maven 3 (to build)

This initial version is largely a proof-of-concept implementation.  There are
only back-end services configured (the Google books API and LastFM).  To access
the LastFM source you must provide an API key in the file
src/main/resources/last-fm.properties.

Running the application can be done by typing:
mvn clean jetty:run

Then you can make queries against various identifiers:

OCLC Number:
http://localhost:8080/webapi/thumbnail?OCLC=76141517

ISBN:
http://localhost:8080/webapi/thumbnail?ISBN=0312932081

Album, Artist:
http://localhost:8080/webapi/thumbnail?artist=Low&album=Trust

And others....


# Roadmap

There are a few features that will be implemented if this application is to be
used locally in a production environment:

1.  documentation and better responses from the single web end point
2.  configurable sources, default image, mime types, etc.
3.  an optional timeout on REST requests (so that it returns the default image after X seconds assuming none of the sources have returned results by then)
4.  cache management (right now it just fills up)


