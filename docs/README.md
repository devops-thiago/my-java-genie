# docs/ — JDK documentation corpus

This directory holds the **local JDK 25 documentation corpus** used as the source
material for RAG ingestion. It is **not part of the repository** and is ignored
by git (`docs/` in `.gitignore`).

## What lives here

The directory mirrors the structure of the official JDK HTML documentation
bundle:

| Path      | Contents                                   |
| --------- | ------------------------------------------ |
| `api/`    | Java SE 25 API reference (javadoc HTML)    |
| `specs/`  | Java language / JVM specification pages    |
| `legal/`  | License and notices                        |
| `index.html` | Documentation index                    |

## Why it is not committed

- The bundle is ~326 MB of generated HTML — too large to commit without
  permanently bloating the repository and its history.
- The corpus is provisioned locally (e.g. on the lab VM) and mounted into the
  Docker stack read-only: `docker-compose.yml` mounts `./docs:/app/docs:ro`.
- Every contributor can provision the same corpus locally; see below.

## How to obtain the corpus

1. Download the JDK 25 HTML documentation bundle from the
   [Oracle Java SE Downloads](https://www.oracle.com/java/technologies/downloads/)
   page (the "**API docs**" / documentation archive for Java 25).
2. Extract it so that `api/`, `specs/`, `legal/` and `index.html` sit directly
   under `docs/` in this repository.
3. Ingest into the RAG system:

   ```bash
   # From the repository root
   curl -X POST "http://localhost:8080/api/ingest?path=docs"
   ```

   or ingest a single file:

   ```bash
   curl -X POST "http://localhost:8080/api/ingest?path=docs/specs/<file>.html"
   ```

> **Note:** In earlier iterations of this project, `docs/` also contained
> project guides (`CHAT_UI.md`, `OBSERVABILITY.md`, `TROUBLESHOOTING.md`,
> `DOCKER.md`). Those are no longer tracked here — keep corpus content and
> project documentation separate.
