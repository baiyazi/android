package com.weizu.router_manager2;


import android.content.Context;
import android.content.Intent;

import com.weizu.api.ARouterPath;

import java.util.Map;

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