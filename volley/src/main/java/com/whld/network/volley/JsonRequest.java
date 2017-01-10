package com.whld.network.volley;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.whld.network.json.JsonTokener;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Json请求框架
 * Created at 2015/11/18.
 *
 * @author YinLanShan
 */
public class JsonRequest extends com.android.volley.toolbox.JsonRequest<JSONObject> {
    private static final String TAG = JsonRequest.class.getSimpleName();

    protected Map<String, String> mParams;
    protected String mParamString;
    protected ResponseHandler mHandler;
    protected Filter<JSONObject> mFilter;

    private Map<String, String> sRequestHeaders = new LinkedHashMap<String, String>(1);

    public JsonRequest(String url, Map<String, String> params, ResponseHandler handler) {
        this(Method.GET, url, params, handler);
    }

    public JsonRequest(int method, String url, Map<String, String> params, ResponseHandler handler) {
        super(method, url, null, handler, handler);
        mParams = params;

        mHandler = handler;
        if (handler != null)
            handler.setUrl(url);
        mFilter = new DefaultFilter();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return sRequestHeaders;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        JSONObject json;
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            json = new JsonTokener(jsonString).readJson();
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }

        if (mHandler != null) {
            mHandler.setHeaders(response.headers);
            mHandler.setUrl(getUrl());
        }

        Log.d(TAG, "RESPONSE:\n\tURL=" + getUrl() + "\n\t" + json);
        if (mFilter != null)
            return mFilter.filter(json, response);
        return Response.success(json,
                HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    public String getUrl() {
        String url = super.getUrl();
        if (getMethod() == Method.GET) {
            if (mParamString != null) {
                return url + mParamString;
            }

            if (mParams != null) {
                StringBuilder sb = getParamString(mParams);
                if (sb != null)
                    mParamString = sb.toString();
                else
                    mParamString = "";
                mParams = null;
                Log.d(TAG, "Request URL: " + url + mParamString);
                return url + mParamString;
            }
        }
        return url;
    }

    @Override
    public void writeBody(OutputStream out) throws IOException {
        if (getMethod() == Method.POST) {
            if (mParamString == null && mParams != null) {
                StringBuilder sb = getPostParamString(mParams);
                if (sb != null)
                    mParamString = sb.toString();
                mParams = null;
                Log.d(TAG, String.format("Post URL: %s [%s]", super.getUrl(), mParamString));
            }
        }
        if (mParamString != null) {
            out.write(mParamString.getBytes(PROTOCOL_CHARSET));
        }
    }

    @Override
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }

    private StringBuilder getParamString(Map<String, String> params) {
        String encoding = getParamsEncoding();
        StringBuilder sb = new StringBuilder("?");
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null)
                    continue;
                if (value != null)
                    value = URLEncoder.encode(value, encoding);
                sb.append(URLEncoder.encode(key, encoding)).
                        append('=').
                        append(value).
                        append('&');
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Parameters encoding error", e);
            return null;
        }

        if (sb.length() == 1) {
            return null;
        } else {
            sb.deleteCharAt(sb.length() - 1);
            return sb;
        }
    }

    private StringBuilder getPostParamString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null)
                continue;
            if (value == null)
                value = "";
            sb.append(key).
                    append('=').
                    append(value).
                    append('&');
        }

        if (sb.length() == 0) {
            return null;
        } else {
            sb.deleteCharAt(sb.length() - 1);
            return sb;
        }
    }

    public void setFilter(Filter<JSONObject> filter) {
        mFilter = filter;
    }
}
