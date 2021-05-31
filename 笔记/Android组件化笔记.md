1. Android组件化实战（一）

## 为什么需要组件化开发？

以往开发中，将各大业务逻辑都按照包名来分放，也就是全都放置在app模块下，这样做会存在下面几个问题：

- 随着项目的增大，管理和理解不便；
- 每次在修改某个小功能的时候，都需要build整个项目；
- 在团队开发中，整合代码时候容易出现命名冲突问题，且整合基于包管理也比较麻烦；
- 

在组件化项目中，我们通常使用三层逻辑架构，即：app壳工程、各大业务模块和公共依赖库。

当App需要新增一个业务模块的时候，直接添加一个独立的业务模块即可。就不需要阅读整个工程代码，然后在合适的地方添加对应的逻辑。使得开发变得容易，新增功能的成本降低，且代码便于维护和修改。

> 以前使用一个模块来开发存在“你中有我，我中有你的现象”，不利于项目的维护，代码耦合性很高，不利于团队协作开发，也不利于新的功能的添加。

## 如何组件化？

首先了解下这三个部分的功能：

- app壳工程：负责管理各个业务组件和打包apk，没有具体的业务功能；

- 各大业务模块：根据具体的业务而独立形成的模块；
- 公共依赖库：功能组件，提供大多数组件所需的基础功能；

各大业务模块在集成模式下可以打包为aar包，其类型是library，集成到壳子工程中，构成一个完整的APP。在开发模式下是一个独立的App，其类型是application，可以独立开发和调试。

由于各大业务模块之间相互独立，没有关联，所以在处理业务逻辑的时候，需要一个“路由”中转站。常用的为ARouter。

由于在每个模块中都有AndroidManifest配置文件，所以我们需要合并；

同理对于配置文件，我们也需要合并；

创建下面的工程，添加两个模块：

<img src="imgs/屏幕截图 2021-05-31 082703.png" style="zoom:60%;" />

然后我们查看`settings.gradle`，内容如下：

```gradle
include ':order'
include ':common'
include ':app'
rootProject.name = "My Application"
```

也就是说模块的引入使用的是`include`

注意到在`Gradle`脚本中有很多`build.gradle`脚本，由于其内容大多数一样的，这里我们需要抽取出一个公共的配置脚本。

我们在每个脚本中打印一句话，表明这个`gradle`，如`order`模块：

```
plugins {
    id 'com.android.application'
}

println "order -> build.gradle"
```

在`Build`面板可以看见如下的内容：

<img src="imgs/屏幕截图 2021-05-31 083611.png" style="zoom:60%;" />

也就是对于`gradle`文件，其加载顺序是`settings`->`Project`->`app`->其他模块的`gradle`文件。

因为其加载的顺序，故而可以构建一个公共的`gradle`文件来抽取公共配置。

我们切换工程视图到`Project`，然后新建一个`app_config.gradle`文件，如下：

<img src="imgs/屏幕截图 2021-05-31 084934.png" style="zoom:60%;" />

在该文件中就输出一句话：

```
println "app_config.gradle"
```

然后，项目的`build.gradle`文件中引入，如：

```
println "project -> build.gradle"
apply from: 'app_config.gradle'
```

然后同步一下可以看见：

<img src="imgs/屏幕截图 2021-05-31 085155.png" style="zoom:60%;" />

然后，我们尝试将公共配置抽取放置到`app_config.gradle`文件中：

```
ext{
    config_android=[
            compileSdkVersion: 30,
            buildToolsVersion : "30.0.2",
            minSdkVersion : 16,
            targetSdkVersion : 30,
            JavaVersion : JavaVersion.VERSION_1_8
    ]
}
```

值得注意的是，不能在ext最外部写`compileSdkVersion`这种，因为`compileSdkVersion`在这些`build.gradle`中为变量名，可以改个名字。

然后可以替换，如这里的`build.gradle(:app)`文件：

