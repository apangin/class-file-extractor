/*
 * Copyright 2022 Andrei Pangin
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

import com.sun.tools.attach.VirtualMachine;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ClassFileExtractor {

    public static void agentmain(String cmdline, Instrumentation inst) throws Exception {
        String[] args = cmdline.split(" ");
        String prefix = args.length > 1 ? args[1] : "";
        String slashPrefix = prefix.replace('.', '/');

        Class<?>[] classes = Arrays.stream(inst.getAllLoadedClasses())
                .filter(c -> c.getName().startsWith(prefix) && !c.isArray() && inst.isModifiableClass(c))
                .toArray(Class[]::new);

        Map<String, byte[]> classData = new ConcurrentHashMap<>();
        ClassFileTransformer extractor = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> cls, ProtectionDomain pd, byte[] classBytes) {
                if (className.startsWith(slashPrefix)) {
                    classData.put(className, classBytes);
                }
                return null;
            }
        };

        inst.addTransformer(extractor, true);
        try {
            inst.retransformClasses(classes);
        } finally {
            inst.removeTransformer(extractor);
        }

        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(args[0]))) {
            for (Map.Entry<String, byte[]> entry : classData.entrySet()) {
                jar.putNextEntry(new ZipEntry(entry.getKey() + ".class"));
                jar.write(entry.getValue());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String url = ClassFileExtractor.class.getProtectionDomain().getCodeSource().getLocation().toString();
        if (!url.startsWith("file:/") || !url.endsWith(".jar")) {
            System.out.println("Must be a JAR file");
            System.exit(1);
        }

        String jar = url.substring(6);
        if (args.length < 2) {
            System.out.println("Usage: java -jar " + jar + " <pid> <output.jar> [prefix]");
            System.exit(1);
        }

        VirtualMachine vm = VirtualMachine.attach(args[0]);
        String cmdline = args.length > 2 ? args[1] + ' ' + args[2] : args[1];
        try {
            vm.loadAgent(jar, cmdline);
        } finally {
            vm.detach();
        }

        System.out.println("Done");
    }
}
