/*
 * Copyright 2020 Alexengrig Dev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.alexengrig.metter.processor;

import com.google.auto.service.AutoService;
import dev.alexengrig.metter.annotation.GetterSupplier;
import dev.alexengrig.metter.processor.element.GetterTypeElementVisitor;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static java.lang.String.format;

@AutoService(Processor.class)
public class GetterSupplierProcessor extends BaseProcessor {
    protected static final String CLASS_NAME_SUFFIX = "GetterSupplier";

    public GetterSupplierProcessor() {
        super(GetterSupplier.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotationTypeElement : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotationTypeElement);
            for (Element annotatedElement : annotatedElements) {
                GetterTypeElementVisitor typeVisitor = new GetterTypeElementVisitor();
                annotatedElement.accept(typeVisitor, null);
                Map<String, String> getterByField = typeVisitor.getMap();
                if (!getterByField.isEmpty()) {
                    String className = typeVisitor.getClassName();
                    String packageName = getPackageName(className);
                    String domainClassName = getSimpleName(className);
                    String sourceClassName = className + CLASS_NAME_SUFFIX;
                    JavaFileObject sourceFile = createSourceFile(sourceClassName);
                    try (PrintWriter sourcePrinter = new PrintWriter(sourceFile.openWriter())) {
                        if (packageName != null) {
                            sourcePrinter.printf("package %s;%n%n", packageName);
                        }
                        String source = generateSource(domainClassName, getterByField);
                        sourcePrinter.println(source);
                    } catch (IOException e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    }
                }
            }
        }
        return true;
    }

    private String getPackageName(String className) {
        int lastIndexOfDot = className.lastIndexOf('.');
        if (lastIndexOfDot > 0) {
            return className.substring(0, lastIndexOfDot);
        }
        return null;
    }

    private String getSimpleName(String className) {
        int lastIndexOfDot = className.lastIndexOf('.');
        return className.substring(lastIndexOfDot + 1);
    }

    private JavaFileObject createSourceFile(String className) {
        try {
            return processingEnv.getFiler().createSourceFile(className);
        } catch (IOException e) {
            throw new IllegalArgumentException(className);
        }
    }

    private String generateSource(String className, Map<String, String> field2Getter) {
        StringJoiner joiner = new StringJoiner("\n")
                .add("import javax.annotation.Generated;")
                .add("import java.util.HashMap;")
                .add("import java.util.Map;")
                .add("import java.util.function.Function;")
                .add("import java.util.function.Supplier;")
                .add("")
                .add(format("@Generated(value = \"%s\", date = \"%s\")",
                        getClass().getName(), LocalDateTime.now().toString()))
                .add(format("public class %s%s implements Supplier<Map<String, Function<%s, Object>>> {",
                        className, CLASS_NAME_SUFFIX, className))
                .add(format("    protected final Map<String, Function<%s, Object>> getterByField;",
                        className))
                .add("")
                .add(format("    public %s%s() {", className, CLASS_NAME_SUFFIX))
                .add("        this.getterByField = createMap();")
                .add("    }")
                .add("")
                .add(format("    protected Map<String, Function<%s, Object>> createMap() {",
                        className))
                .add(format("        Map<String, Function<%s, Object>> map = new HashMap<>(%d);",
                        className, field2Getter.size()));
        field2Getter.forEach((field, getter) -> joiner.add(format("        map.put(\"%s\", %s::%s);",
                field, className, getter)));
        return joiner
                .add("        return map;")
                .add("    }")
                .add("")
                .add("    @Override")
                .add(format("    public Map<String, Function<%s, Object>> get() {",
                        className))
                .add("        return getterByField;")
                .add("    }")
                .add("}")
                .add("")
                .toString();
    }
}