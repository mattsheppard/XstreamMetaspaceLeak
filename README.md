# XstreamMetaspaceLeak

Demonstration project for reporting a case where Xstream seems to cause a JVM metaspace leak (based off https://github.com/mattsheppard/JacksonDatabindMetaspaceLeak)

Try running it with...

```
> mvn clean dependency:copy-dependencies package
> java -XX:MaxMetaspaceSize=100m -cp "target/dependency/*:target/jackson-classloader-leaker-0.0.1-SNAPSHOT.jar" com.kstruct.LeakMetaspaceViaXstream
```

On my machine it runs happily for a few minutes, then fails.

I'll add a link once I've cleaned this up, explored a bit and (if I'm not doing somehting wrong, reported it).