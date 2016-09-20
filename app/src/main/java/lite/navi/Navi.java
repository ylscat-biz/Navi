package lite.navi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRouteGuideManager.OnNavigationListener;
import com.baidu.navisdk.adapter.BNaviBaseCallbackModel;
import com.baidu.navisdk.adapter.BaiduNaviCommonModule;
import com.baidu.navisdk.adapter.NaviModuleFactory;
import com.baidu.navisdk.adapter.NaviModuleImpl;
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

/**
 * 诱导界面
 * 
 * @author sunhao04
 *
 */
public class Navi extends Activity implements LocationListener{

    private final String TAG = Navi.class.getName();
    private BaiduNaviCommonModule mBaiduNaviCommonModule = null;

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

    private double Xmin = 114.3272189510811;
    private double Xmax = 114.34153905102028;
    private double Ymin = 30.542809291954995;
    private double Ymax = 30.56307135368746;
    private boolean isInRegion = false;

    private static final SpatialReference WGS84 = SpatialReference.create(
            SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mArcMap = new MapView(this);
        mArcMap.setOnTouchListener(null);

        ArcGISTiledMapServiceLayer base = new ArcGISTiledMapServiceLayer(
                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
//                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer");
        mArcMap.addLayer(base);
        base.setVisible(false);
        //Envelope [m_envelope=Envelope2D [xmin=114.3272189510811, ymin=30.542809291954995, xmax=114.34153905102028, ymax=30.56307135368746], m_attributes=null]
        DynamicLayer dl = new ArcGISDynamicMapServiceLayer("http://121.40.231.209:6080/arcgis/rest/services/MiddleNorthRoad/MapServer");
        mArcMap.addLayer(dl);
        mGraphicsLayer = new GraphicsLayer();
        mArcMap.addLayer(mGraphicsLayer);
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

        for(int i = 0; i < mArcMap.getChildCount(); i++) {
            View child = mArcMap.getChildAt(i);
            if(child instanceof GLSurfaceView) {
                GLSurfaceView glView = (GLSurfaceView)child;
                glView.setZOrderOnTop(true);
            }
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int w = metrics.widthPixels/3;
        int h = metrics.heightPixels;
        mArcMap.setLayoutParams(new FrameLayout.LayoutParams(w, h));

        FrameLayout fl = (FrameLayout)findViewById(android.R.id.content);

        mMockLocation.setLatitude((Ymin + Ymax)/2);
        mMockLocation.setLongitude(Xmin - 0.01);
//        Button button = new Button(this);
//        button.setText("Test");
//        fl.addView(button, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT));
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mMockLocation.setLongitude(mMockLocation.getLongitude() + 0.002);
//                Log.d("Loc", String.format("%f %f", mMockLocation.getLongitude(), mMockLocation.getLatitude()));
//                setCurrentLocation(mMockLocation);
//            }
//        });
    }

    private void registerLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        for(String provider : locMgr.getAllProviders()) {
            Log.d("Prv", provider);
        }
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }

    private void unregisterLocListener() {
        LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locMgr.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

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
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onDestroy();
            }
        } else {
            BNRouteGuideManager.getInstance().onDestroy();
        }
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

    @Override
    public void onBackPressed() {
        if(useCommonInterface) {
            if(mBaiduNaviCommonModule != null) {
                mBaiduNaviCommonModule.onBackPressed(false);
            }
        } else {
            BNRouteGuideManager.getInstance().onBackPressed(false);
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
    public void onLocationChanged(Location location) {
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
        if(inRegion(x, y)) {
            if(!isInRegion) {
                FrameLayout fl = (FrameLayout) findViewById(android.R.id.content);
                fl.addView(mArcMap);
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
                mArcMap.setScale(5000);
                mArcMap.centerAt(y, x, true);
                SpatialReference sr = mArcMap.getSpatialReference();
                Point pt = GeometryEngine.project(x, y, WGS84);
                mGraphicsLayer.updateGraphic(mLocMarker, pt);
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
                FrameLayout fl = (FrameLayout)mArcMap.getParent();
                fl.removeView(mArcMap);
                isInRegion = false;
            }
        }
    }

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
}