```
plugins {
    id 'com.android.application'
}

android {
    compileSdkVersion config_android.compileSdkVersion
    buildToolsVersion config_android.buildToolsVersion

    defaultConfig {
        applicationId "com.weizu.myapplication"
        minSdkVersion config_android.minSdkVersion
        targetSdkVersion config_android.targetSdkVersion
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility config_android.JavaVersion
        targetCompatibility config_android.JavaVersion
    }
}

dependencies {

    implementation 'androidx.appcompat:appcompat:1.1.0'
    implementation 'com.google.android.material:material:1.1.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.+'
    androidTestImplementation 'androidx.test.ext:junit:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'
}
```

其他文件同理替换即可。注：在项目中的`gradle.properties`文件，是项目配置文件，在任何一个`build.gradle`文件中都可以将其中定义的常量读取出来，其使用方式和在`app_config.gradle`定义的变量的使用方式类似。

对于在工程中定义的其他模块，我们需要引入公共模块：

```
implementation project(":common")
```

但是我们在开发项目的时候会依赖很多开源库，而这些库每个组件都需要用到，要是每个组件都去依赖一遍也是很麻烦的，尤其是给这些库升级的时候，为了方便我们统一管理第三方库，我们将给给整个工程提供统一的依赖第三方库的入口，前面介绍的`common`库的作用之一就是统一依赖开源库，因为其他业务组件都依赖了`common`库，所以这些业务组件也就间接依赖了`common`所依赖的开源库。

因为在开发阶段我们需要各大模块以`application`的形式独立存在，在发布阶段以`library`的形式打包存在。故而需要设置一个开关，标识是开发模式还是发布模式。

在`app_config.gradle`中，添加：

```
is_release = true
```

此时的`order`模块中，我们修改了两处：

```
if(is_release){
    apply plugin: 'com.android.library' // 集成化模式
}else {
    apply plugin: 'com.android.application' // 组件化状态
}

android {
    compileSdkVersion config_android.compileSdkVersion
    buildToolsVersion config_android.buildToolsVersion

    defaultConfig {
        if(!is_release){
            applicationId "com.weizu.order"
        }
        minSdkVersion config_android.minSdkVersion
        targetSdkVersion config_android.targetSdkVersion
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility config_android.JavaVersion
        targetCompatibility config_android.JavaVersion
    }
}

dependencies {

    implementation 'androidx.appcompat:appcompat:1.1.0'
    implementation 'com.google.android.material:material:1.1.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.+'
    androidTestImplementation 'androidx.test.ext:junit:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'

    implementation project(":common")
}
```

使用`apply plugin`来申明其类型，且组件化状态才需要`applicationId`。

同理，在发布模式下，我们需要引入定义的各大模块，也就是在`build.gradle(:app)`中需要使用类似的配置：

```
if(is_release){
	implementation project(":order")
}
```

由于这里我只有一个额外的模块，所以这里就写一个就行。

然后我们对这个项目`build`打包`apk`，然后分析下这个包，如图：

<img src="imgs/屏幕截图 2021-05-31 093958.png" style="zoom:60%;" />

可以看见这里的`order`模块和对应的`common`模块在发布模式下，被打包到了`apk`文件中。

对于`AndroidManifest`配置文件，我们也可以定义对应的两种模式。首先我们观察下我们直接定义的`common`和`order`两个模块，因为在新建的时候分别选中的是`Android Library`和`Phone & Tablet Module`所以在`order`的`AndroidManifest`文件中有启动配置，如：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.weizu.order">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

而，启动部分的配置在发布阶段是不需要的，所以这里需要配置两个版本的`AndroidManifest`文件。

不妨新建一个文件用来存放开发阶段需要加载的`AndroidManifest`文件，也就是现在项目中的`AndroidManifest.xml`配置文件，如下图：

<img src="imgs/屏幕截图 2021-05-31 100305.png" style="zoom:60%;" />

