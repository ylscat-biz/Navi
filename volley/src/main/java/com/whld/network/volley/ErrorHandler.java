package com.whld.network.volley;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

/**
 * Created at 2015/11/18.
 *
 * @author YinLanShan
 */
public class ErrorHandler implements Response.ErrorListener {
    private static final String TAG = ErrorHandler.class.getSimpleName();
    private String mUrl;

    public void onErrorResponse(VolleyError error) {
        Context context = Network.sContext;
        String msg = mUrl == null ? error.getMessage() : "FAIL URL:" + mUrl;
        if(error instanceof NoConnectionError) {
            Toast.makeText(context, "亲，网络连接失败！", Toast.LENGTH_SHORT).show();
        }
        else if (error instanceof TimeoutError) {
            Toast.makeText(context, "亲，网络连接超时！", Toast.LENGTH_SHORT).show();
        }
        else if (error instanceof NetworkError) {
            Toast.makeText(context, "亲，网络连接错误！", Toast.LENGTH_SHORT).show();
        }
        else if (error instanceof ServerError) {
            Toast.makeText(context, "亲，服务器君开了个小差，请稍后再试~~~", Toast.LENGTH_SHORT).show();
        }
        else if (error instanceof ParseError) {
            Toast.makeText(context, "数据解析错误", Toast.LENGTH_SHORT).show();
        }
        else if(error instanceof ProtocolError) {
            ProtocolError e = (ProtocolError)error;
            Toast.makeText(context, e.msg, Toast.LENGTH_SHORT).show();
        }

        Log.e(TAG, msg, error);
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}
