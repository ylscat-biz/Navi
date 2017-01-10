package com.whld.network.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 自定义JSONArray，更好的处理NULL对象
 * Created at 2015/12/9.
 *
 * @author YinLanShan
 */
public class JsonArray extends JSONArray {

    @Override
    public String getString(int index) throws JSONException {
        Object object = get(index);
        if(object == JSONObject.NULL)
            return null;
        return super.getString(index);
    }

    @Override
    public String optString(int index) {
        return optString(index, null);
    }

    @Override
    public String optString(int index, String fallback) {
        Object object = opt(index);
        if(object == JSONObject.NULL || object == null)
            return fallback;

        return String.valueOf(object);
    }
}