对于外层的`AndroidManifest.xml`配置文件，删减，保留如下：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.weizu.order">

    <application>
        <activity android:name=".MainActivity"/>
    </application>

</manifest>
```

然后，我们需要配置在不同时刻加载对应的配置文件。

我们需要使用源集`sourceSets`，在对应的模块的`gradle`文件中进行配置。对应的配置文件此时为：

```gradle
if(is_release){
    apply plugin: 'com.android.library' // 集成化模式
}else {
    apply plugin: 'com.android.application' // 组件化状态
}

android {
    compileSdkVersion config_android.compileSdkVersion
    buildToolsVersion config_android.buildToolsVersion

    defaultConfig {
        if(!is_release){
            applicationId "com.weizu.order"
        }
        minSdkVersion config_android.minSdkVersion
        targetSdkVersion config_android.targetSdkVersion
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility config_android.JavaVersion
        targetCompatibility config_android.JavaVersion
    }

    // 源集 —— 用来设置Java目录或者资源目录
    sourceSets {
        main {
            if(!is_release){
                // 如果是组件化模式，需要单独运行时
                manifest.srcFile 'src/main/debug/AndroidManifest.xml'
            }else {
                // 集成化模式，整个项目打包
                manifest.srcFile 'src/main/AndroidManifest.xml'
                java {
                    // release 时 debug 目录下文件不需要合并到主工程
                    exclude '**/debug/**'
                }
            }
        }
    }
}

dependencies {

    implementation 'androidx.appcompat:appcompat:1.1.0'
    implementation 'com.google.android.material:material:1.1.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.+'
    androidTestImplementation 'androidx.test.ext:junit:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'

    implementation project(":common")
}
```

注意组件化工程的文件命名与打印日志规范：

> order -> OrderMainActivity、order_main_activity.xml、order/OrderMainActivity/onCreate..

对应视频地址：[Android组件化实战（一）_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1Ar4y1A7kh)

对应github地址：[zouchanglin/study_module: Android组件化学习示例代码 (github.com)](https://github.com/zouchanglin/study_module)

参考博客：[一篇文章搞懂Android组件化 - 简书 (jianshu.com)](https://www.jianshu.com/p/8b6e6a50e21e)

# 2. Android组件化实战（二）

通过上篇我们已经知道了如何定义一个模块，并抽取出公共的配置，如何设置开关控制是开发模式还是发布模式，如何加载对应的配置文件等。对于单个模块这里基本上已经没什么问题了。但是在实际开发中，我们往往在项目中需要多个组件模块，组件之间调用和通信问题就急需解决。

前面提到过，组件之间的通信需要使用“路由”来解决。对于“路由”的解决方案这里有两种比较好的实现：

- [ActivityRouter](https://github.com/mzule/ActivityRouter)
- [ARouter](https://github.com/alibaba/ARouter)

这里不先直接使用这两个现有的框架。跟着视频来尝试理解这个实现过程。

首先我们需要了解`Annotation Processing Tool`，即注解处理器，简称为APT。

>APT是一种处理注解的工具，它对源代码文件进行检测找出其中的Annotation，使用Annotation进行额外的处理。 Annotation处理器在处理Annotation时可以根据源文件中的Annotation生成额外的源文件和其它的文件(文件具体内容由Annotation处理器的编写者决定)，APT还会编译生成的源文件和原来的源文件，将它们一起生成class文件。
>————————————————
>原文链接：https://blog.csdn.net/guiying712/article/details/55213884

也就是根据解析规则，处理注解，生成跳转和通信所需的代码。

那么，这里我们来尝试实现一个简单的注解处理器的模块，新建一个模块`annotation`，使用`Java or Kotlin Library`。

对于这个`annotation`模块，我们同样的设置其`gradle`文件：

```gradle
plugins {
    id 'java-library'
}

