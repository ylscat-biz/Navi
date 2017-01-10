package com.whld.network.volley;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created at 2015/11/18.
 *
 * @author YinLanShan
 */
public class ResponseHandler implements
        Response.Listener<JSONObject>,
        Response.ErrorListener {
    private static final String TAG = ResponseHandler.class.getSimpleName();

    private Callback mCallback;
    private Response.ErrorListener mErrorListener;
    private String mUrl;
    private Map<String, String> mHeaders;

    public ResponseHandler(Callback callback, Response.ErrorListener errorListener) {
        mCallback = callback;
        mErrorListener = errorListener;
    }

    public ResponseHandler(Callback callback) {
        mCallback = callback;
        mErrorListener = new ErrorHandler();
    }

    @Override
    public void onResponse(JSONObject response) {
        if(mCallback != null) {
            mCallback.onResponse(response, mHeaders, null);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        if(mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
        else {
            Log.e(TAG, "FAIL url:" + mUrl, error);
        }
        if(mCallback != null) {
            mCallback.onResponse(null, mHeaders, error);
        }
    }

    public void setUrl(String url) {
        mUrl = url;
        if(mErrorListener instanceof ErrorHandler) {
            ((ErrorHandler) mErrorListener).setUrl(url);
        }
    }

    public void setHeaders(Map<String, String> respHeaders) {
        mHeaders = respHeaders;
    }
}
