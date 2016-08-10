package lite.navi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.navisdk.adapter.BNOuterLogUtil;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lite.navi.view.Utils;

/**
 * @author ylscat
 *         Date: 2016-07-25 13:05
 */
public class Main extends Activity implements
        BDLocationListener, View.OnClickListener,
        BaiduMap.OnMarkerClickListener,
        LocationListener, BaiduNaviManager.RoutePlanListener {
    private static final String TAG = "Main";

    private static final int SEARCH_ACTION = 1;
    private LocationClient mLocClient;
    private TextureMapView mMapView;
    private BaiduMap mMap;
    private boolean isFirstLoc = true;
    private BDLocation mLastLocation;
    private TextView mSearch;
    private ImageView mIcon;

    private Dialog mWaitingDialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mMapView = (TextureMapView) findViewById(R.id.bmapView);
        mMap = mMapView.getMap();
        mMap.setOnMarkerClickListener(this);

        mSearch = (TextView) findViewById(R.id.search);
        mSearch.setOnClickListener(this);
        findViewById(R.id.locate).setOnClickListener(this);
        mIcon = (ImageView) findViewById(R.id.icon);
        mIcon.setOnClickListener(this);
        // 开启定位图层
        mMap.setMyLocationEnabled(true);
        mMap.setMaxAndMinZoomLevel(19, 3);

        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);

//        registerLocListener();
        BNOuterLogUtil.setLogSwitcher(true);
        File dir = Environment.getExternalStorageDirectory();
        String name = getResources().getString(R.string.app_name);
        File f = new File(dir, name);
        if(f.exists() || f.mkdir()) {
            BaiduNaviManager.getInstance().init(this,
                    dir.toString(), name,
                    mNaviInitListener, null, null, null);
        }
    }

    private void registerLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        for(String p : locMgr.getAllProviders()) {
            locMgr.requestLocationUpdates(p, 1000, 0, this);
        }
    }

    private void unregisterLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locMgr.removeUpdates(this);
    }

    @Override
    public void onReceiveLocation(BDLocation location) {
        if (location == null || mMapView == null) {
            return;
        }
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(location.getDirection())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        mMap.setMyLocationData(locData);
        mLastLocation = location;
        ((App)getApplication()).mLocation = location;
        if (isFirstLoc) {
            isFirstLoc = false;
            moveToLocation(location, 18f);
        }
    }

    private void moveToLocation(BDLocation location, float zoom) {
        LatLng ll = new LatLng(location.getLatitude(),
                location.getLongitude());
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(ll);
        if(zoom > 0)
            builder.zoom(zoom);
        mMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:
                if(mLastLocation == null)
                    return;
                moveToLocation(mLastLocation, -1);
                break;
            case R.id.search:
                Intent intent = new Intent(this, Search.class);
                startActivityForResult(intent, SEARCH_ACTION);
                break;
            case R.id.icon:
                if(mSearch.getText().length() > 0) {
                    mSearch.setText(null);
                    mIcon.setImageResource(R.drawable.search);
                    mMap.clear();
                }
                else {
                    intent = new Intent(this, Search.class);
                    startActivityForResult(intent, SEARCH_ACTION);
                }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        routeTo(marker);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SEARCH_ACTION && resultCode == RESULT_OK) {
            mMap.clear();
            PoiInfo info = data.getParcelableExtra(Search.EXTRA_RESULT);
            if(info == null)
                return;
            mSearch.setText(info.name);
            mIcon.setImageResource(R.drawable.cross);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            MarkerOptions options = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromAssetWithDpi(
                            "Icon_mark1.png")).title(info.name)
                    .position(info.location);
            View marker = getLayoutInflater().inflate(R.layout.marker, null);
            TextView tv = (TextView) marker.findViewById(R.id.name);
            tv.setText(info.name);
            marker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int w = marker.getMeasuredWidth();
            int h = marker.getMeasuredHeight();
            marker.layout(0, 0, w, h);
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
//            c.drawColor(0xFFFFFFFF);
            marker.draw(c);
            MarkerOptions text = new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)).title(info.name)
                    .position(info.location)
                    .anchor(0.5f, 0);

            mMap.addOverlay(options);
            mMap.addOverlay(text);
            builder.include(options.getPosition());

            mMap.setMapStatus(MapStatusUpdateFactory
                    .newLatLngBounds(builder.build()));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        mLocClient.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        mLocClient.start();
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        super.onDestroy();
//        BaiduNaviManager.getInstance().uninit();
        // 关闭定位图层
        mMap.setMyLocationEnabled(false);
        mMapView.onDestroy();

        unregisterLocListener();
        ((App)getApplication()).mLocation = null;
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
    public void onLocationChanged(Location location) {
        Log.d("Prvd", "GotLoc");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void routeTo(Marker marker) {
        showWaitingDialog();

        BNRoutePlanNode sNode = new BNRoutePlanNode(mLastLocation.getLongitude(),
                mLastLocation.getLatitude(), "当前位置", null,
                BNRoutePlanNode.CoordinateType.BD09LL);

        BNRoutePlanNode eNode = new BNRoutePlanNode(marker.getPosition().longitude,
                marker.getPosition().latitude, marker.getTitle(), null,
                BNRoutePlanNode.CoordinateType.BD09LL);
        List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
        list.add(sNode);
        list.add(eNode);
        BaiduNaviManager.getInstance().launchNavigator(this, list, 1, true, this);
    }

    private void initSetting(){
        BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        BNaviSettingManager.setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Novice);
        BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_OFF);
    }

    BaiduNaviManager.NaviInitListener mNaviInitListener = new BaiduNaviManager.NaviInitListener() {
        @Override
        public void onAuthResult(int status, String msg) {
            if (0 == status) {
                Log.d(TAG, "key校验成功!");
            } else {
                Log.d(TAG, "key校验失败, " + msg);
            }
        }

        public void initSuccess() {
            Log.d(TAG, "百度导航引擎初始化成功");
            initSetting();
        }

        public void initStart() {
            Log.d(TAG, "百度导航引擎初始化开始");
        }

        public void initFailed() {
            Log.e(TAG, "百度导航引擎初始化失败");
        }
    };

    @Override
    public void onJumpToNavigator() {
        dismissWaitingDialog();
        Intent intent = new Intent(this, Navi.class);
        startActivity(intent);
    }

    @Override
    public void onRoutePlanFailed() {
        dismissWaitingDialog();
        Toast.makeText(this, "路径规划失败", Toast.LENGTH_SHORT).show();
    }
}
