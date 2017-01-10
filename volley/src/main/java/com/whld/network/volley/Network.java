package com.whld.network.volley;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created at 2015/11/18.
 * 使用方法：
 *   1 在Application onCreate()里，init(context)
 *   2 [可选] 配置公共参数 sCommonParameters
 *   3 [可选] 设置Cookie setCookie()
 *   4 调用request()等方法，请求网络数据
 * @author YinLanShan
 */
public class Network {
    static Context sContext;
    public static RequestQueue sRequestQueue;

    private static String sCookie;
    public static Map<String, String> sCommonParameters = new LinkedHashMap<>();

    public static void init(Context context) {
        sContext = context;
        sRequestQueue = Volley.newRequestQueue(context);
    }

    public static JsonRequest request(String url, Map<String, String> params, Callback callback) {
        ResponseHandler handler = new ResponseHandler(callback);
        JsonRequest request = new JsonRequest(url, mergeParams(params), handler);
        initJsonRequest(request);

        sRequestQueue.add(request);
        return request;
    }

    public static JsonRequest request(String url, Map<String, String> params,
                                      Callback callback,
                                      Response.ErrorListener errorListener) {
        ResponseHandler handler = new ResponseHandler(callback, errorListener);
        JsonRequest request = new JsonRequest(url, mergeParams(params), handler);
        initJsonRequest(request);
        sRequestQueue.add(request);
        return request;
    }

    public static JsonRequest requestSilently(String url, Map<String, String> params, Callback callback) {
        ResponseHandler handler = new ResponseHandler(callback, null);
        JsonRequest request = new JsonRequest(url, mergeParams(params), handler);
        initJsonRequest(request);
        sRequestQueue.add(request);
        return request;
    }

    public static JsonRequest postSilently(String url, Map<String, String> params, Callback callback) {
        ResponseHandler handler = new ResponseHandler(callback, null);
        JsonRequest request = new JsonRequest(Request.Method.POST, url, mergeParams(params), handler);
        initJsonRequest(request);
        sRequestQueue.add(request);
        return request;
    }

    public static JsonRequest post(String url, Map<String, String> params, Callback callback) {
        ResponseHandler handler = new ResponseHandler(callback);
        JsonRequest request = new JsonRequest(Request.Method.POST, url, mergeParams(params), handler);
        initJsonRequest(request);
        sRequestQueue.add(request);
        return request;
    }

    public static MultipartRequest postPhoto(String url, Map<String, String> params,
                                        String key, File photo,
                                        Callback callback){
        ResponseHandler handler =  new ResponseHandler(callback);
        MultipartRequest request = new MultipartRequest(url, mergeParams(params), handler);
        initJsonRequest(request);
        request.addFile(key, determineType(photo.getName()), photo);
        sRequestQueue.add(request);
        return request;
    }

    public static MultipartRequest postPhotoSilently(
            String url, Map<String, String> params,
            String key, File photo, String fileName,
            Callback callback) {
        ResponseHandler handler = new ResponseHandler(callback, null);
        MultipartRequest request = new MultipartRequest(url, mergeParams(params), handler);
        initJsonRequest(request);
        request.addFile(key, determineType(fileName), fileName, photo);
        sRequestQueue.add(request);
        return request;
    }

    private static String determineType(String fileName) {
        if(fileName.toLowerCase().endsWith("png")) {
            return "image/png";
        }

        return "image/jpeg";
    }

    @SuppressWarnings("StringEquality")
    public static void setCookie(String cookie) {
        if(sCookie != null && sCookie.equals(cookie) || sCookie == cookie)
            return;
        sCookie = cookie;
    }

    private static void initJsonRequest(JsonRequest request) {
        if(sCookie != null) {
            try {
                Map<String, String> headers= request.getHeaders();
                headers.put("Cookie", sCookie);
            } catch (AuthFailureError authFailureError) {
                //ignore
            }
        }
        Log.d("Network", "Session:" + sCookie);
    }

    private static Map<String, String> mergeParams(Map<String, String> params) {
        LinkedHashMap<String, String> p = new LinkedHashMap<String, String>();
    	if(params != null) {
//            String dev = params.get("devid");
//            if(dev != null) {
//                params.put("devid", dev + "A");
//            }
            p.putAll(params);
    	}

    	p.putAll(sCommonParameters);
    	return p;
    }
}
