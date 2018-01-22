# JacksonDatabindMetaspaceLeak

Demonstration project for reporting a case where Jackson seems to cause a JVM metaspace leak

Try running it with...

```
> mvn clean dependency:copy-dependencies package
> java -XX:MaxMetaspaceSize=100m -cp "target/dependency/*:target/jackson-classolader-leaker-0.0.1-SNAPSHOT.jar" com.kstruct.LeakMetaspaceViaJackson
```

On my machine it runs happily for a few minutes, then fails.

The are a couple of things which improve the situation...

```
if (Boolean.getBoolean("clearTypeFactoryCache")) {
    // Note that we haven't kept any reference to any groovyClasses or
    // groovyObjects, so they should be collectable in both the heap
    // and meta-space.
    
    // https://github.com/FasterXML/jackson-databind/issues/489
    // suggests we may be expected to call this if we're dynamically
    // creating types
    mapper.getTypeFactory().clearCache();
}

if (Boolean.getBoolean("flushCachedSerializers")) {
    // I've found that clearing this helps a bit (but doesn't totally fix the problem)
    // System.out.println(((DefaultSerializerProvider) mapper.getSerializerProvider()).cachedSerializersCount());
    ((DefaultSerializerProvider) mapper.getSerializerProvider()).flushCachedSerializers();
}
```

You can turn them on on the command line with `-DclearTypeFactoryCache=true -DflushCachedSerializers=true`

Raised with jackson-databind as https://github.com/FasterXML/jackson-databind/issues/1905
