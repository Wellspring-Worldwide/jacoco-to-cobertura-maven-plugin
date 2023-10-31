# JacocoToCobertura Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/com.example/jacoco-to-cobertura-maven-plugin)](https://search.maven.org/artifact/com.example/jacoco-to-cobertura-maven-plugin)

This Maven plugin allows you to convert JaCoCo XML reports to Cobertura reports. The Cobertura report can be used in GitLab to display code coverage information in Merge Requests. This project is based on the original [Kotlin Gradle plugin](https://github.com/razvn/jacoco-to-cobertura-gradle-plugin/tree/main) and has been converted to Maven based on Java 11. 

## How to Use the Plugin

### Add the Plugin to Your Maven Project

To use this plugin, add it to your Maven project's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.tomassatka</groupId>
            <artifactId>jacoco-to-cobertura-maven-plugin</artifactId>
            <version><!-- see latest version above --></version>
            <executions>
                <execution>
                    <goals>
                        <goal>jacocoToCobertura</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Configure the Plugin

| Property            | Description | Default Value |
|---------------------|---|--|
| `inputFile`         | JaCoCo XML file to read | XML report of single `JacocoReport` task found in the project; must be specified manually if zero or more than one `JacocoReport` task exists |
| `outputFile`        | Cobertura XML file to generate | `cobertura-${inputFile.nameWithoutExtension}.xml` in the directory of `inputFile` |
| `sourceDirectories` | Directories containing source files the JaCoCo report used | Source directories of single `JacocoReport` task found in the project; must be specified manually if zero or more than one `JacocoReport` tasks exist |
| `splitByPackage`    | Whether to generate one Cobertura report per package | `false` |

Example configuration:
```xml
<configuration>
    <inputFile>path/to/jacoco-report.xml</inputFile>
    <outputFile>path/to/output/cobertura.xml</outputFile>
    <sourceDirectories>
        <sourceDirectory>src/main/java</sourceDirectory>
    </sourceDirectories>
    <splitByPackage>true</splitByPackage>
</configuration>
```
### Run the Plugin
Run the plugin's convert goal:
```
mvn jacoco-to-cobertura:convert
```
This will convert the JaCoCo report to a Cobertura report. You can configure the plugin to run automatically as part of your build lifecycle.

