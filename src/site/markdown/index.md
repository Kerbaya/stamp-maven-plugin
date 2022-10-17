# Stamp Maven Plugin
Used to prepare a SNAPSHOT-versioned Maven project for building as a self-aware SNAPSHOT version deployment

## Goals Overview
* [stamp:stamp](./stamp-mojo.html) Update the project version

## Usage
`mvn com.kerbaya:stamp-maven-plugin:1.0.0:stamp`

Updates project version in `pom.xml`:

```xml
<groupId>com.myorg</groupId>
<artifactId>myapp</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

To:

```xml
<groupId>com.myorg</groupId>
<artifactId>myapp</artifactId>
<version>1.0.0-20221017.021433-12</version>
```
