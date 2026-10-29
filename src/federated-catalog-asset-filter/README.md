# Federated Catalog Asset Filter
A lightweight EDC extension that exposes a REST endpoint to query assets stored in a Federated Catalog using flexible, JSON-defined conditions. It translates filters into SQL over catalog JSON, executes within an EDC transaction, and returns normalized JSON.
## Features
- REST API: POST /federated-catalog/search
- JSON input → SQL generation
- Filter types:
    - SimpleObject (plain string fields; asset or catalog scope)
    - SimpleArray (arrays of strings; all values must be present)
    - ArrayObject (arrays of objects with language-tagged values)
    - NestedObject (one-level nested objects with language-tagged values)

- Results include: id, asset (full JSON), participantId, originatorURL
- Type-safe JSON conversion (timestamps, numeric, boolean, JSON/JSONB)
- Runs via EDC DataSourceRegistry + TransactionContext

## Module
- Path: src/federated-catalog-asset-filter

## API
- Base path: /federated-catalog
- Endpoint: POST /search
- Consumes/Produces: application/json

## Request body
- Object with a conditions array. Each condition:
    - type: SimpleObject | SimpleArray | ArrayObject | NestedObject
    - name: field path (varies per type)
    - operator: eq | contains (accepts typo “contanis”)
    - value: string (comma-separated list for SimpleArray)
    - source: asset | catalog (for SimpleObject)

Examples:
- SimpleObject (asset scope): { "type": "SimpleObject", "source": "asset", "name": "title", "operator": "contains", "value": "weather" }
- SimpleArray: { "type": "SimpleArray", "name": "tags", "value": "climate,public" }
- ArrayObject (array.field): { "type": "ArrayObject", "name": "pack.name", "operator": "eq", "value": "Dataset A" }
- NestedObject (parent.child): { "type": "NestedObject", "name": "customObject.customProperty", "operator": "contains", "value": "EU" }

Full request: POST /federated-catalog/search Content-Type: application/json
{ "conditions": }
## Response
- JSON array; each item:
    - id: dataset id
    - asset: dataset JSON
    - participantId: string
    - originatorULR: string

- On error: {"error":"message"}

## How it works
- Parses conditions and builds a SQL query over the catalog JSON (expands datasets, adds lateral joins as needed).
- Executes inside an EDC transaction using the “default” data source.
- Streams the ResultSet to JSON with sensible type conversions.

## Prerequisites
- EDC runtime with:
    - DataSourceRegistry configured with a “default” datasource pointing to the federated catalog DB
    - TransactionContext available

- Catalog data stored as JSON with datasets under catalog->'datasets'

## Build
- From project root:
    - ./gradlew :federated-catalog-asset-filter:build

## Deploy
- Package and place the extension on the EDC runtime classpath.
- Ensure it’s registered as a ServiceExtension and that your runtime exposes JAX-RS resources.

## Notes
- SimpleArray requires all provided values to be present (logical AND).
- ArrayObject and NestedObject assume language-tagged values stored as arrays with @value.
- Supported operators: eq, contains.
- For SimpleObject, source controls namespace (asset vs catalog).
- Query text is logged; avoid putting secrets in values.

## Troubleshooting
- Error payload: check runtime logs for “Executing SQL” and “SQL error”.
- No results: verify condition type, operator, field path, and scope (asset/catalog).
- Performance: logs include query time; consider jsonb indexes if needed.