# urplans — core (urplans-dbcore)

---
* Language: Java (tested with Java 21)
* Build tool: Maven
* Persistence: Spring Data JPA + Hibernate, H2 (file-backed by default)
* CLI entrypoint: implemented with a Spring runner (reads *non-option* args)
* Jar output: `target/urplans-1.0-SNAPSHOT.jar`

---

## Build

From the repository root:

```bash
mvn -DskipTests clean package
# output -> target/urplans-1.0-SNAPSHOT.jar
```

---

## CLI usage (how to communicate with the core)

The CLI runner reads the program's **non-option** arguments (arguments that are not Spring properties). Example commands:

```
urplans-dbcore CLI:
 -i "Title|Description|2025-08-13>FOREVER|PRIORITY"  insert
 -s 2025-08-13   show tasks for date
 -l              list all
 -d <id>         delete
 --search title date priority page size
```

> [!NOTE]
> Date format: YYYY-MM-DD
> 
> Priority must be one of: [URGENT_IMPORTANT, NOT_URGENT_IMPORTANT, URGENT_NOT_IMPORTANT, NOT_URGENT_NOT_IMPORTANT]
> 
> To run as non-web CLI generally use: -Dspring.main.web-application-type=none

### Examples

Insert a task (run non-web):

```bash
java -jar target/urplans-1.0-SNAPSHOT.jar \
  -i "Study Java|Study to get prepared|2025-08-13>FOREVER|URGENT_IMPORTANT"
```

Show tasks for a date:

```bash
java -jar target/urplans-1.0-SNAPSHOT.jar -s 2025-08-13
```

List all tasks (omit the `-D` if you set `spring.main.web-application-type=none` in `application.properties`):

```bash
java -jar target/urplans-1.0-SNAPSHOT.jar -l
```

Delete by id:

```bash
java -jar target/urplans-1.0-SNAPSHOT.jar -d 42
```

Search (title/date/priority/page/size):

```bash
java -jar target/urplans-1.0-SNAPSHOT.jar --search "java" 2025-08-13 URGENT_IMPORTANT 0 20
```
