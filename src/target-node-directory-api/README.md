
# Target Node Directory API

A lightweight REST API extension that exposes CRUD-style endpoints to manage Target Nodes in a directory. It registers a JAX-RS controller under /target-node-directory and integrates with the EDC runtime via a ServiceExtension.

## Features

- List all target nodes
- Get a target node by id
- Create a new target node
- Delete a target node by id
- Case-insensitive identifiers (ids are normalized to lowercase on create and lookup)

## Module Layout

- Extension: initializes and registers the API controller with the EDC WebService
- Controller: JAX-RS resource implementing the HTTP endpoints
- SPI dependencies: uses TargetNodeDirectory for persistence, JsonLd for namespace registration, and Monitor for logging

## Prerequisites

- Java 23
- An EDC-based runtime with:
  - WebService enabled (REST)
  - TargetNodeDirectory available in the DI container (backed by your chosen storage)
  - JsonLd service (for namespace registration)
  - QueryExecutor if your runtime wiring requires it

## Build


bash
From repository root
./gradlew :target-node-directory-api:build```

## Install

Add the built JAR to your EDC runtime’s extensions (e.g., libs or extensions folder depending on your setup), or include this module as a Gradle dependency in your distribution.

Ensure the ServiceExtension is discoverable via ServiceLoader (META-INF/services/org.eclipse.edc.spi.system.ServiceExtension is already provided in this module).

## Configuration

No mandatory configuration keys are required by this module. The extension expects the following services to be available in the runtime:

- TargetNodeDirectory
- WebService
- JsonLd
- Monitor
- (Optional) QueryExecutor, depending on your environment wiring

When initialized, the extension:
- Logs initialization
- Registers a JSON-LD namespace: prefix nexus -> https://w3id.org/nexus/v0.0.1/ns/
- Registers the REST controller

## API

Base path: /target-node-directory  
Media types: application/json

- GET /target-node-directory
    - Description: Returns all target nodes
    - Response: 200 OK with JSON array

- GET /target-node-directory/{id}
    - Description: Returns the target node with the given id (case-insensitive)
    - Responses:
        - 200 OK with JSON object
        - 404 Not Found if the node does not exist
        - 500 Internal Server Error on unexpected errors

- POST /target-node-directory
    - Description: Creates a new target node
    - Request body: JSON of a TargetNode; if id is present, it will be normalized to lowercase
    - Responses:
        - 201 Created with the created node
        - 409 Conflict if a node with the same id already exists
        - 500 Internal Server Error on unexpected errors

- DELETE /target-node-directory/{id}
    - Description: Removes a node by id (case-insensitive)
    - Responses:
        - 204 No Content when deletion is processed (the removed entity is included in the response body for reference)
        - 500 Internal Server Error on unexpected errors

Notes:
- The controller serializes/deserializes using Jackson.
- The TargetNode model must be available on the classpath and be compatible with the directory implementation used by your runtime.
- Id matching is lowercase-only; clients may send ids in any case, but the system stores and compares them in lowercase.

## Example Requests

- List all:
  curl -s http://localhost:8181/target-node-directory

- Get by id:
  curl -s http://localhost:8181/target-node-directory/my-node-id

- Create:
  curl -s -X POST http://localhost:8181/target-node-directory \
  -H "Content-Type: application/json" \
  -d '{"id":"My-Node-Id","name":"My Node","endpoint":"https://example.org/api"}'

- Delete:
  curl -s -X DELETE http://localhost:8181/target-node-directory/my-node-id

## Error Handling

- 404 when a requested node does not exist
- 409 when creating a node that already exists
- 500 for unexpected exceptions (message included in response)

## Development

Run unit tests and build:
- ./gradlew :target-node-directory-api:build

If you embed this module in a custom runtime:
- Ensure the WebService registers JAX-RS resources
- Provide a TargetNodeDirectory implementation (e.g., in-memory or backed by a database)
- Confirm the REST port and base path are exposed by your runtime
```