// 控制台中文设置UTF-8
tasks.withType(JavaCompile){
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = config_android.JavaVersion
    targetCompatibility = config_android.JavaVersion
}
```

为什么需要使用指定控制台为UTF-8编码呢？

作者说因为注解是配置在运行时生效，在window机器上会存在问题。所以需要使用utf-8编码。

然后定义个注解：

<img src="imgs/屏幕截图 2021-05-31 105946.png" style="zoom:60%;" />

然后，我们在`app`的`build.gradle`文件中引入这个模块：

```gradle
implementation project(':annotation')
```

在此之前，我们需要先熟悉`java`注解，这个可以参考我之前的博客：[Java注解]([java注解_庆述倾述-CSDN博客](https://blog.csdn.net/qq_26460841/article/details/115378442))

然后自定义该注解，如下：

```java
package com.weizu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ARouter {
    String path();
}
```

对于自定义注解，我们还需要一个注解处理器类，来处理这个注解。这里定义一个注解处理器的模块。这里还是选择`Java or Kotlin Library`，创建一个`annotation_processor`模块。

定义一个注解处理器类，这个类需要继承`AbstractProcessor`：

```java
public class ARouterProcessor extends AbstractProcessor {}
```

对于`AbstractProcessor`类，可以查看下官方文档：[AbstractProcessor]([在线文档-jdk-zh (oschina.net)](https://tool.oschina.net/apidocs/apidoc?api=jdk-zh))

这里做一个简单的摘要：

- 完整包名：javax.annotation.processing.AbstractProcessor
- 实现了接口：javax.annotation.processing.Processor
- `Processor` 的每次实现都必须提供一个公共的**无参数构造方法**，工具将使用该构造方法实例化 Processor。
- 接下来，工具调用具有适当 `ProcessingEnvironment` 的 [`init`](https://tool.oschina.net/uploads/apidocs/jdk-zh/javax/annotation/processing/Processor.html#init(javax.annotation.processing.ProcessingEnvironment)) 方法。
- 然后，工具调用 [`getSupportedAnnotationTypes`](https://tool.oschina.net/uploads/apidocs/jdk-zh/javax/annotation/processing/Processor.html#getSupportedAnnotationTypes())、[`getSupportedOptions`](https://tool.oschina.net/uploads/apidocs/jdk-zh/javax/annotation/processing/Processor.html#getSupportedOptions()) 和 [`getSupportedSourceVersion`](https://tool.oschina.net/uploads/apidocs/jdk-zh/javax/annotation/processing/Processor.html#getSupportedSourceVersion())。这些方法只在每次运行时调用一次。
- 在适当的时候，工具在 `Processor` 对象上调用 [`process`](https://tool.oschina.net/uploads/apidocs/jdk-zh/javax/annotation/processing/Processor.html#process(java.util.Set, javax.annotation.processing.RoundEnvironment)) 方法；

由于在`AbstractProcessor`类中，已经简单的处理了上述所需的调用方法，且这里我们还不需要高级的更改。且默认的构造函数为无参构造。故而这里这个类定义为：

```java
package com.weizu.annotation_processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

public class ARouterProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }
}
```

也就是我们的重点在于`process`方法的编写。

但是实际上是需要写的，如果不写我遇到了这个错误：

> No SupportedSourceVersion annotation found on com.weizu.annotation_processor.ARouterProcessor, returning RELEASE_6.

然后我们还需要在 main 文件夹下，与 java 同一级目录中创建 resources 等文件：，如下图：

<img src="imgs/屏幕截图 2021-05-31 123509.png" style="zoom:60%;" />

然后就可以开始`process`方法的编写。

首先我们需要将`annotation这个模块`导入到`annotation_processor`的依赖中，即：

```java
dependencies {
    // 依赖于注解
    implementation project(':annotation')
}
```

这样我们就可以在`ARouterProcessor`中使用`ARouter`。然后我们开始编写代码：

```java
package com.weizu.annotation_processor;

