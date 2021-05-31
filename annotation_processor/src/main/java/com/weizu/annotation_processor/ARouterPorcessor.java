package com.weizu.annotation_processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.weizu.annotation.ARouter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;


@AutoService(Process.class)
@SupportedAnnotationTypes({"com.weizu.annotation.ARouter"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"path"})
public class ARouterPorcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    // 找到注册的类的信息
    private Map<String, String> path = new HashMap<>();
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(annotations.isEmpty()){
            return false; // 标注注解处理器没有工作
        }

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ARouter.class);
        for(Element element: elements){
            // 获取所注册的类的包名
            PackageElement pkgElement = (PackageElement) element.getEnclosingElement();
            String packageName = pkgElement.getQualifiedName().toString();
            // 获取所注册类的类名
            String className = element.getSimpleName().toString();
            // 获取所注册的类的标识路由信息
            ARouter aRouter = element.getAnnotation(ARouter.class);
            path.put(aRouter.path(), packageName+"."+className);
        }
        try {
            TypeElement pathType = elementUtils.getTypeElement("com.weizu.api.ARouterPath");
            writeFile(pathType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void writeFile(TypeElement pathType) throws IOException {
        ParameterizedTypeName methodReturn = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(String.class)
        );
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getMaps")
                .addModifiers(Modifier.PUBLIC)
                .returns(methodReturn);
        builder.addStatement("$T<$T,$T> $N = new $T<>()",
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(String.class),
                "pathMap",
                ClassName.get(HashMap.class));
        for(String key: path.keySet()){
            builder.addStatement(
                    "$N.put($S, $S)",
                    "pathMap",
                    key,
                    path.get(key)
            );
        }

        builder.addStatement("return $N", "pathMap");
        String finalClassName = "ARouter$$map";
        JavaFile.builder("com.weizu.router",
                TypeSpec.classBuilder(finalClassName)
                        .addSuperinterface(ClassName.get(pathType))
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(builder.build())
                        .build())
                .build()
                .writeTo(filer);
    }
}