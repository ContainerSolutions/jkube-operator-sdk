package io.javaoperatorsdk.operator.processing;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.javaoperatorsdk.operator.ControllerUtils.CONTROLLERS_RESOURCE_PATH;

@SupportedAnnotationTypes(
        "io.javaoperatorsdk.operator.api.Controller")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class ControllerAnnotationProcessor extends AbstractProcessor {
    private FileObject resource;
    PrintWriter printWriter = null;
    private Set<String> generatedDoneableClassFiles = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", CONTROLLERS_RESOURCE_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            printWriter = new PrintWriter(resource.openOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                Set<? extends Element> annotatedElements
                        = roundEnv.getElementsAnnotatedWith(annotation);
                annotatedElements.stream().filter(element -> element.getKind().equals(ElementKind.CLASS))
                        .map(e -> (TypeElement) e)
                        .forEach(e -> this.generateDoneableClass(e, printWriter));
            }
        } finally {
            printWriter.close();
        }
        return true;
    }

    private void generateDoneableClass(TypeElement controllerClassSymbol, PrintWriter printWriter) {
        try {
            final TypeMirror resourceType = findResourceType(controllerClassSymbol);

            TypeElement customerResourceTypeElement = processingEnv
                    .getElementUtils()
                    .getTypeElement(resourceType.toString());

            final String doneableClassName = customerResourceTypeElement.getSimpleName() + "Doneable";
            final String destinationClassFileName = customerResourceTypeElement.getQualifiedName() + "Doneable";
            final TypeName customResourceType = TypeName.get(resourceType);

            if (!generatedDoneableClassFiles.add(destinationClassFileName)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        String.format(
                                "%s already exist! adding the mapping to the %s",
                                destinationClassFileName,
                                CONTROLLERS_RESOURCE_PATH)
                );
                printWriter.println(controllerClassSymbol.getQualifiedName() + "," + customResourceType.toString());
                return;
            }
            JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(destinationClassFileName);

            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                printWriter.println(controllerClassSymbol.getQualifiedName() + "," + customResourceType.toString());
                final MethodSpec constructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(customResourceType, "resource")
                        .addParameter(Function.class, "function")
                        .addStatement("super(resource,function)")
                        .build();


                final TypeSpec typeSpec = TypeSpec.classBuilder(doneableClassName)
                        .superclass(ParameterizedTypeName.get(ClassName.get(CustomResourceDoneable.class), customResourceType))
                        .addAnnotation(RegisterForReflection.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(constructor)
                        .build();

                final PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(customerResourceTypeElement);
                JavaFile file = JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec)
                        .build();
                file.writeTo(out);
            }
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }
    }

    private TypeMirror findResourceType(TypeElement controllerClassSymbol) throws Exception {
        try {
            final DeclaredType controllerType = collectAllInterfaces(controllerClassSymbol)
                    .stream()
                    .filter(i -> i.toString()
                            .startsWith(ResourceController.class.getCanonicalName())
                    )
                    .findFirst()
                    .orElseThrow(() -> new Exception("ResourceController is not implemented by " + controllerClassSymbol.toString()));

            return controllerType.getTypeArguments().get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<DeclaredType> collectAllInterfaces(TypeElement element) {
        try {
            List<DeclaredType> interfaces = new ArrayList<>(element.getInterfaces()).stream().map(t -> (DeclaredType) t).collect(Collectors.toList());
            TypeElement superclass = ((TypeElement) ((DeclaredType) element.getSuperclass()).asElement());
            while (superclass.getSuperclass().getKind() != TypeKind.NONE) {
                interfaces.addAll(superclass.getInterfaces().stream().map(t -> (DeclaredType) t).collect(Collectors.toList()));
                superclass = ((TypeElement) ((DeclaredType) superclass.getSuperclass()).asElement());
            }
            return interfaces;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
