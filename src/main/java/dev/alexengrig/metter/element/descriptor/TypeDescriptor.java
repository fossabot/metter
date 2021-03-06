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

package dev.alexengrig.metter.element.descriptor;


import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeDescriptor {
    protected final TypeElement typeElement;
    protected transient String qualifiedName;
    protected transient String simpleName;
    protected transient Set<FieldDescriptor> fields;
    protected transient Set<MethodDescriptor> methods;
    protected transient Set<AnnotationDescriptor> annotations;
    protected transient Map<String, Boolean> hasMethodByNameMap;
    protected transient Map<String, Boolean> hasAnnotationByQualifiedNameMap;

    public TypeDescriptor(TypeElement typeElement) {
        this.typeElement = typeElement;
    }

    public String getQualifiedName() {
        if (qualifiedName == null) {
            qualifiedName = typeElement.getQualifiedName().toString();
        }
        return qualifiedName;
    }

    public String getSimpleName() {
        if (simpleName == null) {
            simpleName = typeElement.getSimpleName().toString();
        }
        return simpleName;
    }

    public Set<FieldDescriptor> getFields() {
        if (fields == null) {
            fields = FieldDescriptor.of(typeElement);
        }
        return fields;
    }

    public Set<MethodDescriptor> getMethods() {
        if (methods == null) {
            methods = MethodDescriptor.of(typeElement);
        }
        return methods;
    }

    public boolean hasMethod(String methodName) {
        if (hasMethodByNameMap == null) {
            hasMethodByNameMap = new HashMap<>();
        } else if (hasMethodByNameMap.containsKey(methodName)) {
            return hasMethodByNameMap.get(methodName);
        }
        boolean hasMethod = getMethods().stream()
                .map(MethodDescriptor::getName)
                .anyMatch(methodName::equals);
        hasMethodByNameMap.put(methodName, hasMethod);
        return hasMethod;
    }

    public Set<AnnotationDescriptor> getAnnotations() {
        if (annotations == null) {
            annotations = AnnotationDescriptor.of(typeElement);
        }
        return annotations;
    }

    public boolean hasAnnotation(String annotationQualifiedName) {
        if (hasAnnotationByQualifiedNameMap == null) {
            hasAnnotationByQualifiedNameMap = new HashMap<>();
        } else if (hasAnnotationByQualifiedNameMap.containsKey(annotationQualifiedName)) {
            return hasAnnotationByQualifiedNameMap.get(annotationQualifiedName);
        }
        boolean hasAnnotation = getAnnotations().stream()
                .map(AnnotationDescriptor::getQualifiedName)
                .anyMatch(annotationQualifiedName::equals);
        hasAnnotationByQualifiedNameMap.put(annotationQualifiedName, hasAnnotation);
        return hasAnnotation;
    }

    public <T extends Annotation> T getAnnotation(Class<? extends T> annotationType) {
        return typeElement.getAnnotation(annotationType);
    }
}
