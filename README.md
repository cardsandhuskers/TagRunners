# Tag Runners
## Dependencies
- [Teams Plugin](https://github.com/cardsandhuskers/TeamsPlugin)

## Compilation 

Download the teams plugin and set Dfile to the location of the jar file

```
mvn install:install-file -Dfile="TeamsPlugin.jar" -DgroupId=io.github.cardsandhuskers -DartifactId=Teams -Dversion=1.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
```

Once the neccessary project has been established, type:

```
mvn package
```
