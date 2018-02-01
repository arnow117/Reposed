package com.arnow117.reposd;

/**
 * Created by arnow117 on 25/01/2018.
 */

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;


public class HookUtils implements IXposedHookLoadPackage{
    interface CallBack{
        void execute(ClassLoader mClassLoader);
    }
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        Log.i("arnow117","Reposed Loaded in App: " + lpparam.packageName);
        if(!lpparam.packageName.equals(Config.TARGET_PACKAGE_NAME))
            return;

        initMutliDexHook(new CallBack() {
            @Override
            public void execute(ClassLoader mClassLoader) {
                HookUtils.hookSignatureCheck(mClassLoader);
                HookUtils.antiDectect(mClassLoader);
            }
        });

    }

    public void initMutliDexHook(final CallBack mCallBack){
        findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context)param.args[0];
                mCallBack.execute(context.getClassLoader());
//                mMutliClazzLoader = context.getClassLoader();//call back here
            }
        });
    }

    static void hookSignatureCheck(ClassLoader mClassLoader){
        XposedBridge.hookAllMethods(java.security.Signature.class, "verify", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("arnow117","Anti Signature Check !");
                param.setResult(Boolean.TRUE);
            }
        });

    }

    static void antiRootDetect(){
        XposedBridge.hookAllConstructors(File.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (((File)param.thisObject).getName().equals("su")){

                }
            }
        });
    }


    static void hookHashCode(ClassLoader mClassloacer, final int hashCode){
        findAndHookMethod("android.content.pm.Signatrue", mClassloacer, "hashCode", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("arnow117","hook hashCode");
                param.setResult(hashCode);
            }
        });
    }

    static void antiDectect(ClassLoader mClassLoader){

        findAndHookMethod("java.lang.ClassLoader", mClassLoader, "loadClass", String.class,new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String clazzName = (String)param.args[0];
                if (clazzName.contains("xposed")) {
                    Log.i("arnow117","hook loadclass for calss: "+clazzName);
                    param.setThrowable(new ClassNotFoundException());// set for class not found
                }
            }
        });
    }
    static void hookSSLPinning(){

    }

    private boolean hookCheck1(){
        Class clazz = null;
        Set<String> key = null;
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try{
            clazz = cl.loadClass("de.robv.android.xposed.XposedHelpers");
        }catch(Exception e){
            Log.i("arnow117","find class failed!");
        }

        if(clazz!=null){
            try {
                Field cacheField = clazz.getDeclaredField("fieldCache");//methodCache,constructorCache
                cacheField.setAccessible(true);
                HashMap fieldCache = (HashMap)cacheField.get(null);
                key = fieldCache.keySet();
            }catch (Throwable e){
            }
            String temp = null;
            for(String each : key){
                temp = each.toLowerCase();
                if (temp.contains("android.support")|temp.contains("javax.")|temp.contains("android.widget")|temp.contains("java.util")|temp.contains("android.webkit")|temp.contains("sun.http")){
                    return true;
                }
            }
        }
        return false;
    }


}
