/*
 * Copyright (C) 2008-2013 The Android Open Source Project,
 * Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhiyu.mirror;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.zhiyu.mirror.R;
import com.zhiyu.mirror.util.GnssType;
import com.zhiyu.mirror.util.GpsTestUtil;
import com.zhiyu.mirror.view.GpsSkyView;
import com.zhiyu.mirror.view.HorizontalListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GpsBarChartFragment extends Fragment implements GpsTestActivity.GpsTestListener {

    private final static String TAG = "GpsBarChartActivity";

    private static final int PRN_COLUMN = 0;

    private static final int FLAG_IMAGE_COLUMN = 1;

    private static final int SNR_COLUMN = 2;

    private static final int ELEVATION_COLUMN = 3;

    private static final int AZIMUTH_COLUMN = 4;

    private static final int FLAGS_COLUMN = 5;

    private static final int COLUMN_COUNT = 6;

    private static final String EMPTY_LAT_LONG = "             ";

    private Resources mRes;

    private TextView mLatitudeView, mLongitudeView, mFixTimeView, mTTFFView, mAltitudeView,
            mAccuracyView, mSpeedView, mBearingView;

    private int mSvCount, mPrns[];

    private float mSnrs[], mSvElevations[], mSvAzimuths[];

    private int mEphemerisMask, mAlmanacMask, mUsedInFixMask;

    private long mFixTime;

    private boolean mNavigating, mGotFix;

    private Drawable flagUsa, flagRussia, flagJapan, flagChina;

    private HorizontalListView lv;
    List<Map<String, Object>> listmap;
    private BarChart mBarChart;
    private BarData mBarData;
    public static int mHeight;

    public static int mWidth;

    private GpsSkyView mSkyView;
    private double mOrientation = 0.0;
    private int listSize;
    private ArrayList<String> xValues;
    private int totalWidth;
    public GpsBarChartFragment() {
    }

    private static String doubleToString(double value, int decimals) {
        String result = Double.toString(value);
        // truncate to specified number of decimal places
        int dot = result.indexOf('.');
        if (dot > 0) {
            int end = dot + decimals + 1;
            if (end < result.length()) {
                result = result.substring(0, end);
            }
        }
        return result;
    }

    public void onLocationChanged(Location location) {
        if (!mGotFix) {
            mTTFFView.setText(GpsTestActivity.getInstance().mTtff);
            mGotFix = true;
        }
        mLatitudeView.setText(doubleToString(location.getLatitude(), 7) + " ");
        mLongitudeView.setText(doubleToString(location.getLongitude(), 7) + " ");
        mFixTime = location.getTime();
        if (location.hasAltitude()) {
            mAltitudeView.setText(doubleToString(location.getAltitude(), 1) + " m");
        } else {
            mAltitudeView.setText("");
        }
        if (location.hasAccuracy()) {
            mAccuracyView.setText(doubleToString(location.getAccuracy(), 1) + " m");
        } else {
            mAccuracyView.setText("");
        }
        if (location.hasSpeed()) {
            mSpeedView.setText(doubleToString(location.getSpeed(), 1) + " m/sec");
        } else {
            mSpeedView.setText("");
        }
        if (location.hasBearing()) {
            mBearingView.setText(doubleToString(location.getBearing(), 1) + " deg");
        } else {
            mBearingView.setText("");
        }
        updateFixTime();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRes = getResources();
        View v = inflater.inflate(R.layout.gps_status2, container, false);

        mLatitudeView = (TextView) v.findViewById(R.id.latitude);
        mLongitudeView = (TextView) v.findViewById(R.id.longitude);
        mFixTimeView = (TextView) v.findViewById(R.id.fix_time);
        mTTFFView = (TextView) v.findViewById(R.id.ttff);
        mAltitudeView = (TextView) v.findViewById(R.id.altitude);
        mAccuracyView = (TextView) v.findViewById(R.id.accuracy);
        mSpeedView = (TextView) v.findViewById(R.id.speed);
        mBearingView = (TextView) v.findViewById(R.id.bearing);

        mLatitudeView.setText(EMPTY_LAT_LONG);
        mLongitudeView.setText(EMPTY_LAT_LONG);

        flagUsa = getResources().getDrawable(R.drawable.ic_flag_usa);
        flagRussia = getResources().getDrawable(R.drawable.ic_flag_russia);
        flagJapan = getResources().getDrawable(R.drawable.ic_flag_japan);
        flagChina = getResources().getDrawable(R.drawable.ic_flag_china);

        GpsTestActivity.getInstance().addListener(this);
        mBarChart = (BarChart) v.findViewById(R.id.barChart);
        mBarData = getBarData();
//        if(mBarData!=null)
            showBarChart(mBarChart, mBarData);

//        mSkyView = new GpsSkyView(getActivity());
        mSkyView=(GpsSkyView) v.findViewById(R.id.skyView);
//        LinearLayout ll = (LinearLayout)v.findViewById(R.id.ll);
//        ll.setGravity(Gravity.CENTER);
//        mSkyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mSkyView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @SuppressWarnings("deprecation")
                    @SuppressLint("NewApi")
                    @Override
                    public void onGlobalLayout() {
                        final View v = getView();
                        mHeight = v.getHeight();
                        mWidth = v.getWidth();

                        if (v.getViewTreeObserver().isAlive()) {
                            // remove this layout listener
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    }
                }
        );

        lv=(HorizontalListView) v.findViewById(R.id.list);
        lv.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @SuppressWarnings("deprecation")
                    @SuppressLint("NewApi")
                    @Override
                    public void onGlobalLayout() {
                        final View v = getView();
                        totalWidth = v.getWidth()/2-70;
                    }
                }
        );
        myData();
        //绑定数据
        lv.setAdapter(new MyAdapter());

//        setListViewHeightBasedOnChildren(lv);
//        ll.addView(mSkyView);
        return v;
    }

    public void to(View v){
//        Intent intent = new Intent(getActivity(),WholeActivity.class);
//        startActivity(intent);
    }
    private void showBarChart(BarChart barChart, BarData barData) {
        barChart.setDrawBorders(false);  ////是否在折线图上添加边框

        barChart.setDescription("");// 数据描述

        // 如果没有数据的时候，会显示这个，类似ListView的EmptyView
        barChart.setNoDataTextDescription("当前没有卫星数据.");

        barChart.setDrawGridBackground(false); // 是否显示表格颜色
        barChart.setGridBackgroundColor(Color.WHITE & 0x70FFFFFF); // 表格的的颜色，在这里是是给颜色设置一个透明度

        barChart.setTouchEnabled(false); // 设置是否可以触摸
        barChart.setVisibleXRange(20);  //一个界面显示多少个点，其他点可以通过滑动看到
        barChart.setDragDecelerationEnabled(false);//拖拽滚动时，手放开是否会持续滚动，默认是true（false是拖到哪是哪，true拖拽之后还会有缓冲）
        barChart.setDragDecelerationFrictionCoef(0.99f);
//        barChart.setVisibleXRangeMinimum(3);
        barChart.setDragEnabled(false);// 是否可以拖拽
        barChart.setScaleEnabled(false);// 是否可以缩放

        barChart.setPinchZoom(false);//
        barChart.setVisibleXRange(75f);//setVisibleXRangeMaximum(7.5f);
//        barChart.setVisibleXRangeMinimum(7.5f);
        barChart.setHorizontalScrollBarEnabled(false);
//      barChart.setBackgroundColor();// 设置背景

        barChart.setDrawBarShadow(false);
        barChart.setScaleYEnabled(false);//
//        barChart.setScaleXEnabled(true);//
        barChart.setMaxVisibleValueCount(100);
        barChart.setData(barData); // 设置数据

        Legend mLegend = barChart.getLegend(); // 设置比例图标示

        mLegend.setForm(Legend.LegendForm.CIRCLE);// 样式
        mLegend.setFormSize(15f);// 字体
        mLegend.setTextColor(Color.RED);// 颜色
//      X轴设定
        XAxis xAxis = barChart.getXAxis();
//        xAxis.setLabelsToSkip(10);
        xAxis.setAxisLineWidth(1);
        xAxis.setXOffset(1);
        xAxis.setTextSize(15);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);//竖线
        xAxis.resetLabelsToSkip();
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setSpaceBetweenLabels(4);
        xAxis.setLabelsToSkip(0);
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setLabelCount(5);
        leftAxis.setAxisMaxValue(100);
        leftAxis.setSpaceTop(15f);
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setLabelCount(5);
        rightAxis.setAxisMaxValue(100);
        rightAxis.setSpaceTop(15f);

//        barChart.animateX(10); // 立即执行的动画,x轴
        barChart.animateY(10); // 立即执行的动画,Y轴
    }

    private BarData getBarData() {
//        if (mPrns==null||mSnrs==null){
//            return null;
//        }
//        ArrayList<String> xValues = new ArrayList<String>();
        xValues= new ArrayList<String>();
        if (mPrns!=null) {
            for (int i = 0; i < mPrns.length; i++) {
                if (mPrns[i] != 0) {
//                    Log.d("test", mPrns[i] + "x");
                    xValues.add(Integer.toString(mPrns[i]));
                }
            }
        }
        listSize = xValues.size();
        ArrayList<BarEntry> yValues = new ArrayList<BarEntry>();
        if (mSnrs!=null) {
            for (int j = 0; j < mSnrs.length; j++) {
                if (mSnrs[j] != 0.0) {
//                    Log.d("test", mSnrs[j] + "y");
                    yValues.add(new BarEntry(mSnrs[j], j));
                }
            }
        }

        // y轴的数据集合
        BarDataSet barDataSet = new BarDataSet(yValues, "卫星柱状图");
        barDataSet.setBarSpacePercent(10f);
        barDataSet.setValueTextSize(10);
        barDataSet.setValueTextColor(Color.RED);
        barDataSet.setColors(new int[]{
                Color.rgb(192, 255, 140), Color.rgb(255, 247, 140), Color.rgb(255, 208, 140),
                Color.rgb(140, 234, 255), Color.rgb(255, 140, 157)});

        ArrayList<BarDataSet> barDataSets = new ArrayList<BarDataSet>();
        barDataSets.add(barDataSet); // add the datasets

        BarData barData = new BarData(xValues, barDataSets);
        mPrns=null; mSnrs=null;
        return barData;
    }

    private void setStarted(boolean navigating) {
        if (navigating != mNavigating) {
            if (navigating) {

            } else {
                mLatitudeView.setText(EMPTY_LAT_LONG);
                mLongitudeView.setText(EMPTY_LAT_LONG);
                mFixTime = 0;
                updateFixTime();
                mTTFFView.setText("");
                mAltitudeView.setText("");
                mAccuracyView.setText("");
                mSpeedView.setText("");
                mBearingView.setText("");
                mSvCount = 0;
//                setData();
                mBarData = getBarData();
                if(mBarData!=null)
                    showBarChart(mBarChart, mBarData);
//                mAdapter.notifyDataSetChanged();
            }
            mNavigating = navigating;
        }
    }

    private void updateFixTime() {
        if (mFixTime == 0 || !GpsTestActivity.getInstance().mStarted) {
            mFixTimeView.setText("");
        } else {
            mFixTimeView.setText(DateUtils.getRelativeTimeSpanString(
                    mFixTime, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        GpsTestActivity gta = GpsTestActivity.getInstance();
        setStarted(gta.mStarted);
    }

    public void onGpsStarted() {
        setStarted(true);
    }

    public void onGpsStopped() {
        setStarted(false);
    }

    @SuppressLint("NewApi")
    public void gpsStart() {
        //Reset flag for detecting first fix
        mGotFix = false;
    }

    public void gpsStop() {
    }

    public void onGpsStatusChanged(int event, GpsStatus status) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                setStarted(true);
                mSkyView.setStarted();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                setStarted(false);
                mSkyView.setStopped();
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateStatus(status);
                mSkyView.setSats(status);
                break;
        }
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        if (!getUserVisibleHint()) {
            return;
        }

        mOrientation = orientation;
        if (mSkyView != null) {
            mSkyView.onOrientationChanged(orientation, tilt);
            mSkyView.invalidate();
            lv.setAdapter(new MyAdapter());
        }

    }

    private void updateStatus(GpsStatus status) {

        setStarted(true);
        // update the fix time regularly, since it is displaying relative time
        updateFixTime();

        Iterator<GpsSatellite> satellites = status.getSatellites().iterator();

        if (mPrns == null) {
            int length = status.getMaxSatellites();
//            Log.d("test","test"+length);
            mPrns = new int[length];
            mSnrs = new float[length];
            mSvElevations = new float[length];
            mSvAzimuths = new float[length];
        }

        mSvCount = 0;
        mEphemerisMask = 0;
        mAlmanacMask = 0;
        mUsedInFixMask = 0;
        while (satellites.hasNext()) {
            GpsSatellite satellite = satellites.next();
            int prn = satellite.getPrn();
            int prnBit = (1 << (prn - 1));
            mPrns[mSvCount] = prn;
            mSnrs[mSvCount] = satellite.getSnr();
            mSvElevations[mSvCount] = satellite.getElevation();
            mSvAzimuths[mSvCount] = satellite.getAzimuth();
            if (satellite.hasEphemeris()) {
                mEphemerisMask |= prnBit;
            }
            if (satellite.hasAlmanac()) {
                mAlmanacMask |= prnBit;
            }
            if (satellite.usedInFix()) {
                mUsedInFixMask |= prnBit;
            }
            mSvCount++;
        }
//        setData();
        showBarChart(mBarChart, getBarData());
//        mAdapter.notifyDataSetChanged();
    }

    public  void setListViewHeightBasedOnChildren(HorizontalListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int totalWidth=800;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
//            listItem.setMinimumWidth(totalWidth/listAdapter.getCount());
//            totalWidth += listItem.getMeasuredWidth();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();

        params.width = totalWidth
                + (listView.getDividerWidth() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    class MyAdapter extends BaseAdapter {
        //获得总条目数
        @Override
        public int getCount() {

            return listSize;
        }

        @Override
        public Object getItem(int position) {

            return listmap.get(position);
        }

        @Override
        public long getItemId(int position) {

            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            convertView=inflater.inflate(R.layout.item_activity, null);
            //找到控件
            final ImageView im=(ImageView) convertView.findViewById(R.id.imageView1);
            ViewGroup.LayoutParams params = im.getLayoutParams();
            if (listSize!=0)
            params.width = totalWidth/(listSize+1);
//            Log.d("test","单个长度:"+params.width);
            im.setLayoutParams(params);
            //绑定数据
            if (xValues!=null && xValues.size()>0) {
                GnssType type = GpsTestUtil.getGnssType(Integer.parseInt(xValues.get(position)));
                switch (type) {
                    case NAVSTAR:
                        im.setImageDrawable(flagUsa);
                        break;
                    case GLONASS:
                        im.setImageDrawable(flagRussia);
                        break;
                    case QZSS:
                        im.setImageDrawable(flagJapan);
                        break;
                    case BEIDOU:
                        im.setImageDrawable(flagChina);
                        break;
                }
            }
//            im.setImageResource(Integer.parseInt(imageStr));
            return convertView;
        }


    }

    //模拟10条数据
    private List<Map<String, Object>>myData(){

        listmap = new ArrayList<Map<String,Object>>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("image", R.drawable.ic_launcher);
            listmap.add(map);
        }
        return listmap;
    }

}