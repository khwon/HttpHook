package org.kh.httphook;

import android.util.Log;

import com.saurik.substrate.MS;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.InflaterInputStream;

/**
 * Created by kanghee on 11/4/14.
 */
public class HttpHook {
    public static void initialize(){
        final String TAG = "HTTP_HOOK";
        final boolean on = true;
        final boolean off = false;
        //Log.i(TAG, "initialize()");

        final String package_name;
        package_name = "org.apache.http.impl.client.AbstractHttpClient";
        MS.hookClassLoad(package_name, new MS.ClassLoadHook() {
            @Override
            public void classLoaded(Class<?> aClass) {
                Log.d(TAG,"loaded " + package_name);
                Method method = null;
                final String methodName = "execute";
                /*
                for(Method m: aClass.getDeclaredMethods()){
                    if(m.toString().contains("")){
                        m.setAccessible(true);
                        method = m;
                    }
                }
                */
                Class[] args = new Class[3];
                args[0] = HttpHost.class;
                args[1] = HttpRequest.class;
                args[2] = HttpContext.class;
                try {
                    method = aClass.getMethod(methodName,args);
                } catch (NoSuchMethodException e) {
                    method = null;
                    Log.d(TAG, "cannot find " + methodName);
                }
                if(method != null){
                    Log.d(TAG,"start hook " + methodName);
                    final MS.MethodPointer old = new MS.MethodPointer();
                    MS.hookMethod(aClass, method, new MS.MethodHook() {
                        @Override
                        public Object invoked(Object arg0, Object... args) throws Throwable {
                            Log.d(TAG,"inside " + methodName);
                            AbstractHttpClient cli = (AbstractHttpClient) arg0;
                            cli.addRequestInterceptor(new HttpRequestInterceptor() {
                                @Override
                                public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
                                    Log.i(TAG, ">> request");
                                    Log.i(TAG, httpRequest.getRequestLine().toString());
                                    try {
                                        for (Header h : httpRequest.getAllHeaders()) {
                                            Log.i(TAG, h.getName() + ": " + h.getValue());
                                        }
                                        Log.d(TAG, "body");
                                        if (httpRequest.getRequestLine().getMethod().equals("POST")) {
                                            HttpEntity old = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
                                            if (old != null) {
                                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                long size = old.getContentLength();
                                                byte[] arr = new byte[2048];
                                                old.writeTo(baos);
                                                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                                                HttpEntity new_entity = new InputStreamEntity(bais, size);
                                                ((HttpEntityEnclosingRequest) httpRequest).setEntity(new_entity);
                                                if (old.getContentEncoding() != null && old.getContentEncoding().getValue().equals("deflate")) {
                                                    ByteArrayInputStream bais2 = new ByteArrayInputStream(baos.toByteArray());
                                                    InflaterInputStream stream = new InflaterInputStream(bais2);
                                                    size = stream.read(arr);
                                                    Log.i(TAG, new String(arr, 0, (int) size));
                                                } else {
                                                    if (old.getContentEncoding() != null) {
                                                        Log.i(TAG, old.getContentEncoding().getValue());
                                                    }
                                                    Log.i(TAG, new String(arr, 0, (int) size));
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.d(TAG, "got exception");
                                        Log.d(TAG, e.getClass().toString());
                                        for (StackTraceElement s : e.getStackTrace()) {
                                            Log.d(TAG, s.toString());
                                        }
                                        Log.d(TAG, e.getMessage());
                                    }
                                }
                            });
                            cli.addResponseInterceptor(new HttpResponseInterceptor() {
                                @Override
                                public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                                    Log.i(TAG,"<< response");
                                    for(Header h: httpResponse.getAllHeaders()){
                                        Log.i(TAG,h.getName() + ": " + h.getValue());
                                    }
                                    if(httpResponse.getEntity() != null) {
                                        HttpEntity old = httpResponse.getEntity();
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        long size = old.getContentLength();
                                        byte[] arr = new byte[2048];
                                        old.writeTo(baos);
                                        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                                        HttpEntity new_entity = new InputStreamEntity(bais, size);
                                        httpResponse.setEntity(new_entity);
                                        if (old.getContentEncoding() != null && old.getContentEncoding().getValue().equals("deflate")) {
                                            ByteArrayInputStream bais2 = new ByteArrayInputStream(baos.toByteArray());
                                            InflaterInputStream stream = new InflaterInputStream(bais2);
                                            size = stream.read(arr);
                                            Log.i(TAG, new String(arr, 0, (int) size));
                                        } else {
                                            if(old.getContentEncoding() != null) {
                                                Log.d(TAG, old.getContentEncoding().getValue());
                                            }
                                            Log.i(TAG, new String(arr, 0, (int) size));
                                        }
                                    }
                                }
                            });
                            return old.invoke(arg0,args);
                        }
                    },old);
                }
            }
        });
        /*
        //https://android.googlesource.com/platform/libcore-snapshot/+/ics-mr1/luni/src/main/java/libcore/net/http/HttpsURLConnectionImpl.java

// TODO : test
        //MS.hookClassLoad("libcore.net.http.HttpsURLConnectionImpl", new MS.ClassLoadHook() {
        MS.hookClassLoad("com.android.okhttp.internal.http.HttpsURLConnectionImpl", new MS.ClassLoadHook() {
            @Override
            public void classLoaded(Class<?> clazz) {
                Log.i(TAG, "classLoaded");
                final String methodName = "connect";
                Method method;
                try {
                    method = clazz.getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    method = null;
                    Log.i(TAG, "cannot find " + methodName);
                }
                final Class<?> _clazz = clazz;
                if (method != null) {
                    final MS.MethodPointer old = new MS.MethodPointer();
                    MS.hookMethod(clazz, method, new MS.MethodHook() {
                        @Override
                        public Object invoked(Object arg0, Object... args) throws Throwable {
                            Object result = null;
                            HttpsURLConnection _this = (HttpsURLConnection) arg0;
                            try {
                                Log.i(TAG, "> starting request dump");
                                Log.i(TAG, _this.getRequestMethod() + " " + _this.getURL().toString());

                                //https://android.googlesource.com/platform/libcore-snapshot/+/ics-mr1/luni/src/main/java/libcore/net/http/HttpsURLConnectionImpl.java
                                Field f = _clazz.getDeclaredField("delegate");
                                f.setAccessible(true);
                                Object delegate_obj = f.get(arg0);

                                //https://android.googlesource.com/platform/libcore-snapshot/+/ics-mr1/luni/src/main/java/libcore/net/http/HttpURLConnectionImpl.java
                                f = delegate_obj.getClass().getSuperclass().getDeclaredField("rawRequestHeaders");
                                f.setAccessible(true);
                                Object h = f.get(delegate_obj);
                                //https://android.googlesource.com/platform/libcore-snapshot/+/ics-mr1/luni/src/main/java/libcore/net/http/RawHeaders.java
                                Method toMultimap = h.getClass().getMethod("toMultimap");
                                Map<String, List<String>> req_headers = (Map<String, List<String>>) toMultimap.invoke(h);
                                //Map<String, List<String>> req_headers = _this.getRequestProperties(); // cause error if already connected
                                for (String key : req_headers.keySet()) {
                                    String v = "";
                                    for (String vv : req_headers.get(key)) {
                                        v += vv;
                                    }
                                    Log.i(TAG, key + ": " + v);
                                }
                                result = old.invoke(arg0);
                                Log.i(TAG, "> starting response dump");
                                Map<String, List<String>> res_headers = _this.getHeaderFields();
                                if(res_headers != null) {
                                    for (String key : res_headers.keySet()) {
                                        String v = "";
                                        for (String vv : res_headers.get(key)) {
                                            v += vv;
                                        }
                                        if (key == null) {
                                            Log.i(TAG, v);
                                        } else {
                                            Log.i(TAG, key + ": " + v);
                                        }
                                    }
                                }else{
                                    Log.i(TAG, "response header is null");
                                }
                            } catch(Exception e){
                                Log.i(TAG,"got exception");
                                Log.i(TAG,e.getClass().toString());
                                Log.i(TAG,e.getMessage());
                                for(StackTraceElement s: e.getStackTrace()){
                                    Log.i(TAG,s.toString());
                                }
                            }
                            return result;
                        }
                    }, old);
                }

            }
        });
                */
    }
}
