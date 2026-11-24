# Workshop Registration

## Install the course JARs

Use JDK 25 and Apache Maven. Place the three instructor-provided files in the `lib` folder at the root of this repository:

- `model-2.0.jar`
- `logic-2.0.jar`
- `workshop-tester-2.0.jar`

Open PowerShell in the repository root (the folder containing the top-level `pom.xml`). Run these commands once per computer to install the JARs in Maven’s local repository:

```powershell
mvn install:install-file "-Dfile=lib/model-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=model" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"

mvn install:install-file "-Dfile=lib/logic-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=logic" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"

mvn install:install-file "-Dfile=lib/workshop-tester-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=workshop-tester" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
```
