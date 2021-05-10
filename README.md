# Executable Thin Jar
## About
This allows building a thin jar executable for running a complete java application. When the jar executable is built, the artifactories and artifacts references are stored with the executable jar and not the artifacts themselves.

When the jar is executed, if the artifacts have not already been downloaded, the artifactories are searched as previously defined and they are download loaded to the system and made available. The system "java.class.path" is altered to reflect these changes.

The next time the jar is executed it loads the cached dependencies.

## Why
Historically, if you want a self contained java executable a jar file, the dependencies in order to run needed to be included in the jar file, this was coined a "fat jar"

Fat jars are problematic in the sense they duplicate data and can become very large, which can quickly consume resources on artifactories that host them. This project attempts to eliminate the need to upload executable jar files of significant size.

## Requirements
* Java 11
* Gradle 6.8.x

## Build
`gradle clean build`

## Usage
### Main Class Options
* `com.infernalbeast.lang.ThinMainClass`
* `--main-class`
The class which contains the static main method to proxy.
* `--class-path <CLASSPATH>`  
Additional dependencies.
* `--lazy-class-path <CLASSPATH>`  
Additional dependencies which are loaded on demand.

### Jar Options
* `--artifact-retry <INTEGER>`  
Set the number of times a failed pull from the artifactory is attempted. (default: 1)
* `--artifact-directory <FOLDER>`  
Set the location to store artifacts which are downloaded from the artifactories. (default: `${user.home}`/.artifacts)
* `--class-path <CLASSPATH>`  
Additional dependencies above those which have already been defined in the thin jar.
* `--lazy-class-path <CLASSPATH>`  
Additional dependencies which are loaded on demand above those which have already been defined in the thin jar.

```
java --add-opens java.base/java.lang=ALL-UNNAMED -jar myprogram.jar --class-path <CLASSPATH> --artifact-retry <INTEGER> --artifact-directory <FOLDER>
```

### Experimental
* `--module-path <MODULE_PATH>`  
Supply additional module dependencies above those which have already been defined in the thin jar.
* `--lazy-module-path <MODULE_PATH>`  
Supply additional module dependencies which are loaded on demand above those which have already been defined in the thin jar.
* `--add-exports`
$module/$package=$readingmodule exports $package of $module to $readingmodule
* `--add-opens`
$module/$package=$reflectingmodule opens $package of $module for deep reflection to $reflectingmodule
* `--add-reads`
$module=$targets adds readability edges from $module to all modules in the comma-separated list $targets

## Suppress Reflection Warnings
To suppress warnings regarding reflection use the following parameter
`--add-opens java.base/java.lang=ALL-UNNAMED`

## Gradle Plugin
The gradle plugin which can be used to generate executable jar files which are thin jar capable.

### Issues
While it is capable to adding dependencies into a module path dynamically, gradle does not yet seem to provide a clear list of modules which are added to the module path. Therefore, at the moment all dependencies are added to the classpath.

```
buildscript {
	dependencies {
		classpath 'com.infernalbeast:artifactory-classloader-gradle-plugin'
		// classpath files("libraries/artifactory-classloader-gradle-plugin.jar")
	}
}

plugins {
	id 'com.infernalbeast.artifactory-classloader'
}

artifactoryClassloader {
	// This enumerates the dependencies and stores it as a resource
	task thinClientLedger(type: ledgerTaskClass) {
		// Required: The source set to be modified
		sourceSet = sourceSets.main
		// Required: The configuration which to extract the dependencies
		configuration = configurations.runtimeClasspath
		// Optional: The default number of retries when downloading before failing
		retry = 5
		// Optional: The default user artifact directory
		artifactDirectory = ".myproject"
		// Optional: The credentials use use when accessing the artifactories
		credentials = [
			// Should match the name of the gradle repository being used
			MavenRepo: [
				// Get the credentials from various sources
				// If no value can be determined it will ask the user at runtime.
				username: [
					// System variable which defines the value
					environment: "MAVEN_USERNAME",
					// Java system property which defines the value
					system: "mavenUsername",
					// Static parameterless method which returns the value
					method: "com.test.Artifactory.username"
				],
				password: [
					environment: "MAVEN_PASSWORD",
					system: "mavenPassword",
					method: "com.test.Artifactory.password"
				]
			]
		]
	}
	// This configures the META-INF\MANIFEST.MF
	task thinClient(type: artifactTaskClass) {
		dependsOn thinClientLedger
		// Required: The task which generates the executable jar
		jar = tasks.findByName("jar")
		// Optional: How artifacts are downloaded (Defaults to true)
		lazy = false
	}
}
```