import com.weizu.annotation.ARouter;
import java.util.Collections;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

public class ARouterProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ARouter.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("Get into process method.");
        for(TypeElement element: annotations){
            if(element.getQualifiedName().toString().equals(ARouter.class.getCanonicalName())){
                //System.out.println(element.get);
            }
        }
        return true;
    }
}
```

在`build`中可以看见我们打印的信息：

<img src="imgs/屏幕截图 2021-05-31 152412.png" style="zoom:60%;" />

不妨查看下`TypeElement`有哪些属性和方法：[TypeElement]([在线文档-jdk-zh (oschina.net)](https://tool.oschina.net/apidocs/apidoc?api=jdk-zh))

```java
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    System.out.println("Get into process method.");
    for(TypeElement element: annotations){
        if(element.getQualifiedName().toString().equals(ARouter.class.getCanonicalName())){
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(element);
            for (Element el : elements) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = el.getAnnotationMirrors().get(0).getElementValues();
                String path = null;
                for(ExecutableElement e : elementValues.keySet()){
                    //System.out.println(e.toString()+"|"+elementValues.get(e).toString()); // path()|"123"
                    path = elementValues.get(e).toString();
                }
                if(path!=null){

                }
            }
        }
    }
    return false;
}
```

参考[Android 中自定义注解和注解解析]([Android 中自定义注解和注解解析_wangjiang-CSDN博客_android 注解解析器](https://blog.csdn.net/wangjiang_qianmo/article/details/99350360))虽然可以在调用的包下写一个Java文件来做Intent，但是这样我却解决不了两个问题：

- 虽然可以代码生成一个Intent的封装，但是如何触发？
- 这种内在的Intent方式是否是阿里的ARouter的封装？

带着这两个我目前解决不了的问题继续学这个视频[Android组件化实战（二）]([Android组件化实战（二）_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1X5411A7dc/?spm_id_from=trigger_reload))

在视频中使用auto-service，如下：

```groovy
dependencies {
    implementation fileTree(dir: 'libs', includes: ['*.jar'])

    // 编译时期进行注解处理
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc4'
    compileOnly 'com.google.auto.service:auto-service:1.0-rc4'

    // 帮助我们通过类调用的方式来生成Java代码[JavaPoet]
    implementation 'com.squareup:javapoet:1.10.0'

    // 依赖于注解
    implementation project(':annotation')
}
```

## auto-service

[Android关于AutoService、Javapoet讲解 - 帅气的码农 - 博客园 (cnblogs.com)](https://www.cnblogs.com/limingblogs/p/8074582.html)

- AutoService会自动在META-INF文件夹下生成Processor配置信息文件
-  而当外部程序装配这个模块的时候，就能通过该jar包META-INF/services/里的配置文件找到具体的实现类名，并装载实例化，完成模块的注入。基于这样一个约定就能很好的找到服务接口的实现类，而不需要再代码里制定，方便快捷。

## javapoet

- JavaPoet是一款可以自动生成Java文件的第三方依赖

- 可以很方便的使用它根据注解、数据库模式、协议格式等来对应生成代码。

然后其实看了这节视频还是很懵，所以一口气看完了整个视频，最终的路由过程我觉得其逻辑是这样的。

- 根据规则解析和封装预定义的注解，然后使用代码生成技术来生成Java文件来生成逻辑上的路由表项；
- 自定义一个路由管理器类，这个路由管理器类中提供传入参数，和跳转的方法；
- 而跳转的逻辑其实还是Intent;

由于视频比较长，这里我来尝试写一个简单的版本，来做路由的跳转。

不妨先理清下思路：

#### 1. 类上的注解

用来标识这个类的自身的路由标识，类似于一个物理计算机对应一个Mac地址。比如：

```java
package cn.tim.study_module;

