package com.whld.network.json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 自定义JSONObject，更好的处理NULL对象
 * Created at 2015/12/9.
 *
 * @author YinLanShan
 */
public class JsonObject extends JSONObject {
    @Override
    public String getString(String name) throws JSONException {
        Object object = get(name);
        if(object == JSONObject.NULL)
            return null;
        return super.getString(name);
    }

    @Override
    public String optString(String name) {
        return optString(name, null);
    }

    @Override
    public String optString(String name, String fallback) {
        Object object = opt(name);
        if(object == JSONObject.NULL || object == null)
            return fallback;

        return String.valueOf(object);
    }
}
