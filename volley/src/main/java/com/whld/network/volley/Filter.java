package com.whld.network.volley;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;

/**
 * Created at 2015/11/18.
 *
 * @author YinLanShan
 */
public interface Filter<T> {
    Response<T> filter(T data, NetworkResponse response);
}
