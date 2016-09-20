package lite.navi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.FileTileProvider;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.map.Tile;
import com.baidu.mapapi.map.TileOverlay;
import com.baidu.mapapi.map.TileOverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.navisdk.adapter.BNOuterLogUtil;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.esri.android.map.DynamicLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnPanListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureFillSymbol;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.zhiyu.mirror.GpsTestActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
        LocationListener,
        BaiduNaviManager.RoutePlanListener {
    private static final String TAG = "Main";

    private static final int SEARCH_ACTION = 1;
    private LocationClient mLocClient;
    private TextureMapView mMapView;
    private BaiduMap mMap;
    private boolean isFirstLoc = true;
    private BDLocation mLastLocation;
    private TextView mSearch;
    private ImageView mIcon;
    private MyTileProvider mTileProvider;

    private Dialog mWaitingDialog;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_TRACE = 1;
    private int mMode = MODE_NORMAL;

    private MapView mArcMap;
    private GraphicsLayer mGraphicsLayer;
    private SimpleMarkerSymbol mDot;
    private int mLocMarker = -1;
    private TextView mTraceButton;

    private Location mArcMapLocation;
    private Point mLastDot;
    private boolean isTracing = true;
    private boolean autoCenter = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.tv_settings).setOnClickListener(this);
        findViewById(R.id.iv_settings).setOnClickListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        mMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.setOnMapLoadedCallback(null);
                addTileLayer();
            }
        });

        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setNeedDeviceDirect(true);
        option.setIsNeedAddress(true);
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);

        BNOuterLogUtil.setLogSwitcher(true);
        File dir = Environment.getExternalStorageDirectory();
        String name = getResources().getString(R.string.app_name);
        File f = new File(dir, name);
        if(f.exists() || f.mkdir()) {
            BaiduNaviManager.getInstance().init(this,
                    dir.toString(), name,
                    mNaviInitListener, null, null, null);
        }

        mArcMap = (MapView) findViewById(R.id.arc_map);
        findViewById(R.id.mode).setOnClickListener(this);
        findViewById(R.id.reset).setOnClickListener(this);
        mTraceButton = (TextView) findViewById(R.id.trace);
        mTraceButton.setOnClickListener(this);
        mTraceButton.setActivated(isTracing);

        ArcGISTiledMapServiceLayer base = new ArcGISTiledMapServiceLayer(
//                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer");
        mArcMap.addLayer(base);
                //Envelope [m_envelope=Envelope2D [xmin=114.3272189510811, ymin=30.542809291954995, xmax=114.34153905102028, ymax=30.56307135368746], m_attributes=null]
//        DynamicLayer dl = new ArcGISDynamicMapServiceLayer("http://121.40.231.209:6080/arcgis/rest/services/MiddleNorthRoad/MapServer");
        DynamicLayer dl = new ArcGISDynamicMapServiceLayer("http://121.40.231.209:6080/arcgis/rest/services/TMRITest/MapServer");
        mArcMap.addLayer(dl);
        mGraphicsLayer = new GraphicsLayer();
        mArcMap.addLayer(mGraphicsLayer);
        float size = getResources().getDisplayMetrics().density*15;
        mDot = new SimpleMarkerSymbol(Color.RED, (int)size,
                SimpleMarkerSymbol.STYLE.CIRCLE);

        mArcMap.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {
                if(status == STATUS.INITIALIZED) {
                    SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
                    mArcMap.setScale(5000);
                    double x = sp.getFloat("position_x", (float)(114.3272189510811 + 114.34153905102028)/2);
                    double y = sp.getFloat("position_y", (float)(30.542809291954995 + 30.56307135368746)/2);
                    Point pt = GeometryEngine.project(x, y, mArcMap.getSpatialReference());
                    mArcMap.centerAt(pt, false);
                    mArcMap.setOnStatusChangedListener(null);
                    Drawable d = getResources().getDrawable(R.drawable.loc_dot);
                    assert d != null;
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    PictureMarkerSymbol symbol = new PictureMarkerSymbol(d);
                    mLocMarker = mGraphicsLayer.addGraphic(new Graphic(pt, symbol));
                    mGraphicsLayer.updateGraphic(mLocMarker, mGraphicsLayer.getMaxDrawOrder() + 1);
                }
            }
        });
        mArcMap.setOnPanListener(new OnPanListener() {
            @Override
            public void prePointerMove(float v, float v1, float v2, float v3) {

            }

            @Override
            public void postPointerMove(float v, float v1, float v2, float v3) {

            }

            @Override
            public void prePointerUp(float v, float v1, float v2, float v3) {

            }

            @Override
            public void postPointerUp(float v, float v1, float v2, float v3) {
                autoCenter = false;
            }
        });
    }

    private void registerLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        for(String p : locMgr.getAllProviders()) {
//            locMgr.requestLocationUpdates(p, 1000, 0, this);
//        }
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 0, this);
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
                if(mMode == MODE_TRACE) {
                    if(mArcMapLocation != null){
                        mArcMap.centerAt(mArcMapLocation.getLatitude(),
                                mArcMapLocation.getLongitude(), true);
                        autoCenter = true;
                    }
                    return;
                }
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
                break;
            case R.id.mode:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setSingleChoiceItems(new String[]{"百度地图", "ArcGIS地图"},
                        mMode == MODE_NORMAL ? 0 : 1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if(which == 0 ^ mMode == MODE_NORMAL) {
                                    setMode(which == 0 ? MODE_NORMAL : MODE_TRACE);
                                }
                            }
                        });
                builder.create().show();
                break;
            case R.id.reset:
                clearDots();
                break;
            case R.id.trace:
                isTracing = !isTracing;
                mTraceButton.setActivated(isTracing);
                break;
            case R.id.iv_settings:
            case R.id.tv_settings:
                intent = new Intent(this, GpsTestActivity.class);
                startActivity(intent);
                break;
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
        if(mMode == MODE_TRACE)
            unregisterLocListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mWaitingDialog != null && mWaitingDialog.isShowing()) {
            mWaitingDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        mLocClient.start();
        if(mMode == MODE_TRACE)
            registerLocListener();
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        super.onDestroy();
//        BaiduNaviManager.getInstance().uninit();
        // 关闭定位图层
        mMap.setMyLocationEnabled(false);
        mMapView.onDestroy();

        if(mTileProvider != null) {
            mTileProvider.mTile.recycle();
            mTileProvider.mBitmap.recycle();
            mTileProvider = null;
        }
        ((App)getApplication()).mLocation = null;
        SharedPreferences sp = getSharedPreferences("SP", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if(mMode == MODE_TRACE && mArcMapLocation != null) {
            editor.putFloat("position_x", (float)mArcMapLocation.getLongitude());
            editor.putFloat("position_y", (float)mArcMapLocation.getLatitude());
            editor.apply();
        }
        else if(mLastLocation != null) {
            editor.putFloat("position_x", (float)mLastLocation.getLongitude());
            editor.putFloat("position_y", (float)mLastLocation.getLatitude());
            editor.apply();
        }
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
        if(lite.navi.Utils.isBetterLocation(location, mArcMapLocation)) {
            moveToLocationOnArcMap(location);
        }
    }

    private void moveToLocationOnArcMap(Location location) {
        final double y = location.getLatitude();
        final double x = location.getLongitude();

        SpatialReference sr = mArcMap.getSpatialReference();
        Point pt = GeometryEngine.project(x, y, sr);
        if(mLocMarker != -1) {
            mGraphicsLayer.updateGraphic(mLocMarker, pt);
        }

        if(mArcMapLocation == null || autoCenter) {
            mArcMap.centerAt(y, x, true);
        }

        if(!isTracing) {

            mArcMapLocation = location;
            return;
        }


        if(mLastDot == null || mArcMapLocation == null) {
            mLastDot = pt;
        } else {
            pt = GeometryEngine.project(mArcMapLocation.getLongitude(),
                    mArcMapLocation.getLatitude(), sr);
            double distance = GeometryEngine.geodesicDistance(mLastDot, pt, sr, null);
            Log.d("DIS:", distance + "");
            if (distance > 10) {
                mGraphicsLayer.addGraphic(new Graphic(pt, mDot));
                mLastDot = pt;
            }
        }
        mArcMapLocation = location;
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
//        dismissWaitingDialog();
        Intent intent = new Intent(this, Navi.class);
        startActivity(intent);
    }

    @Override
    public void onRoutePlanFailed() {
        dismissWaitingDialog();
        Toast.makeText(this, "路径规划失败", Toast.LENGTH_SHORT).show();
    }

    private void addTileLayer() {
        mTileProvider = new MyTileProvider();
        TileOverlayOptions options = new TileOverlayOptions();
        // 构造显示瓦片图范围，当前为世界范围
        LatLng northeast = new LatLng(MyTileProvider.LAT_MAX, MyTileProvider.LON_MAX);
        LatLng southwest = new LatLng(MyTileProvider.LAT_MIN, MyTileProvider.LON_MIN);
        // 设置离线瓦片图属性option
        options.tileProvider(mTileProvider)
                .setPositionFromBounds(new LatLngBounds.Builder().include(northeast).include(southwest).build());
        // 通过option指定相关属性，向地图添加离线瓦片图对象
        TileOverlay tileOverlay = mMap.addTileLayer(options);
        mMap.setMaxAndMinZoomLevel(19, 3);
    }

    private void setMode(int mode) {
        if(mode == mMode)
            return;
        switch (mode) {
            case MODE_NORMAL:
                unregisterLocListener();
                ((View)mArcMap.getParent()).setVisibility(View.GONE);
                mArcMapLocation = null;
                break;
            case MODE_TRACE:
                registerLocListener();
                ((View)mArcMap.getParent()).setVisibility(View.VISIBLE);
                clearDots();
                break;
        }
        mMode = mode;
    }

    private void clearDots() {
        int[] ids = mGraphicsLayer.getGraphicIDs();
        if(ids != null) {
            Log.d("ARC", "Clear " + (ids.length - 1));
            for(int id : ids) {
                if(id != mLocMarker)
                    mGraphicsLayer.removeGraphic(id);
            }
        }
    }

    class MyTileProvider extends FileTileProvider {

        final static int SIZE = 256;
        final static int LEVEL = 19;
        final static int X_MIN = 12728316*2;
        final static int X_MAX = 12729906*2;
        final static int Y_MIN = 3552073*2;
        final static int Y_MAX = 3554712*2;
        final static int W = X_MAX - X_MIN;
        final static int H = Y_MAX - Y_MIN;

        final static double LAT_MAX = 30.567009;
        final static double LAT_MIN = 30.546484;
        final static double LON_MAX = 114.353699;
        final static double LON_MIN = 114.339119;

        Bitmap mBitmap, mTile;
        Canvas mCanvas;
        Rect mSrc = new Rect();
        Rect mDst = new Rect(0, 0, SIZE, SIZE);

        public MyTileProvider() {
            AssetManager am = getAssets();
            try {
                InputStream is = am.open("L19.png");
                mBitmap = BitmapFactory.decodeStream(is);
                is.close();
                mTile = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mTile);
            }
            catch (IOException e) {
                //never happen
            }
        }

        @Override
        public Tile getTile(int x, int y, int z) {
            if(z < 16)
                return null;
            int scale = (int)Math.pow(2, LEVEL - z);
            Rect src = mSrc;
            Rect dst = mDst;
            final int STRIDE = SIZE*scale;
            src.set(x*STRIDE, y*STRIDE, (x + 1)*STRIDE, (y + 1)*STRIDE);

            int result = getOverlap(src, dst, scale);
            if(result == -1) {
                return null;
            }

            mTile.eraseColor(0);
            mCanvas.drawBitmap(mBitmap, src, dst, null);
            ByteBuffer buffer = ByteBuffer.allocate(256
                    * 256 * 4);
            mTile.copyPixelsToBuffer(buffer);
            byte[] data = buffer.array();
            buffer.clear();
            return new Tile(256, 256, data);
        }

        private boolean getSrc(Rect src) {
            if(src.left >= X_MIN && src.right <= X_MAX &&
                    src.top >= Y_MIN && src.bottom <= Y_MAX) {
                src.left = src.left - X_MIN;
                src.right = src.right - X_MIN;
                int t = src.top;
                src.top = Y_MAX - src.bottom;
                src.bottom = Y_MAX - t;
                return true;
            }

            return false;
        }

        private int getOverlap(Rect src, Rect dst, int scale) {
            if(src.left >= X_MAX)
                return -1;
            if(src.right <= X_MIN)
                return -1;
            if(src.top >= Y_MAX)
                return -1;
            if(src.bottom <= Y_MIN)
                return -1;
            int result = 4;
            if(src.right > X_MAX) {
                result--;
                dst.right = (X_MAX - src.left)/scale;
                src.right = W;
            }
            else {
                dst.right = SIZE;
                src.right = src.right - X_MIN;
            }

            if(src.left < X_MIN) {
                result--;
                dst.left = (X_MIN - src.left)/scale;
                src.left = 0;
            }
            else {
                src.left = src.left - X_MIN;
                dst.left = 0;
            }

            int b;
            if(src.top < Y_MIN) {
                result--;
                dst.bottom = (src.bottom - Y_MIN)/scale;
                b = H;
            }
            else {
                dst.bottom = SIZE;
                b = Y_MAX - src.top;
            }

            if(src.bottom > Y_MAX) {
                result--;
                dst.top = (src.bottom - Y_MAX)/scale;
                src.top = 0;
            }
            else {
                dst.top = 0;
                src.top = Y_MAX - src.bottom;
            }

            src.bottom = b;

            return result;
        }

        @Override
        public int getMaxDisLevel() {
            return LEVEL;
        }

        @Override
        public int getMinDisLevel() {
            return 16;
        }
    }
}
