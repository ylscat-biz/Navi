package com.whld.network.volley;

import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created at 2015/11/18.
 *
 * @author YinLanShan
 */
public interface Callback {
    void onResponse(JSONObject json, Map<String, String> headers, VolleyError error);
}
