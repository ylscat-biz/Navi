package lite.navi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.whld.network.volley.Callback;
import com.whld.network.volley.Network;
import com.whld.network.volley.ProtocolError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gallery.picker.ImagePicker;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler2;
import in.srain.cube.views.ptr.PtrFrameLayout;
import lite.navi.network.Urls;

/**
 * @author ylscat
 *         Date: 2016-12-05 05:23
 */

public class Team extends Activity implements
        View.OnClickListener,
        ListView.OnItemClickListener, Callback, BaiduNaviManager.RoutePlanListener, BDLocationListener {
    private static final String TAG = "Team";

    private static Team sInstance;

    private EditText mNameView;
    private ImageView mAvatarView;
    private String mName = "", mAvatar;
    private File mNewAvatar;
    private TextView mSaveView, mCancelView;
    private View mSpinView;
    private PtrClassicFrameLayout mPtr;

    private LocationClient mLocClient;

    private EditText mInput;
    private Adapter mAdapter;

    private Dialog mWaitingDialog;

    private String mStartAddr, mStopAddr;
    private Double mDstLng, mDstLat;
    private Double mCurLng, mCurLat;
    private String mGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.team_search);
        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.join).setOnClickListener(this);
        mSaveView = (TextView) findViewById(R.id.edit);
        mSaveView.setOnClickListener(this);
        mCancelView = (TextView) findViewById(R.id.cancel);
        mCancelView.setOnClickListener(this);
        mSpinView = findViewById(R.id.spin);
        mPtr = (PtrClassicFrameLayout) findViewById(R.id.ptr);
        mPtr.setPtrHandler(new PtrDefaultHandler2() {
            @Override
            public void onLoadMoreBegin(PtrFrameLayout frame) {

            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                searchNearby();
            }
        });
        mPtr.setEnabledNextPtrAtOnce(true);

        mNameView = (EditText) findViewById(R.id.name);
        mAvatarView = (ImageView) findViewById(R.id.avatar);
        mAvatarView.setOnClickListener(this);

        mAdapter = new Adapter();
        ListView lv = (ListView) findViewById(R.id.list);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(this);
        mInput = (EditText) findViewById(R.id.input);

        loadAccount();

        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setNeedDeviceDirect(true);
        option.setIsNeedAddress(true);
        mLocClient.setLocOption(option);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mLocClient.requestLocation() != 0) {
            mSpinView.post(new Runnable() {
                @Override
                public void run() {
                    mPtr.autoRefresh();
                }
            });
        } else {
            mSpinView.setVisibility(View.VISIBLE);
        }
        sInstance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        sInstance = null;
    }

    private void searchNearby() {
        App app = (App)getApplication();
        if(app.mLocation != null) {
            BDLocation loc = app.mLocation;
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("lng", String.valueOf(loc.getLongitude()));
            params.put("lat", String.valueOf(loc.getLatitude()));
            Network.request(Urls.GROUP_NEARBY, params, this);
        }
        else {
            mAdapter.mList.clear();
            mAdapter.notifyDataSetChanged();
            mPtr.refreshComplete();
        }
    }

    private void loadAccount() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("devid", Build.SERIAL);
        Network.request(Urls.PROFILE, params, new Callback() {
            @Override
            public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                if(error != null) {
                    if(error instanceof ProtocolError) {
                        mName = null;
                    }
                    return;
                }
                JSONObject data = json.optJSONObject("data");
                String avatar = data.optString("avatar");
                String name = mName = data.optString("name");
                if(!TextUtils.isEmpty(name)) {
                    mNameView.setText(name);
                    if(!TextUtils.isEmpty(avatar)) {
                        mAvatar = Urls.SERVER + avatar;
                        ImageLoader.getInstance().displayImage(
                                mAvatar, mAvatarView);
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.join:
                String groupId = mInput.getText().toString();
                if(groupId.length() == 0) {
                    Toast.makeText(this, "群组号不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                BDLocation start = App.sApp.mLocation;
                if(start == null) {
                    return;
                }
                joinGroup(start.getLatitude(), start.getLongitude(),
                        start.getStreet() + start.getStreetNumber(),
                        groupId);

                break;
            case R.id.edit:
                if(mNameView.isEnabled()) {
                    if(mName == null) {
                        LinkedHashMap<String, String> params = new LinkedHashMap<>();
                        params.put("devid", Build.SERIAL);
                        Network.post(Urls.CREATE, params, new Callback() {
                            @Override
                            public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                                setWaiting(false);
                                if(error == null) {
                                    updateProfile();
                                    mName = "";
                                }
                            }
                        });
                        setWaiting(true);
                    }
                    else
                        updateProfile();
                }
                else {
                    setEditing(true);
                }
                break;
            case R.id.cancel:
                {
                    mNameView.setText(mName);
                    if(mAvatar == null) {
                        mAvatarView.setImageResource(R.drawable.def_avatar);
                    }
                    else {
                        ImageLoader.getInstance().displayImage(mAvatar, mAvatarView);
                    }
                    if(mNewAvatar != null) {
                        mNewAvatar.delete();
                        mNewAvatar = null;
                    }
                    setEditing(false);
                }
                break;
            case R.id.avatar:
                if(mNameView.isEnabled()) {
                        Intent intent = new Intent(this, ImagePicker.class);
                        intent.putExtra(ImagePicker.EXTRA_WITH_CAMERA, true);
                        intent.putExtra(ImagePicker.EXTRA_MAX, 1);
                        startActivityForResult(intent, 1);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1 && resultCode == RESULT_OK) {
            String path = data.getStringArrayListExtra(ImagePicker.EXTRA_RESULT).get(0);
            new AsyncTask<String, Object, File>() {
                @Override
                protected void onPreExecute() {
                    setWaiting(true);
                }

                @Override
                protected File doInBackground(String[] params) {
                    File f = new File(params[0]);
                    if(!f.exists())
                        return null;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                    if(options.outWidth <= 200 && options.outHeight <= 200)
                        return f;

                    Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                    Bitmap t;
                    int w = b.getWidth();
                    int h = b.getHeight();
                    int s = Math.min(w, h);
                    if(s <= 200) {
                        if(s == w) {
                            t = Bitmap.createBitmap(w, 200, Bitmap.Config.ARGB_8888);
                            Canvas c = new Canvas(t);
                            c.drawBitmap(b, 0, (h - 200)/2, null);
                        }
                        else {
                            t = Bitmap.createBitmap(200, h, Bitmap.Config.ARGB_8888);
                            Canvas c = new Canvas(t);
                            c.drawBitmap(b, (w - 200)/2, 0, null);
                        }
                    }
                    else {
                        t = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(t);
                        Rect src;
                        if(s == w) {
                            src = new Rect(0, (h - w)/2, w, (h + w)/2);
                        }
                        else {
                            src = new Rect((w - h)/2, 0, (h + w)/2, h);
                        }
                        c.drawBitmap(b, src, new Rect(0, 0, 200, 200), null);
                    }
                    b.recycle();
                    File dir = getExternalCacheDir();
                    String name;
                    Bitmap.CompressFormat format;
                    if(t.hasAlpha()) {
                        name = "out.png";
                        format = Bitmap.CompressFormat.PNG;
                    }
                    else {
                        name = "out.jpg";
                        format = Bitmap.CompressFormat.JPEG;
                    }

                    File out = new File(dir, name);
                    try {
                        FileOutputStream fos = new FileOutputStream(out);
                        t.compress(format, 80, fos);
                        fos.close();
                    }
                    catch (IOException e) {
                        return null;
                    }
                    t.recycle();
                    return out;
                }

                @Override
                protected void onPostExecute(File file) {
                    setWaiting(false);
                    if(file != null) {
                        Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
                        mAvatarView.setImageBitmap(b);
                        mNewAvatar = file;
                    }
                }
            }.execute(path);
        }
    }

    private void setEditing(boolean editing) {
        if(editing) {
            mSaveView.setText("保存");
            mCancelView.setVisibility(View.VISIBLE);
            mNameView.setEnabled(true);
        }
        else {
            mSaveView.setText("编辑");
            mCancelView.setVisibility(View.GONE);
            mNameView.setEnabled(false);
        }
    }

    private void setWaiting(boolean show) {
        if(show) {
            if(mWaitingDialog == null) {
                mWaitingDialog = lite.navi.view.Utils.createWaitingDialog(this);
            }
            if(mWaitingDialog.isShowing())
                return;
            mWaitingDialog.show();
        }
        else if(mWaitingDialog != null && mWaitingDialog.isShowing()) {
            mWaitingDialog.dismiss();
        }
    }

    private void updateProfile() {
        final String name = mNameView.getText().toString().trim();
        if(TextUtils.isEmpty(name)) {
            Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if(name.equals(mName) && mNewAvatar == null) {
            //no change
            setEditing(false);
            return;
        }

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("devid", Build.SERIAL);
        params.put("name", name);
        Callback cb = new Callback() {
            @Override
            public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                setWaiting(false);
                if(error == null) {
                    JSONObject data = json.optJSONObject("data");
                    mName = data.optString("name");
                    mNameView.setText(name);
                    mAvatar = Urls.SERVER + json.optString("avatar");
                    if(mNewAvatar != null) {
                        mNewAvatar.delete();
                        mNewAvatar = null;
                    }
                    setEditing(false);
                }
            }
        };
        if(mNewAvatar == null) {
            Network.post(Urls.PROFILE, params, cb);
            setWaiting(true);
        }
        else {
            Network.postPhoto(Urls.PROFILE, params, "avatar", mNewAvatar, cb);
            setWaiting(true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JSONObject json = (JSONObject) parent.getItemAtPosition(position);

        String groupId = json.optString("groupId");
        BDLocation start = App.sApp.mLocation;
        if(start == null) {
            return;
        }
        joinGroup(start.getLatitude(), start.getLongitude(),
                start.getStreet() + start.getStreetNumber(),
                groupId);
    }

    private void joinGroup(double lat, double lng, String addr, String groupId) {
        mGroupId = groupId;
        mCurLat = lat;
        mCurLng = lng;
        mStartAddr = addr;
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("devid", Build.SERIAL);
        params.put("lat", String.valueOf(mCurLat));
        params.put("lng", String.valueOf(mCurLng));
        params.put("groupId", mGroupId);
        Network.post(Urls.GROUP_JOIN, params, new Callback() {
            @Override
            public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                if(error == null) {
                    JSONObject data = json.optJSONObject("data");
                    mDstLng = data.optDouble("stop_lng");
                    mDstLat = data.optDouble("stop_lat");
                    mStopAddr = data.optString("stop_addr");
                    BNRoutePlanNode sNode = new BNRoutePlanNode(
                            mCurLng, mCurLat, "当前位置", null,
                            BNRoutePlanNode.CoordinateType.BD09LL);

                    BNRoutePlanNode eNode = new BNRoutePlanNode(mDstLng, mDstLat,
                            mStopAddr, null,
                            BNRoutePlanNode.CoordinateType.BD09LL);
                    List<BNRoutePlanNode> list = new ArrayList<>();
                    list.add(sNode);
                    list.add(eNode);
                    BaiduNaviManager.getInstance().launchNavigator(
                            Team.this, list, 1, true, Team.this);
                }
                else {
                    setWaiting(false);
                    mDstLng = null;
                    mDstLat = null;
                    mCurLng = null;
                    mCurLat = null;
                    mStartAddr = null;
                    mStopAddr = null;
                    mGroupId = null;
                }
            }
        });
        setWaiting(true);
    }

    @Override
    public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
        mPtr.refreshComplete();
        if(error != null)
            return;

        JSONArray array = json.optJSONArray("data");
        ArrayList<JSONObject> list = mAdapter.mList;
        list.clear();
        if(array != null) {
            for(int i = 0; i < array.length(); i++)
                list.add(array.optJSONObject(i));
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onJumpToNavigator() {
        setWaiting(false);
        Intent intent = new Intent(Team.this, Navi.class);
        if(mStopAddr != null) {
            intent.putExtra(Navi.EXTRA_START_LNG, mCurLng);
            intent.putExtra(Navi.EXTRA_START_LAT, mCurLat);
            intent.putExtra(Navi.EXTRA_DST_LNG, mDstLng);
            intent.putExtra(Navi.EXTRA_DST_LAT, mDstLat);
            intent.putExtra(Navi.EXTRA_START, mStartAddr);
            intent.putExtra(Navi.EXTRA_STOP, mStopAddr);
            intent.putExtra(Navi.EXTRA_GROUP, mGroupId);
        }
        startActivity(intent);
        mDstLng = null;
        mDstLat = null;
        mCurLng = null;
        mCurLat = null;
        mStartAddr = null;
        mStopAddr = null;
        mGroupId = null;
    }

    @Override
    public void onRoutePlanFailed() {
        setWaiting(false);
        Toast.makeText(this, "路径规划失败", Toast.LENGTH_SHORT).show();
        mDstLng = null;
        mDstLat = null;
        mCurLng = null;
        mCurLat = null;
        mStartAddr = null;
        mStopAddr = null;
        mGroupId = null;
    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        if(bdLocation != null)
            App.sApp.mLocation = bdLocation;
        mSpinView.setVisibility(View.GONE);
        mSpinView.post(new Runnable() {
            @Override
            public void run() {
                mPtr.autoRefresh();
            }
        });
    }

    public static void notifyGroupChange() {
        if(sInstance != null)
            sInstance.mPtr.autoRefresh(false);
    }

    class Adapter extends BaseAdapter {
        ArrayList<JSONObject> mList = new ArrayList<>();
        private DisplayImageOptions mOptions;

        Adapter() {
            DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
            builder.cacheOnDisk(true)
                    .cacheInMemory(false)
                    .showImageForEmptyUri(R.drawable.def_avatar);
        }

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
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.item_team, parent, false);
            }
            JSONObject item = mList.get(position);
            String url = item.optString("avatar");
            if(!TextUtils.isEmpty(url))
                url = Urls.SERVER + url;
            ImageView iv = (ImageView) convertView.findViewById(R.id.avatar);
            ImageLoader.getInstance().displayImage(url, iv, mOptions);

            TextView tv = (TextView) convertView.findViewById(R.id.name);
            tv.setText(item.optString("name"));
            tv = (TextView) convertView.findViewById(R.id.distance);
            tv.setText(item.optString("distance") + "米以内");
            tv = (TextView) convertView.findViewById(R.id.num);
            tv.setText(item.optString("groupId"));
            tv = (TextView) convertView.findViewById(R.id.start);
            tv.setText(item.optString("start_addr"));
            tv = (TextView) convertView.findViewById(R.id.end);
            tv.setText(item.optString("stop_addr"));

            return convertView;
        }
    }
}
