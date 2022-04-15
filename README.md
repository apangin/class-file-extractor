# Class File Extractor
Extract loaded classes from a running JVM application

### Download

 - [extractor.jar](https://github.com/apangin/class-file-extractor/releases/download/v1.0/extractor.jar)

### Usage
```
java -jar extractor.jar <pid> <output.jar> [prefix]
```

 - `<pid>` - process ID of the target JVM.
 - `<output.jar>` - output file name (where to store the extracted class files).
 - `[prefix]` - optional class name prefix (e.g. to extract classes only in a selected package).
