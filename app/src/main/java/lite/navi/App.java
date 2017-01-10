package lite.navi;

import android.app.Application;
import android.os.Build;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.SDKInitializer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.whld.network.volley.Network;

import java.lang.reflect.Field;

/**
 * @author ylscat
 *         Date: 2016-07-27 14:20
 */
public class App extends Application {
    public static App sApp;
    // 百度定位最后一次定位得到的位置,用于搜索界面
    public BDLocation mLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        SDKInitializer.initialize(this);
        sApp = this;
        Network.init(this);
        DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder();
        builder.cacheInMemory(false)
                .cacheOnDisk(true);
        ImageLoaderConfiguration conf = new ImageLoaderConfiguration
                .Builder(this)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheSize(20*1024*1024)
                .memoryCacheSize(5*1024*1024)
                .defaultDisplayImageOptions(builder.build())
//                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(conf);
    }
}
