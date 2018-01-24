package com.kstruct;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.mapper.AnnotationConfiguration;
import com.thoughtworks.xstream.mapper.AnnotationMapper;

import groovy.util.GroovyScriptEngine;

public class LeakMetaspaceViaXstream {

    private static final String GROOVY_SCRIPT_NAME_PREFIX = "Script";
    private static final int NUM_GROOVY_SCRIPTS = 1000;

    public static XStream xstream = new XStream();
    
    public static void main(String[] args) throws Exception {
        leak();
    }
    
    public static void leak() throws Exception {
        File groovyClassDir = new File("target/generated_groovy_classes");
        groovyClassDir.mkdirs();
        
        regenerateGroovyScripts(groovyClassDir);
        
        GroovyScriptEngine engine = new GroovyScriptEngine(groovyClassDir.getAbsolutePath());

        // autodetectAnnotations(true) causes a second leak inside AnnotationMapper.annotatedTypes.
        // AnnotationConfiguration is deprecated though, so maybe this feature is going away.
        // 
        // It is, however, the only option we have (as far as I know)
        // for using @XStreamOmitField on a class we may not know about in advance.
        xstream.autodetectAnnotations(true);
        
        while(true) {
            regenerateGroovyScripts(groovyClassDir);

            for (int i = 0; i < NUM_GROOVY_SCRIPTS; i++) {
                String groovyScriptName = GROOVY_SCRIPT_NAME_PREFIX + i + ".groovy";

                Class groovyClass = engine.loadScriptByName(groovyScriptName);

                Object groovyObject = groovyClass.newInstance();
                
                String xml = xstream.toXML(groovyObject);
                System.out.println(xml);
            }
            
            if (Boolean.getBoolean("clearReflectionFieldDictionary")) {
                SunUnsafeReflectionProvider reflectionProvider = (SunUnsafeReflectionProvider) xstream.getReflectionProvider();
                Field fieldDictionaryField = PureJavaReflectionProvider.class.getDeclaredField("fieldDictionary");
                fieldDictionaryField.setAccessible(true);
                FieldDictionary fieldDictionary = (FieldDictionary) fieldDictionaryField.get(reflectionProvider);
                fieldDictionary.flushCache();
            }
            
            if (Boolean.getBoolean("clearAnnotatedTypes")) {
                Field annotationConfigurationField = XStream.class.getDeclaredField("annotationConfiguration");
                annotationConfigurationField.setAccessible(true);
                AnnotationMapper annotationMapper = (AnnotationMapper) annotationConfigurationField.get(xstream);

                Field annotatedTypesField = AnnotationMapper.class.getDeclaredField("annotatedTypes");
                annotatedTypesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Set<Class<?>> annotatedTypes = (Set<Class<?>>) annotatedTypesField.get(annotationMapper);
                
                Set<Class<?>> initialSet = new HashSet<Class<?>>();
                initialSet.add(Object.class);
                annotatedTypes.retainAll(initialSet);
            }
        }
    }

    /**
     * Generate 1000 groovy script files which GroovyScriptEngine will reload (because the
     * modification time changed).
     */
    private static void regenerateGroovyScripts(File groovyClassDir) throws IOException {
        for (int i = 0; i < NUM_GROOVY_SCRIPTS; i++) {
            File groovyScriptFile = new File(groovyClassDir, GROOVY_SCRIPT_NAME_PREFIX + i + ".groovy");
            byte[] groovyScriptContent = ("import com.thoughtworks.xstream.annotations.XStreamOmitField;\n"
            + "public class Foo {\n"
            + "    public String name = \"NUMBER-" + i + "\";"
            + "    @XStreamOmitField public String ignore = \"SHOULD_NOT_BE_SHOWN\";"
            + " }").getBytes();
            Files.write(groovyScriptFile.toPath(), groovyScriptContent);
        }
    }
}
