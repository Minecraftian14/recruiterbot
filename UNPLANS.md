# API Version Routing Proposal

This document specifies a future API-routing behavior for an implementation agent. It is a proposal, not an instruction
to change the current API immediately. Treat the rules below as applying to APIs that use the versioned
`/api/v{number}/...` route format; do not assume any existing endpoint must keep its current route or response shape.

## Goal

Allow endpoint versions to evolve independently while offering one stable, unversioned API alias. A service may expose
different versions for different endpoint routes at the same time. A missing requested version falls back to the newest
implementation available for that endpoint.

## Route forms

For a logical endpoint path called `point`, support these forms:

| Route form          | Meaning                                                                                     |
|---------------------|---------------------------------------------------------------------------------------------|
| `/api/v{N}/{point}` | Request version `N` of the endpoint. `N` is a positive decimal integer.                     |
| `/api/{point}`      | Version-neutral alias. Resolve it to the newest available implementation for that endpoint. |

The path after the version prefix is the endpoint path. It may contain nested segments and path parameters, for example
`candidates/{id}/matches`. Preserve those segments and parameter values during resolution. Do **not** create or infer a
bare `/{point}` alias. For example, `/candidates` is not an alias under this proposal.

Examples of independently versioned endpoints include:

- `/api/v1/candidates`
- `/api/v1/jobs`
- `/api/v2/jobs`
- `/api/v1/candidates/{id}/matches`

There is no global version number for the entire API. Each logical endpoint may have its own set of available versions.

## Version resolution

Resolve a request using its HTTP method and logical endpoint route (the route template after removing `/api/vN` or
`/api`). Do not use a concrete path parameter value, query string, request ID, or body value to choose an
implementation.

1. If the requested `/api/vN/...` route has an implementation for that method and logical endpoint, use that exact
   version.
2. If the requested version has no implementation for that method and endpoint, use the highest numbered version
   available for that same method and endpoint.
3. For `/api/...`, use the highest numbered version available for that method and endpoint.
4. If no version implements that method and endpoint, return the normal `404 Not Found` response. Do not fall back to a
   different endpoint or HTTP method.

The newest version is selected per method and route template, not globally and not per resource family. Thus, if jobs
exist at v1 and v2 while candidates exist only at v1:

- `/api/v1/jobs` uses jobs v1.
- `/api/v2/jobs` uses jobs v2.
- `/api/v3/jobs` is served by jobs v2.
- `/api/v2/candidates` is served by candidates v1.
- `/api/candidates` is served by candidates v1.

If one method is unavailable at a version, resolve that method independently. For example, `GET /api/v2/jobs` may use v2
while `POST /api/v2/jobs` falls back to the newest version that implements POST for that route.

## Fallback transport behavior

Fallback is server-side route resolution (an internal forward/dispatch), not an externally visible HTTP `3xx` redirect.
The client receives the selected implementation's response directly at the URL it requested. This keeps POST bodies and
other method semantics intact and avoids requiring clients to follow redirects. The response must still identify which
API version produced its representation.

## Response version field

Every successful JSON response produced through these API routes must have a top-level `apiVersion` string containing
the version of the implementation/representation actually selected, formatted as `vN` (for example, `v1` or `v2`). This
is the served version, not necessarily the version requested in the URL.

Examples:

- `GET /api/v3/jobs`, resolved to jobs v2, returns the normal v2 response shape with `"apiVersion": "v2"`.
- `GET /api/candidates`, resolved to candidates v1, returns `"apiVersion": "v1"`.

If an endpoint normally returns a JSON object, add `apiVersion` to that object. If an endpoint returns a top-level JSON
array or scalar, wrap it in an object with `apiVersion` and a documented payload property; do not silently change an
established response shape without versioning that change. Do not add this field to non-JSON responses (for example,
file downloads or `204 No Content`). The version field is reserved: endpoint DTOs must not provide a conflicting value.
Error responses should use the common error format and may include the selected `apiVersion` when a version
implementation was resolved; routing failures that resolve no implementation need not claim an API version.

## Compatibility and documentation requirements

- Adding a new version for an endpoint must not require editing the version number of unrelated endpoints.
- An older version remains available as long as it is registered; adding a newer version does not replace or silently
  alter it.
- The versioned route, `/api` alias, and fallback route for a given endpoint must use the same authentication,
  authorization, validation, request-correlation, usage-recording, and error-handling policies.
- Do not implement fallback by redirecting a client to a guessed URL or by routing to an endpoint with a different route
  template.
- Document each endpoint's available versions, request/response differences, alias behavior, and fallback behavior in
  OpenAPI or accompanying API documentation. OpenAPI should identify the concrete versioned routes and explain that
  `/api/...` and unsupported-version fallback resolve to the highest available version for that endpoint.
- Version selection must be deterministic. If two implementations claim the same method, logical route, and version,
  fail application startup or architecture validation rather than selecting one arbitrarily.

## Implementation guidance for a future agent

Before coding, inventory the API route templates and decide how the framework will represent their available versions.
Implement version resolution centrally or through an explicit route registry so controllers do not each reimplement
fallback rules. Preserve normal Spring binding of path variables, query parameters, request bodies, validation,
security, and exception handling after resolution. Ensure route-template usage telemetry and metrics remain
low-cardinality and record the resolved route/version consistently without storing raw IDs or bodies.

Add tests for exact-version selection, per-endpoint independence, highest-version fallback, the `/api` alias, no
bare-path alias, missing methods/endpoints, path-variable preservation, JSON `apiVersion` decoration, and the absence of
an external redirect. Include the examples above as fixtures so a later API version cannot accidentally change unrelated
endpoint selection.

## Out of scope

This proposal does not prescribe a particular Spring library, dependency, controller organization, API versioning
mechanism, migration of current endpoints, deprecation schedule, or support policy for old versions. Choose the simplest
maintainable Spring-compatible implementation when this proposal is implemented, and update this document if a
deliberate contract change is needed.
