package lite.navi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRouteGuideManager.OnNavigationListener;
import com.baidu.navisdk.adapter.BNaviBaseCallbackModel;
import com.baidu.navisdk.adapter.BaiduNaviCommonModule;
import com.baidu.navisdk.adapter.NaviModuleFactory;
import com.baidu.navisdk.adapter.NaviModuleImpl;
import com.baidu.nplatform.comapi.basestruct.GeoPoint;
import com.baidu.nplatform.comapi.map.ItemizedOverlay;
import com.baidu.nplatform.comapi.map.MapGLSurfaceView;
import com.baidu.nplatform.comapi.map.OverlayItem;
import com.esri.android.map.DynamicLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;
import com.whld.network.volley.Callback;
import com.whld.network.volley.Network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import lite.navi.network.Urls;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * 诱导界面
 * 
 * @author sunhao04
 *
 */
public class Navi extends Activity implements
        LocationListener,
        BDLocationListener,
        Callback, View.OnClickListener {
    public static final String EXTRA_START_LNG = "start_lng";
    public static final String EXTRA_START_LAT = "start_lat";
    public static final String EXTRA_DST_LNG = "dst_lng";
    public static final String EXTRA_DST_LAT = "dst_lat";
    public static final String EXTRA_START = "start";
    public static final String EXTRA_STOP = "stop";
    public static final String EXTRA_GROUP = "group";

    private double mDstLng, mDstLat, mStartLng, mStartLat;
    private String mStartAddr, mStopAddr;
    private String mGroupId;
    private LinkedHashMap<String, Member> mMembers = new LinkedHashMap<>();
    static final int SYNC_INTERVAL = 2000;

    private final String TAG = Navi.class.getName();
    private BaiduNaviCommonModule mBaiduNaviCommonModule = null;

    private LinearLayout mMemberBar;
    private TextView mGroupIdView;
    private View mShareButton;
    private DisplayImageOptions mDisplayImageOptions;

    private MapGLSurfaceView mMapView;
    private ItemizedOverlay mLayer;

    private LocationClient mLocClient;
    private BDLocation mBDLocation, mTempLoc = new BDLocation();

    /*
     * 对于导航模块有两种方式来实现发起导航。 1：使用通用接口来实现 2：使用传统接口来实现
     */
    // 是否使用通用接口
    private boolean useCommonInterface = true;

    private MapView mArcMap;
    private GraphicsLayer mGraphicsLayer;
    private SimpleMarkerSymbol mDot;
    private int mLocMarker;
    private Location mCurrentLocation;
    private Point mLastDot;
    private Location mMockLocation = new Location(LocationManager.GPS_PROVIDER);
    private Location mMockLocation2 = new Location(LocationManager.GPS_PROVIDER);
    private boolean isMocking;

    //显示高精度地图的敏感区域(WGS84坐标)
    private double Xmin = 114.3272189510811;
    private double Xmax = 114.34153905102028;
    private double Ymin = 30.542809291954995;
    private double Ymax = 30.56307135368746;
    //当前是否在
    private boolean isInRegion = false;

    private static final SpatialReference WGS84 = SpatialReference.create(
            SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mStartAddr = intent.getStringExtra(EXTRA_START);
        mStopAddr = intent.getStringExtra(EXTRA_STOP);
        mStartLng = intent.getDoubleExtra(EXTRA_START_LNG, 0);
        mStartLat = intent.getDoubleExtra(EXTRA_START_LAT, 0);
        mDstLng = intent.getDoubleExtra(EXTRA_DST_LNG, 0);
        mDstLat = intent.getDoubleExtra(EXTRA_DST_LAT, 0);
        mGroupId = intent.getStringExtra(EXTRA_GROUP);

        View view = null;
        if (useCommonInterface) {
            //使用通用接口
            mBaiduNaviCommonModule = NaviModuleFactory.getNaviModuleManager().getNaviCommonModule(
                    NaviModuleImpl.BNaviCommonModuleConstants.ROUTE_GUIDE_MODULE, this,
                    BNaviBaseCallbackModel.BNaviBaseCallbackConstants.CALLBACK_ROUTEGUIDE_TYPE,
                    mOnNavigationListener);
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onCreate();
                view = mBaiduNaviCommonModule.getView();
            }

        } else {
            //使用传统接口
            view = BNRouteGuideManager.getInstance().onCreate(this,mOnNavigationListener);
        }
        setContentView(view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGraphicsLayer = new GraphicsLayer();
        float size = getResources().getDisplayMetrics().density*5;
        mDot = new SimpleMarkerSymbol(Color.RED, (int)size,
                SimpleMarkerSymbol.STYLE.CIRCLE);

//        mArcMap.setOnStatusChangedListener(new OnStatusChangedListener() {
//            @Override
//            public void onStatusChanged(Object o, STATUS status) {
//                if(status == STATUS.INITIALIZED) {
//                    Point p = new Point((Xmin + Xmax)/2, (Ymin + Ymax)/2);
//                    mArcMap.centerAt(p, false);
//                    final SpatialReference wm = SpatialReference.create(102100);
//                    final SpatialReference egs = SpatialReference.create(4326);
//                    mArcMap.zoomToResolution(p, 0.000015);
//                }
//            }
//        });

        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setNeedDeviceDirect(false);
        option.setIsNeedAddress(false);
        option.setScanSpan(SYNC_INTERVAL);
        mLocClient.setLocOption(option);
        mBDLocation = App.sApp.mLocation;

        FrameLayout fl = (FrameLayout)findViewById(android.R.id.content);

        //模拟区域的中心点
        mMockLocation.setLatitude((Ymin + Ymax)/2);
        mMockLocation.setLongitude((Xmin + Xmax)/2);
        //模拟区域外一点
        mMockLocation2.setLatitude((Ymin + Ymax)/2);
        mMockLocation2.setLongitude((Xmin + Xmax)/2 + 1);

        mMapView = (MapGLSurfaceView) ((FrameLayout)view).getChildAt(0);
        mLayer = new ItemizedOverlay(null, mMapView);
        mMapView.addOverlay(mLayer);

        View bar = getLayoutInflater().inflate(R.layout.navi_bar, fl, false);
        fl.addView(bar);
        bar.findViewById(R.id.mock).setOnClickListener(this);
        mShareButton = bar.findViewById(R.id.share);
        mShareButton.setOnClickListener(this);
        mGroupIdView = (TextView) bar.findViewById(R.id.group_id);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setBackgroundResource(R.drawable.round_corner_dark);
        LinearLayout group = new LinearLayout(this);
        hsv.addView(group, WRAP_CONTENT, WRAP_CONTENT);
        mMemberBar = group;
        float dp = getResources().getDisplayMetrics().density;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final int MARGIN = (int)(70*dp);
        group.setHorizontalFadingEdgeEnabled(true);
        final int PADDING = (int)(5*dp);
        group.setPadding(PADDING, PADDING, PADDING, PADDING);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                screenWidth*2/3 - 2*MARGIN,
                WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT);
        lp.setMargins(0, 0, MARGIN, (int)(3*dp));
        fl.addView(hsv, lp);

        setupGroupBar();
        if(mGroupId != null)
            mMemberBar.post(mSync);

        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        builder.cacheOnDisk(true);
        builder.cacheInMemory(true);
        builder.preProcessor(new CircleClipper());
        mDisplayImageOptions = builder.build();
    }

    private void registerLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        for(String provider : locMgr.getAllProviders()) {
            Log.d("Prv", provider);
        }
        //ArcGIS图层定位,只使用GPS
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        mLocClient.start();
    }

    private void unregisterLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locMgr.removeUpdates(this);
        mLocClient.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mArcMap != null)
            mArcMap.unpause();
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onResume();
            }
        } else {
            BNRouteGuideManager.getInstance().onResume();
        }

        registerLocListener();
    }

    protected void onPause() {
        super.onPause();

        if(mArcMap != null)
            mArcMap.pause();
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onPause();
            }
        } else {
            BNRouteGuideManager.getInstance().onPause();
        }

        unregisterLocListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLayer.removeAll();
        mMapView.removeOverlay(mLayer);
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onDestroy();
            }
        } else {
            BNRouteGuideManager.getInstance().onDestroy();
        }
        if(mGroupId != null) {
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("devid", Build.SERIAL);
            params.put("groupId", mGroupId);
            Network.request(Urls.GROUP_QUIT, params, new Callback() {
                @Override
                public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                    mGroupId = null;
                    Team.notifyGroupChange();
                }
            });
        }
        mMemberBar.removeCallbacks(mSync);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onStop();
            }
        } else {
            BNRouteGuideManager.getInstance().onStop();
        }
       
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onConfigurationChanged(newConfig);
            }
        } else {
            BNRouteGuideManager.getInstance().onConfigurationChanged(newConfig);
        }

    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        mBDLocation = bdLocation;
        App.sApp.mLocation = bdLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        //如果当前是模式导航模式,就不处理GPS定位信息
        if(isMocking)
            return;
        if(Utils.isBetterLocation(location, mCurrentLocation)) {
//            mMockLocation.setLongitude(mMockLocation.getLongitude() + 0.002);
//            Log.d("Loc", String.format("%f %f", mMockLocation.getLongitude(), mMockLocation.getLatitude()));
            setCurrentLocation(location);
            mCurrentLocation = location;

            //todo delete below line
//            mCurrentLocation = location;
        }
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

    private void setCurrentLocation(Location location) {
        final double y = location.getLatitude();
        final double x = location.getLongitude();
//        final double y = (Ymax + Ymin)/2 + (Ymax - Ymin)*0.8*Math.random();
//        final double x = (Xmax + Xmin)/2 + (Xmax - Xmin)*0.8*Math.random();
        if(inRegion(x, y)) {  //进入敏感区域
            if(!isInRegion) {  //上次不在区域里
                mArcMap = createArcMap();
                FrameLayout fl = (FrameLayout) findViewById(android.R.id.content);
                fl.addView(mArcMap);
                mArcMap.unpause();
                isInRegion = true;
                mGraphicsLayer.removeAll();

                mArcMap.setOnStatusChangedListener(new OnStatusChangedListener() {
                    @Override
                    public void onStatusChanged(Object o, STATUS status) {
                        if(status == STATUS.INITIALIZED) {
                            mArcMap.setScale(5000);
                            mArcMap.centerAt(y, x, false);
                            mArcMap.setOnStatusChangedListener(null);
                            Point pt = GeometryEngine.project(x, y, WGS84);
                            mLocMarker = mGraphicsLayer.addGraphic(new Graphic(pt, mDot));
                            mLastDot = pt;
                        }
                    }
                });

            }
            else {
                mArcMap.setScale(5000);  //设置适当的缩放
                mArcMap.centerAt(y, x, true); //把ArcGIS地图移动到当前位置
                SpatialReference sr = mArcMap.getSpatialReference();
                Point pt = GeometryEngine.project(x, y, WGS84);
                mGraphicsLayer.updateGraphic(mLocMarker, pt); //标示当前位置
//                double distance = mLastDot == null ? 10 :
//                        GeometryEngine.geodesicDistance(mLastDot, pt, sr, null);
//                Log.d("DIS:", distance + "");
//                if(distance > 10) {
//                    mGraphicsLayer.addGraphic(new Graphic(pt, mDot));
//                    mLastDot = pt;
//                }
            }
        }
        else {
            if(isInRegion) {
                try {
                    mArcMap.removeLayer(mGraphicsLayer);
                }
                catch (RuntimeException e) {
                    //ignore
                }
                FrameLayout fl = (FrameLayout)mArcMap.getParent();
                fl.removeView(mArcMap);
                mArcMap = null;
                isInRegion = false;
            }
        }
    }

    /**
     * 是否处于敏感区域中
     * @param x 经度
     * @param y 纬度
     * @return 在区域里
     */
    private boolean inRegion(double x, double y) {
//        return true;
        return x >= Xmin && x <= Xmax && y >= Ymin && y <= Ymax;
    }

    private OnNavigationListener mOnNavigationListener = new OnNavigationListener() {

        @Override
        public void onNaviGuideEnd() {
            finish();
        }

        @Override
        public void notifyOtherAction(int actionType, int arg1, int arg2, Object obj) {

            if (actionType == 0) {
                Log.i(TAG, "notifyOtherAction actionType = " + actionType + ",导航到达目的地！");
            }

            Log.i(TAG, "actionType:" + actionType + "arg1:" + arg1 + "arg2:" + arg2 + "obj:" + obj.toString());
        }

    };

    private MapView createArcMap() {
        MapView mapView = new MapView(this);
        if(isMocking)
            mapView.setOnTouchListener(null);

        //ArcGIS底图图层,必须添加才能正常显示
        ArcGISTiledMapServiceLayer base = new ArcGISTiledMapServiceLayer(
                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
//                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer");
        mapView.addLayer(base);
        //需要要求不要看到底图
        base.setVisible(false);
        //Envelope [m_envelope=Envelope2D [xmin=114.3272189510811, ymin=30.542809291954995, xmax=114.34153905102028, ymax=30.56307135368746], m_attributes=null]
        //高精度地图图层
        DynamicLayer dl = new ArcGISDynamicMapServiceLayer("http://121.40.231.209:6080/arcgis/rest/services/MiddleNorthRoad/MapServer");
        mapView.addLayer(dl);
        mapView.addLayer(mGraphicsLayer);

//        mArcMap.setOnStatusChangedListener(new OnStatusChangedListener() {
//            @Override
//            public void onStatusChanged(Object o, STATUS status) {
//                if(status == STATUS.INITIALIZED) {
//                    Point p = new Point((Xmin + Xmax)/2, (Ymin + Ymax)/2);
//                    mArcMap.centerAt(p, false);
//                    final SpatialReference wm = SpatialReference.create(102100);
//                    final SpatialReference egs = SpatialReference.create(4326);
//                    mArcMap.zoomToResolution(p, 0.000015);
//                }
//            }
//        });

        for(int i = 0; i < mapView.getChildCount(); i++) {
            View child = mapView.getChildAt(i);
            if(child instanceof GLSurfaceView) {
                GLSurfaceView glView = (GLSurfaceView)child;
                glView.setZOrderOnTop(true); //设置ZOrder, 让SurfaceView叠加不会产生黑块显示问题
            }
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int w = metrics.widthPixels/3;
        int h = metrics.heightPixels;
        mapView.setLayoutParams(new FrameLayout.LayoutParams(w, h));
        return mapView;
    }

    @Override
    public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
        if(error != null)
            return;
        if(mGroupId == null)
            return;
        JSONArray array = json.optJSONArray("data");
        if(array == null || array.length() == 0) {
            mGroupId = null;
            setupGroupBar();
            mMemberBar.removeCallbacks(mSync);
        }
        updateMembers(array);
    }

    ImageLoadingListener mImageListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {

        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            onLoadingComplete(imageUri, view, null);
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            String tag = (String) ((View)view.getParent()).getTag();
            Member member = mMembers.get(tag);
            if(member != null) {
                member.avatarDrawable = ((ImageView) view).getDrawable();
                if(member.shouldNotify)
                    toastInfo(String.format("%s 加入群组", member.name), member.avatarDrawable);
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {

        }
    };

    @SuppressWarnings("NumberEquality")
    private void updateMembers(JSONArray array) {
        if(array == null || array.length() == 0) {
            mMembers.clear();
            setupCustomerLayer();
            return;
        }

        HashSet<String> toRemove = new HashSet<>(mMembers.keySet());
        boolean firstTime = mMembers.size() == 0;
        for(int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            String id = item.optString("devid");
            Double lat = item.optDouble("lat", 0);;
            if(lat == 0)
                lat = null;
            Double lng = item.optDouble("lng", 0);;
            if(lng == 0)
                lng = null;

            Member member = mMembers.get(id);

            if(member != null) {
                if(lat != null && lat.equals(member.lat) || lat == member.lat) {
                    member.isUpdated = false;
                }
                else {
                    member.lat = lat;
                    member.isUpdated = true;
                }

                if(lng != null && lng.equals(member.lng) || lng == member.lng) {
                    member.isUpdated = false;
                }
                else {
                    member.lng = lng;
                    member.isUpdated = true;
                }
                toRemove.remove(id);
            }
            else {
                member = new Member();
                member.devId = id;
                member.name = item.optString("name");
                String avatar = item.optString("avatar");
                if(!TextUtils.isEmpty(avatar) && !avatar.startsWith("/"))
                    avatar = "/" + avatar;
                member.avatar = avatar;
                member.name = item.optString("name");
                mMembers.put(id, member);
                member.isUpdated = true;
                if(!firstTime)
                    member.shouldNotify = true;
            }
        }

        //删除退出的成员
        for(String key : toRemove) {
            Member member = mMembers.get(key);
            mMembers.remove(key);
            if(member != null) {
                toastInfo(String.format("%s 退出群组", member.name), member.avatarDrawable);
            }
        }

        //更新成员列表
        ArrayList<Member> members = new ArrayList<>(mMembers.values());
        int index;
        for(index = 0; index < mMemberBar.getChildCount(); index++) {
            View view = mMemberBar.getChildAt(index);
            if(index < members.size()) {
                Member member = members.get(index);
                setupMember(view, member);
            }
            else {
                break;
            }
        }

        if(index < mMemberBar.getChildCount()) {
            for (int i = mMemberBar.getChildCount() - 1; i >= index; i--) {
                mMemberBar.removeViewAt(i);
            }
        }
        else {
            LayoutInflater inflater = getLayoutInflater();
            while (index < members.size()) {
                View view = inflater.inflate(R.layout.item_member, mMemberBar, false);
                mMemberBar.addView(view);
                setupMember(view, members.get(index++));
            }
        }

        //更新自定义图层
        setupCustomerLayer();
    }

    private void setupCustomerLayer() {
        ArrayList<OverlayItem> items = new ArrayList<>(mLayer.getAllItem());
        for(Member member : mMembers.values()) {
            if(member.lat != null && member.lng != null
                    && member.avatarDrawable != null
                    && !Build.SERIAL.equals(member.devId)) {
                OverlayItem overlay = member.mOverlay;
                if(overlay == null) {
                    mTempLoc.setLongitude(member.lng);
                    mTempLoc.setLatitude(member.lat);
                    BDLocation gcj = LocationClient.getBDLocationInCoorType(
                            mTempLoc, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                    BDLocation bd09 = LocationClient.getBDLocationInCoorType(
                            gcj, BDLocation.BDLOCATION_GCJ02_TO_BD09);
                    GeoPoint p = new GeoPoint((int)bd09.getLongitude(), (int)bd09.getLatitude());
                    overlay = new OverlayItem(p, "", "");
                    overlay.setCoordType(OverlayItem.CoordType.CoordType_BD09LL);
                    overlay.setMarker(member.avatarDrawable);
                    member.mOverlay = overlay;
                    mLayer.addItem(overlay);
                    member.isUpdated = false;
                }
                else {
                    if(member.isUpdated) {
                        GeoPoint p = overlay.getPoint();
                        mTempLoc.setLongitude(member.lng);
                        mTempLoc.setLatitude(member.lat);
                        BDLocation gcj = LocationClient.getBDLocationInCoorType(
                                mTempLoc, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                        BDLocation bd09 = LocationClient.getBDLocationInCoorType(
                                gcj, BDLocation.BDLOCATION_GCJ02_TO_BD09);

                        p.setLongitudeE6((int) bd09.getLongitude());
                        p.setLatitudeE6((int) bd09.getLatitude());
                        mLayer.updateItem(overlay);
                        member.isUpdated = false;
                    }
                    items.remove(overlay);
                }
            }
        }
        if(items.size() > 0) {
            for(OverlayItem item : items)
                mLayer.removeItem(item);
        }
        mMapView.refresh(mLayer);
        mMapView.requestRender();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mock:
                if(isMocking) {
                    //模拟进入区域 以显示高精度地图
                    setCurrentLocation(mMockLocation2);
                    setPreview(true);
                    v.setActivated(false);
                }
                else {
                    //模拟离开区域 以关闭高精度地图
                    setCurrentLocation(mMockLocation);
                    setPreview(false);
                    v.setActivated(true);
                }
                isMocking = !isMocking;
                break;
            case R.id.share:
                if(mGroupId == null) {
                    LinkedHashMap<String, String> params = new LinkedHashMap<>();
                    params.put("devid", Build.SERIAL);
                    params.put("start_lng", String.valueOf(mStartLng));
                    params.put("start_lat", String.valueOf(mStartLat));
                    params.put("stop_lng", String.valueOf(mDstLng));
                    params.put("stop_lat", String.valueOf(mDstLat));
                    params.put("start_addr", mStartAddr);
                    params.put("stop_addr", mStopAddr);
                    Network.post(Urls.GROUP_CREATE, params, new Callback() {
                        @Override
                        public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                            if(error == null) {
                                mGroupId = json.optJSONObject("data")
                                        .optString("groupId");
                                setupGroupBar();
                                mMemberBar.post(mSync);
                            }
                        }
                    });
                    break;
                }
                else {
                    LinkedHashMap<String, String> params = new LinkedHashMap<>();
                    params.put("devid", Build.SERIAL);
                    params.put("groupId", mGroupId);
                    Network.request(Urls.GROUP_QUIT, params, new Callback() {
                        @Override
                        public void onResponse(JSONObject json, Map<String, String> headers, VolleyError error) {
                            if(error == null) {
                                mGroupId = null;
                                mMemberBar.removeCallbacks(mSync);
                                setupGroupBar();
                                setupCustomerLayer();
                            }
                        }
                    });
                    break;
                }
        }
    }

    private void setupGroupBar () {
        View groupId = (View)mGroupIdView.getParent();
        if(mGroupId != null) {
            groupId.setVisibility(View.VISIBLE);
            mGroupIdView.setText(mGroupId);
            mShareButton.setActivated(true);
            ((View)mMemberBar.getParent()).setVisibility(View.VISIBLE);
        }
        else {
            groupId.setVisibility(View.INVISIBLE);
            mShareButton.findViewById(R.id.share).setActivated(false);
            mMembers.clear();
            mMemberBar.removeAllViews();
            ((View)mMemberBar.getParent()).setVisibility(View.GONE);
        }
    }

    private void setupMember(View view, Member member) {
        TextView tv = (TextView) view.findViewById(R.id.name);
        ImageView iv = (ImageView) view.findViewById(R.id.avatar);
        if(member == null) {
            tv.setText(null);
            iv.setImageDrawable(null);
            view.setVisibility(View.GONE);
            view.setTag(null);
        }
        else {
            tv.setText(member.name);
            if(member.avatarDrawable != null) {
                iv.setImageDrawable(member.avatarDrawable);
            }
            else {
                ImageLoader.getInstance().displayImage(
                        Urls.SERVER + member.avatar,
                        iv,
                        mDisplayImageOptions, mImageListener);
            }
            view.setVisibility(View.VISIBLE);
            view.setTag(member.devId);
        }
    }

    /**
     * 改变百度导航的浏览模式
     * @param preview 是否查看全程
     */
    private void setPreview(boolean preview) {
        //百度导航界面上的 "查看全程"/"继续导航" 按钮
        View v = findViewById(1711866180);
        if(!(v instanceof TextView)) {
            return;
        }
        TextView tv = (TextView) v;
        if("查看全程".equals(tv.getText())) {
            if(!preview) {
                tv.performClick();
            }
        }
        else if(preview){
            tv.performClick();
        }
    }

    private void toastInfo(String msg, Drawable icon) {
        TextView tv = (TextView) getLayoutInflater().inflate(R.layout.toast, null);
        tv.setText(msg);
        tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        Toast toast = new Toast(this);
        toast.setView(tv);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    Runnable mSync = new Runnable() {
        @Override
        public void run() {
            if(mGroupId == null)
                return;

            if (mBDLocation != null) {
                LinkedHashMap<String, String> params = new LinkedHashMap<>();
                params.put("devid", Build.SERIAL);
                params.put("lat", String.valueOf(mBDLocation.getLatitude()));
                params.put("lng", String.valueOf(mBDLocation.getLongitude()));
                Network.postSilently(Urls.PROFILE, params, null);
            }

            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("groupId", mGroupId);
            Network.postSilently(Urls.GROUP_MEMBER, params, Navi.this);

            mMemberBar.postDelayed(this, SYNC_INTERVAL);
        }
    };

    class Member {
        String name;
        String avatar;
        Double lat;
        Double lng;
        String devId;
        Drawable avatarDrawable;
        OverlayItem mOverlay;
        boolean isUpdated, shouldNotify;
    }

    class CircleClipper implements BitmapProcessor {
        final int SIZE = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                35, App.sApp.getResources().getDisplayMetrics());
        Path mClipPath = new Path();
        Rect mDst = new Rect();
        Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        CircleClipper() {
            mClipPath.addOval(new RectF(0, 0, SIZE, SIZE), Path.Direction.CW);
        }
        @Override
        public Bitmap process(Bitmap bitmap) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if(w > h) {
                int H = h*SIZE/w;
                mDst.set(0, (SIZE - H)/2, SIZE, (SIZE + H)/2);
            }
            else {
                int W = w*SIZE/h;
                mDst.set((SIZE - W)/2, 0, (SIZE + W)/2, SIZE);
            }
            Bitmap b = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.clipPath(mClipPath);
            c.drawBitmap(bitmap, null, mDst, mPaint);
            bitmap.recycle();
            return b;
        }
    }
}
