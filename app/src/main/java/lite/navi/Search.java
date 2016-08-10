package lite.navi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;

import org.apmem.tools.layouts.FlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler2;
import in.srain.cube.views.ptr.PtrFrameLayout;
import lite.navi.account.Account;
import lite.navi.view.Utils;

/**
 * @author ylscat
 *         Date: 2016-07-27 16:07
 */
public class Search extends Activity implements
        View.OnClickListener,
        TextView.OnEditorActionListener,
        OnGetPoiSearchResultListener, AdapterView.OnItemClickListener {
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_RESULT_DETAIL = "result_detail";

    private static final String SP_KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY_COUNT = 10;

    private AutoCompleteTextView mInput;
    private ArrayList<String> mHistory;
    private HashSet<String> mExcludes = new HashSet<>();
    private FlowLayout mHistoryPanel;
    private Dialog mWaitingDialog;

    private View mPanel;
    private ListView mListView;
    private PtrClassicFrameLayout mPtrLayout;
    private Adapter mAdapter;

    private PoiSearch mPoiSearch;
    private String mKey;
    private int mPage, mMaxPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.search).setOnClickListener(this);
        TextView tv = (TextView) findViewById(R.id.gas);
        mExcludes.add(tv.getText().toString());
        tv.setOnClickListener(this);
        tv = (TextView) findViewById(R.id.restaurant);
        mExcludes.add(tv.getText().toString());
        tv.setOnClickListener(this);
        tv = (TextView) findViewById(R.id.hotel);
        mExcludes.add(tv.getText().toString());
        tv.setOnClickListener(this);
        tv = (TextView) findViewById(R.id.parking);
        mExcludes.add(tv.getText().toString());
        tv.setOnClickListener(this);

        mHistoryPanel = (FlowLayout) findViewById(R.id.history);
        mPanel = (View) mHistoryPanel.getParent();
        ListView listView = (ListView) findViewById(R.id.list);
        mAdapter = new Adapter();
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        mListView = listView;
        mPtrLayout = (PtrClassicFrameLayout) listView.getParent();
        mPtrLayout.setDurationToClose(300);
        mPtrLayout.setForceBackWhenComplete(true);
        mPtrLayout.setPtrHandler(new PtrDefaultHandler2() {

            @Override
            public void onLoadMoreBegin(PtrFrameLayout frame) {
                loadMore();
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {

            }

            @Override
            public boolean checkCanDoLoadMore(PtrFrameLayout frame, View content, View footer) {
                return mPage < mMaxPage && super.checkCanDoLoadMore(frame, mListView, footer);
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return false;
            }
        });

        mInput = (AutoCompleteTextView) findViewById(R.id.input);
        mInput.setOnEditorActionListener(this);
        mHistory = getHistory();
        for(int i = mHistory.size() - 1; i >= 0; i--)
            showHistoryItems(mHistory.get(i));

        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);
    }

    private void search() {
        String key = mInput.getText().toString().trim();
        mInput.dismissDropDown();
        if(TextUtils.isEmpty(key)) {
            Toast.makeText(this, "请输入关键字", Toast.LENGTH_SHORT).show();
            return;
        }
        if(key.equals(mKey))
            return;
        mKey = key;
        mMaxPage = 0;
        mPage = 0;
        BDLocation location = ((App)getApplication()).mLocation;
        if(location == null)
            return;
        PoiNearbySearchOption option = new PoiNearbySearchOption()
                .location(new LatLng(location.getLatitude(), location.getLongitude()))
                .keyword(key)
                .radius(100000)
                .sortType(PoiSortType.distance_from_near_to_far);

        showWaitingDialog();
        mPoiSearch.searchNearby(option);
        mAdapter.mList.clear();
        if(mPtrLayout.getVisibility() == View.VISIBLE)
            mAdapter.notifyDataSetChanged();
    }

    private void loadMore() {
        BDLocation location = ((App)getApplication()).mLocation;
        if(location == null)
            return;
        PoiNearbySearchOption option = new PoiNearbySearchOption()
                .location(new LatLng(location.getLatitude(), location.getLongitude()))
                .keyword(mKey)
                .pageNum(++mPage)
                .radius(100000)
                .sortType(PoiSortType.distance_from_near_to_far);
        mPoiSearch.searchNearby(option);
    }

    private ArrayList<String> getHistory() {
        ArrayList<String> history = new ArrayList<>();
        SharedPreferences sp = getSharedPreferences(Account.getName(), MODE_PRIVATE);
        String hs = sp.getString(SP_KEY_HISTORY, null);
        if(hs == null)
            return history;
        String[] items = hs.split(";");
        Collections.addAll(history, items);
        return history;
    }

    private void addHistory(String key) {
        if(mExcludes.contains(key))
            return;
        int index = mHistory.indexOf(key);
        if(index == 0) {
            return;
        }
        else if(index > 0) {
            mHistory.remove(index);
            mHistory.add(0, key);
            View item = mHistoryPanel.getChildAt(index);
            mHistoryPanel.removeView(item);
            mHistoryPanel.addView(item, 0);
        }
        else {
            if (mHistory.size() >= MAX_HISTORY_COUNT) {
                index = mHistory.size() - 1;
                mHistory.remove(index);
                TextView tv = (TextView) mHistoryPanel.getChildAt(index);
                mHistoryPanel.removeView(tv);
            }
            mHistory.add(key);
            showHistoryItems(key);
        }

        StringBuilder sb = new StringBuilder();
        for(String s : mHistory) {
            sb.append(s).append(";");
        }
        SharedPreferences sp = getSharedPreferences(Account.getName(), MODE_PRIVATE);
        sp.edit().putString(SP_KEY_HISTORY, sb.substring(0, sb.length() - 1)).apply();
    }

    private void showHistoryItems(String item) {
        LayoutInflater inflater = getLayoutInflater();

        TextView tv = (TextView) inflater.inflate(R.layout.history_item, mHistoryPanel, false);
        tv.setText(item);
        tv.setOnClickListener(this);
        mHistoryPanel.addView(tv, 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                back();
                break;
            case R.id.search:
                search();
                break;
            case R.id.gas:
            case R.id.restaurant:
            case R.id.hotel:
            case R.id.parking:
                String text = ((TextView)v).getText().toString();
                mInput.setText(text);
                search();
                break;
            case View.NO_ID:
                text = ((TextView)v).getText().toString();
                mInput.setText(text);
                search();
                break;

        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        search();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPoiSearch.destroy();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        if(mPtrLayout.getVisibility() == View.VISIBLE) {
            mPtrLayout.setVisibility(View.GONE);
            mPanel.setVisibility(View.VISIBLE);
            mInput.setText(null);
            mPage = 0;
            mMaxPage = 0;
        }
        else {
            finish();
        }
    }

    @Override
    public void onGetPoiResult(PoiResult result) {
        dismissWaitingDialog();
        mPtrLayout.refreshComplete();
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            if(mPage == 0) {
                addHistory(mKey);
            }
            mMaxPage = result.getTotalPageNum();
            mAdapter.mList.addAll(result.getAllPoi());
            mPtrLayout.setVisibility(View.VISIBLE);
            mPanel.setVisibility(View.GONE);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult result) {
        dismissWaitingDialog();
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Intent data = new Intent();
            data.putExtra(EXTRA_RESULT_DETAIL, result);
            setResult(RESULT_OK, data);
            finish();
            addHistory(mKey);
        }
    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
        dismissWaitingDialog();
    }

    private void showWaitingDialog() {
        if(mWaitingDialog == null) {
            mWaitingDialog = Utils.createWaitingDialog(this);
        }
        if(!mWaitingDialog.isShowing() && !isFinishing()) {
            mWaitingDialog.show();
        }
    }

    private void dismissWaitingDialog() {
        if(mWaitingDialog != null && mWaitingDialog.isShowing()) {
            mWaitingDialog.dismiss();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PoiInfo info = (PoiInfo) parent.getItemAtPosition(position);
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT, info);
        setResult(RESULT_OK, data);
        finish();
    }

    class Adapter extends BaseAdapter {
        ArrayList<PoiInfo> mList = new ArrayList<>();

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.result_item,
                        parent, false);
            }

            PoiInfo info = mList.get(position);
            TextView tv = (TextView) convertView.findViewById(R.id.name);
            tv.setText(String.format("%d %s", position + 1, info.name));
            tv = (TextView) convertView.findViewById(R.id.address);
            tv.setText(info.address);
            return convertView;
        }
    }

    /*class SuggestionAdapter extends BaseAdapter implements Filterable {
        private List<SuggestionResult.SuggestionInfo> mList;

        @Override
        public int getCount() {
            if(mList == null)
                return 0;
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_dropdown_item_1line,
                        parent, false);
            }

            TextView tv = (TextView)convertView.findViewById(android.R.id.text1);
            tv.setText(mList.get(position).key);
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return null;
        }
    }*/
}