@ARouter(path = "/app/MainActivity", group = "app")
public class MainActivity extends AppCompatActivity 
```

表明`app`模块下的`/app/MainActivity`对应的路由标识为`cn.tim.study_module.MainActivity`。对应于前面的代码生成技术，这里需要使用一个类可以动态的添加路由信息到这个类中。

#### 2. 启动

```java
RouterManager.getInstance()
    .build("/order/OrderMainActivity")
    .withString("name", "张三")
    .withInt("age", 20)
    .navigation(this);
```

也就是出发操作，需要可以传递要跳转的路由的标识，然后可以传递一些参数，最后调用。

这里需要实例化前面生成的类，然后加载路由信息，按照路由表对应的进行实例化，然后跳转。

这里写下Intent最简单的跳转，如下：

```java
Intent intent = new Intent(this, OtherActivity.class);
startActivity(intent);
```

这里就按照最简单的来。

## 简单实现

但是，在`annotation_processor`中，我使用了`auto-services`但是却无法生成对应的`annotation_processor\src\main\resources\META-INF\services\javax.annotation.processing.Processor`文件，所以这里我还是手动创建了。

然后为了简便就使用

```java
@AutoService(Process.class)
@SupportedAnnotationTypes({"com.weizu.annotation.ARouter"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"path"})
```

虽然生成不了，但是这里使用对应的注解可以减少相关方法的书写。

然后，我们的目标就是生成一个Java来存储我们的路由信息，比如：

```java
public class ARouter$$map implements ARouterPath {
  public Map<String, String> getMaps() {
    Map<String,String> pathMap = new HashMap<>();
    pathMap.put("123", "com.weizu.myapplication.MainActivity");
    pathMap.put("233", "com.weizu.myapplication.OtherActivity");
    return pathMap;
  }
}
```

且在不同的模块下，应该具有不同的路由信息表，也就是我们需要使用一种统一的标识来标识这个生成的类，且这个类名需要关联模块名字。

这里，为了简单，我就不考虑使用不同的模块。因为模块名字的获取还需要使用gradle。

然后我们尝试来写生成Java这个Java文件的代码，如下：

```java
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
```

也就是建造新的Java文件叫做`ARouter$$map`，使用的包是`com.weizu.router`，对于接口的申明是因为在实例化的时候，需要一个对象来接收，而`ARouter$$map`在编译时候才自动生成，故而不能作为接收的对象，可以使用一个接口来做。

然后我们新定义一个`Android library`的模块，在这个模块中，我们申明接口`com.weizu.api.ARouterPath`，然后再定义一个管理类，对于接口如下：

```java
public interface ARouterPath {
    public Map<String, String> getMaps();
}
```

对于管理类：

```java
public class ARouterManager {

    public static void jump(Context context, String path) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // 加载路由得到类名
        String routerClassName = "com.weizu.router.ARouter$$map";
        Class<? extends ARouterPath> clazz = (Class<? extends ARouterPath>) Class.forName(routerClassName);
        ARouterPath aRouterPath = clazz.newInstance();
        if(aRouterPath!=null){
            Map<String, String> maps = aRouterPath.getMaps();
            String className = maps.get(path);
            Class<?> aClass = Class.forName(className);
            Intent intent = new Intent(context, aClass);
            context.startActivity(intent);
        }
    }
}
```

然后，就可以使用反射来进行加载，得到路由表中的对应的path的类的全称，然后就可以使用Intent实现跳转。

我们在app模块中建立两个Activity，需要注意的是需要注册：

```xml
<activity android:name=".OtherActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />

        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
<activity android:name=".MainActivity"/>
```

然后我们在OtherActivity中使用：

```java
@ARouter(path = "233")
public class OtherActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);

        TextView viewById = findViewById(R.id.text);
        viewById.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        try {
            ARouterManager.jump(this, "123");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
```

当然在MainActivity中注册：

```java
@ARouter(path = "123")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
```

运行就可以发现实现了跳转。

这个简易版本的跳转实现，感觉很容易理解，所以就还是保存一份到github。

