# XstreamMetaspaceLeak

Demonstration project for reporting a case where Xstream seems to cause a JVM metaspace leak

Try running it with...

```
> mvn clean dependency:copy-dependencies package
> java -XX:MaxMetaspaceSize=100m -cp "target/dependency/*:target/xstream-classloader-leaker-0.0.1-SNAPSHOT.jar" com.kstruct.LeakMetaspaceViaXstream
```

On my machine it runs happily for a few minutes, then fails when it hits the metaspace limit.

If I set -DclearReflectionFieldDictionary=true to enable my hack to flush
the cache in PureJavaReflectionProvider.fieldDictionary it seems to run
indefinitely.

Raised as https://github.com/x-stream/xstream/issues/107