<!-- Copyright (c) 2026 Ashutosh Sinha. Proprietary. All rights reserved. -->
# server-demo: the standalone Rachana server

The standalone server (`java/rachana-server`) in one folder: one Sutra page (`project/sutras/overview.sutra.yaml`), two tables read
from files (`project/data/sales.csv`, `project/data/regions.json`, the sales file re-read every 5 seconds), and sign-in with a users
file. One JVM process, one configuration file (`rachana-server.yaml`: port 8090, loopback only, `auth.mode: users`). All data is
synthetic (fictional regions, products and figures). Guide: [The Rachana server](../../docs/IN_YOUR_APP.md#the-rachana-server).

**Development only:** the user `dev` with the password `dev-only-pass` is written in `users.yaml` so the example runs. It protects
nothing; never reuse it.

## Prerequisites

- Java 21 (JDK, to build once; a JRE 21 is enough to run the built jar). Check: `java -version`.
- The Maven wrapper in `java/` (no separate Maven install) and access to a Maven repository for the first build (the build downloads
  its dependencies; on a machine without access, use an offline repository you have prepared).
- No Python and no Node are needed. The browser runtime bundle comes from `python/src/drishti_rachana/bundle/` (committed).

## Build the jar (once)

Windows (cmd or PowerShell):

```
cd drishti-platform\drishti-rachana\java
mvnw.cmd clean package -DskipTests -pl rachana-server -am
```

Linux and macOS:

```
cd drishti-platform/drishti-rachana/java
./mvnw clean package -DskipTests -pl rachana-server -am
```

This writes `java\rachana-server\target\rachana-server-1.1.1-all.jar` (the version is the one in `java/pom.xml`).

## Run

From `drishti-platform\drishti-rachana` (Linux and macOS: forward slashes):

```
java -jar java\rachana-server\target\rachana-server-1.1.1-all.jar check --config examples\server-demo\rachana-server.yaml
java -jar java\rachana-server\target\rachana-server-1.1.1-all.jar serve --config examples\server-demo\rachana-server.yaml
```

`check` prints the resolved settings and any problem, and starts nothing. `serve` prints `rachana-server listening on
http://127.0.0.1:8090/`. Open <http://127.0.0.1:8090/>, sign in as `dev` with `dev-only-pass`, then click **overview**. Add `--port 8091` to `serve` when 8090
is taken (`--port 0` takes any free port).

## What you should see

The sign-in page, a list of one page (`overview`), then that page: tiles, bars and a table over the sales file, and a region lookup. Edit a number in
`project\data\sales.csv` and save: within about 5 seconds the page shows the new figure without a reload. Without a browser:
`curl -i http://127.0.0.1:8090/` answers `303` to the sign-in page.

## Stop

Press Ctrl+C in the window that runs it, or end the `java` process by its id.

Other tools on the same folder: `java -jar ...-all.jar hash-password` (type a password, get a hash for `users.yaml`) and Rachana Studio
(`node js\packages\rachana-cli\bin\rachana.js studio examples\server-demo\project\sutras --data examples\server-demo\project\data --port 4500`,
Node 20).

## Troubleshooting

| You see | Cause and fix |
|---|---|
| `'java' is not recognised` or `UnsupportedClassVersionError` | Java 21 is not installed or is not the first `java` on the PATH: install a JDK 21 and open a new window |
| `Unable to access jarfile ...-all.jar` | the jar is not built, or its version differs: run the build step and use the file name it wrote (`dir java\rachana-server\target\*-all.jar`) |
| the build cannot download dependencies | no Maven repository access: use a network that has it, or point `-Dmaven.repo.local=` at a prepared repository |
| `Address already in use` / `BindException` | port 8090 is taken: add `--port 8091` to `serve` |
| `check` prints a problem about a path | run the command from `drishti-platform\drishti-rachana`, or give `--config` an absolute path |
| sign-in is refused | the user is `dev`, the password `dev-only-pass`; a changed `users.yaml` needs a restart |

Other examples: [examples index](../README.md).
