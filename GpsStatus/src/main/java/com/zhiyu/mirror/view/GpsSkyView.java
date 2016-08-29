package com.zhiyu.mirror.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import com.zhiyu.mirror.GpsBarChartFragment;
import com.zhiyu.mirror.GpsTestActivity;
import com.zhiyu.mirror.util.GnssType;
import com.zhiyu.mirror.util.GpsTestUtil;

import java.util.Iterator;

/**
 * Created by indy on 2016/7/9.
 */
public class GpsSkyView extends View implements GpsTestActivity.GpsTestListener {

    private static final int SAT_RADIUS = 10;

    private static final float PRN_TEXT_SCALE = 2.5f;

    private final float mSnrThresholds[];

    private final int mSnrColors[];

    Context mContext;

    WindowManager mWindowManager;

    private Paint mHorizonActiveFillPaint, mHorizonInactiveFillPaint, mHorizonStrokePaint,
            mGridStrokePaint,
            mSatelliteFillPaint, mSatelliteStrokePaint, mNorthPaint, mNorthFillPaint,
            mPrnIdPaint;

    private double mOrientation = 0.0;

    private boolean mStarted;

    private float mSnrs[], mElevs[], mAzims[];

    private int mPrns[];

    private int mSvCount;
    AttributeSet attributeSet;
    public GpsSkyView(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);
        mContext = context;
        GpsTestActivity.getInstance().addListener(this);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        mHorizonActiveFillPaint = new Paint();
        mHorizonActiveFillPaint.setColor(Color.WHITE);
        mHorizonActiveFillPaint.setStyle(Paint.Style.FILL);
        mHorizonActiveFillPaint.setAntiAlias(true);

        mHorizonInactiveFillPaint = new Paint();
        mHorizonInactiveFillPaint.setColor(Color.LTGRAY);
        mHorizonInactiveFillPaint.setStyle(Paint.Style.FILL);
        mHorizonInactiveFillPaint.setAntiAlias(true);

        mHorizonStrokePaint = new Paint();
        mHorizonStrokePaint.setColor(Color.BLACK);
        mHorizonStrokePaint.setStyle(Paint.Style.STROKE);
        mHorizonStrokePaint.setStrokeWidth(2.0f);
        mHorizonStrokePaint.setAntiAlias(true);

        mGridStrokePaint = new Paint();
        mGridStrokePaint.setColor(Color.GRAY);
        mGridStrokePaint.setStyle(Paint.Style.STROKE);
        mGridStrokePaint.setAntiAlias(true);

        mSatelliteFillPaint = new Paint();
        mSatelliteFillPaint.setColor(Color.YELLOW);
        mSatelliteFillPaint.setStyle(Paint.Style.FILL);
        mSatelliteFillPaint.setAntiAlias(true);

        mSatelliteStrokePaint = new Paint();
        mSatelliteStrokePaint.setColor(Color.BLACK);
        mSatelliteStrokePaint.setStyle(Paint.Style.STROKE);
        mSatelliteStrokePaint.setStrokeWidth(2.0f);
        mSatelliteStrokePaint.setAntiAlias(true);

        mSnrThresholds = new float[]{0.0f, 10.0f, 20.0f, 30.0f};
        mSnrColors = new int[]{Color.GRAY, Color.RED, Color.YELLOW, Color.GREEN};

        mNorthPaint = new Paint();
        mNorthPaint.setColor(Color.BLACK);
        mNorthPaint.setStyle(Paint.Style.STROKE);
        mNorthPaint.setStrokeWidth(4.0f);
        mNorthPaint.setAntiAlias(true);

        mNorthFillPaint = new Paint();
        mNorthFillPaint.setColor(Color.GRAY);
        mNorthFillPaint.setStyle(Paint.Style.FILL);
        mNorthFillPaint.setStrokeWidth(4.0f);
        mNorthFillPaint.setAntiAlias(true);

        mPrnIdPaint = new Paint();
        mPrnIdPaint.setColor(Color.BLACK);
        mPrnIdPaint.setStyle(Paint.Style.STROKE);
        mNorthPaint.setStrokeWidth(4.0f);
        mPrnIdPaint.setTextSize(SAT_RADIUS * PRN_TEXT_SCALE);
        mPrnIdPaint.setAntiAlias(true);

        setFocusable(true);
    }
    public GpsSkyView(Context context) {
        super(context);

        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        mHorizonActiveFillPaint = new Paint();
        mHorizonActiveFillPaint.setColor(Color.WHITE);
        mHorizonActiveFillPaint.setStyle(Paint.Style.FILL);
        mHorizonActiveFillPaint.setAntiAlias(true);

        mHorizonInactiveFillPaint = new Paint();
        mHorizonInactiveFillPaint.setColor(Color.LTGRAY);
        mHorizonInactiveFillPaint.setStyle(Paint.Style.FILL);
        mHorizonInactiveFillPaint.setAntiAlias(true);

        mHorizonStrokePaint = new Paint();
        mHorizonStrokePaint.setColor(Color.BLACK);
        mHorizonStrokePaint.setStyle(Paint.Style.STROKE);
        mHorizonStrokePaint.setStrokeWidth(2.0f);
        mHorizonStrokePaint.setAntiAlias(true);

        mGridStrokePaint = new Paint();
        mGridStrokePaint.setColor(Color.GRAY);
        mGridStrokePaint.setStyle(Paint.Style.STROKE);
        mGridStrokePaint.setAntiAlias(true);

        mSatelliteFillPaint = new Paint();
        mSatelliteFillPaint.setColor(Color.YELLOW);
        mSatelliteFillPaint.setStyle(Paint.Style.FILL);
        mSatelliteFillPaint.setAntiAlias(true);

        mSatelliteStrokePaint = new Paint();
        mSatelliteStrokePaint.setColor(Color.BLACK);
        mSatelliteStrokePaint.setStyle(Paint.Style.STROKE);
        mSatelliteStrokePaint.setStrokeWidth(2.0f);
        mSatelliteStrokePaint.setAntiAlias(true);

        mSnrThresholds = new float[]{0.0f, 10.0f, 20.0f, 30.0f};
        mSnrColors = new int[]{Color.GRAY, Color.RED, Color.YELLOW, Color.GREEN};

        mNorthPaint = new Paint();
        mNorthPaint.setColor(Color.BLACK);
        mNorthPaint.setStyle(Paint.Style.STROKE);
        mNorthPaint.setStrokeWidth(4.0f);
        mNorthPaint.setAntiAlias(true);

        mNorthFillPaint = new Paint();
        mNorthFillPaint.setColor(Color.GRAY);
        mNorthFillPaint.setStyle(Paint.Style.FILL);
        mNorthFillPaint.setStrokeWidth(4.0f);
        mNorthFillPaint.setAntiAlias(true);

        mPrnIdPaint = new Paint();
        mPrnIdPaint.setColor(Color.BLACK);
        mPrnIdPaint.setStyle(Paint.Style.STROKE);
        mNorthPaint.setStrokeWidth(4.0f);
        mPrnIdPaint.setTextSize(SAT_RADIUS * PRN_TEXT_SCALE);
        mPrnIdPaint.setAntiAlias(true);

        setFocusable(true);
    }

    public void setStarted() {
        mStarted = true;
        invalidate();
    }

    public void setStopped() {
        mStarted = false;
        mSvCount = 0;
        invalidate();
    }

    public void setSats(GpsStatus status) {
        Iterator<GpsSatellite> satellites = status.getSatellites().iterator();

        if (mSnrs == null) {
            int length = status.getMaxSatellites();
            mSnrs = new float[length];
            mElevs = new float[length];
            mAzims = new float[length];
            mPrns = new int[length];
        }

        mSvCount = 0;
        while (satellites.hasNext()) {
            GpsSatellite satellite = satellites.next();
            mSnrs[mSvCount] = satellite.getSnr();
            mElevs[mSvCount] = satellite.getElevation();
            mAzims[mSvCount] = satellite.getAzimuth();
            mPrns[mSvCount] = satellite.getPrn();
            mSvCount++;
        }

        mStarted = true;
        invalidate();
    }

    private void drawLine(Canvas c, float x1, float y1, float x2, float y2) {
        // rotate the line based on orientation
        double angle = Math.toRadians(-mOrientation);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float centerX = (x1 + x2) / 2.0f;
        float centerY = (y1 + y2) / 2.0f;
        x1 -= centerX;
        y1 = centerY - y1;
        x2 -= centerX;
        y2 = centerY - y2;

        float X1 = cos * x1 + sin * y1 + centerX;
        float Y1 = -(-sin * x1 + cos * y1) + centerY;
        float X2 = cos * x2 + sin * y2 + centerX;
        float Y2 = -(-sin * x2 + cos * y2) + centerY;

        c.drawLine(X1, Y1, X2, Y2, mGridStrokePaint);
    }

    private void drawHorizon(Canvas c, int s) {
        float radius = s / 2;

        c.drawCircle(radius, radius, radius,
                mStarted ? mHorizonActiveFillPaint : mHorizonInactiveFillPaint);
        drawLine(c, 0, radius, 2 * radius, radius);
        drawLine(c, radius, 0, radius, 2 * radius);
        c.drawCircle(radius, radius, elevationToRadius(s, 60.0f), mGridStrokePaint);
        c.drawCircle(radius, radius, elevationToRadius(s, 30.0f), mGridStrokePaint);
        c.drawCircle(radius, radius, elevationToRadius(s, 0.0f), mGridStrokePaint);
        c.drawCircle(radius, radius, radius, mHorizonStrokePaint);
    }

    private void drawNorthIndicator(Canvas c, int s) {
        float radius = s / 2;
        double angle = Math.toRadians(-mOrientation);
        final float ARROW_HEIGHT_SCALE = 0.05f;
        final float ARROW_WIDTH_SCALE = 0.1f;

        float x1, y1;  // Tip of arrow
        x1 = radius;
        y1 = elevationToRadius(s, 90.0f);

        float x2, y2;
        x2 = x1 + radius * ARROW_HEIGHT_SCALE;
        y2 = y1 + radius * ARROW_WIDTH_SCALE;

        float x3, y3;
        x3 = x1 - radius * ARROW_HEIGHT_SCALE;
        y3 = y1 + radius * ARROW_WIDTH_SCALE;

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x1, y1);
        path.close();

        // Rotate arrow around center point
        Matrix matrix = new Matrix();
        matrix.postRotate((float) -mOrientation, radius, radius);
        path.transform(matrix);

        c.drawPath(path, mNorthPaint);
        c.drawPath(path, mNorthFillPaint);
    }

    private void drawSatellite(Canvas c, int s, float elev, float azim, float snr, int prn) {
        double radius, angle;
        float x, y;
        // Place PRN text slightly below drawn satellite
        final double PRN_X_SCALE = 1.4;
        final double PRN_Y_SCALE = 3.5;
        Paint thisPaint;

        thisPaint = getSatellitePaint(mSatelliteFillPaint, snr);

        radius = elevationToRadius(s, elev);
        azim -= mOrientation;
        angle = (float) Math.toRadians(azim);

        x = (float) ((s / 2) + (radius * Math.sin(angle)));
        y = (float) ((s / 2) - (radius * Math.cos(angle)));

        // Change shape based on satellite operator
        GnssType operator = GpsTestUtil.getGnssType(prn);
        switch (operator) {
            case NAVSTAR:
                c.drawCircle(x, y, SAT_RADIUS, thisPaint);
                c.drawCircle(x, y, SAT_RADIUS, mSatelliteStrokePaint);
                break;
            case GLONASS:
                c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                        thisPaint);
                c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                        mSatelliteStrokePaint);
                break;
            case QZSS:
                drawTriangle(c, x, y, thisPaint);
                break;
            case BEIDOU:
                drawPentagon(c, x, y, thisPaint);
                break;
        }

        c.drawText(String.valueOf(prn), x - (int) (SAT_RADIUS * PRN_X_SCALE),
                y + (int) (SAT_RADIUS * PRN_Y_SCALE), mPrnIdPaint);
    }

    private float elevationToRadius(int s, float elev) {
        return ((s / 2) - SAT_RADIUS) * (1.0f - (elev / 90.0f));
    }

    private void drawTriangle(Canvas c, float x, float y, Paint fillPaint) {
        float x1, y1;  // Top
        x1 = x;
        y1 = y - SAT_RADIUS;

        float x2, y2; // Lower left
        x2 = x - SAT_RADIUS;
        y2 = y + SAT_RADIUS;

        float x3, y3; // Lower right
        x3 = x + SAT_RADIUS;
        y3 = y + SAT_RADIUS;

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x1, y1);
        path.close();

        c.drawPath(path, fillPaint);
        c.drawPath(path, mSatelliteStrokePaint);
    }

    private void drawPentagon(Canvas c, float x, float y, Paint fillPaint) {
        Path path = new Path();
        path.moveTo(x, y - SAT_RADIUS);
        path.lineTo(x - SAT_RADIUS, y - (SAT_RADIUS / 3));
        path.lineTo(x - 2 * (SAT_RADIUS / 3), y + SAT_RADIUS);
        path.lineTo(x + 2 * (SAT_RADIUS / 3), y + SAT_RADIUS);
        path.lineTo(x + SAT_RADIUS, y - (SAT_RADIUS / 3));
        path.close();

        c.drawPath(path, fillPaint);
        c.drawPath(path, mSatelliteStrokePaint);
    }

    private Paint getSatellitePaint(Paint base, float snr) {
        int numSteps;
        Paint newPaint;

        newPaint = new Paint(base);

        numSteps = mSnrThresholds.length;

        if (snr <= mSnrThresholds[0]) {
            newPaint.setColor(mSnrColors[0]);
            return newPaint;
        }

        if (snr >= mSnrThresholds[numSteps - 1]) {
            newPaint.setColor(mSnrColors[numSteps - 1]);
            return newPaint;
        }

        for (int i = 0; i < numSteps - 1; i++) {
            float threshold = mSnrThresholds[i];
            float nextThreshold = mSnrThresholds[i + 1];
            if (snr >= threshold && snr <= nextThreshold) {
                int c1, r1, g1, b1, c2, r2, g2, b2, c3, r3, g3, b3;
                float f;

                c1 = mSnrColors[i];
                r1 = Color.red(c1);
                g1 = Color.green(c1);
                b1 = Color.blue(c1);

                c2 = mSnrColors[i + 1];
                r2 = Color.red(c2);
                g2 = Color.green(c2);
                b2 = Color.blue(c2);

                f = (snr - threshold) / (nextThreshold - threshold);

                r3 = (int) (r2 * f + r1 * (1.0f - f));
                g3 = (int) (g2 * f + g1 * (1.0f - f));
                b3 = (int) (b2 * f + b1 * (1.0f - f));
                c3 = Color.rgb(r3, g3, b3);

                newPaint.setColor(c3);

                return newPaint;
            }
        }

        newPaint.setColor(Color.MAGENTA);

        return newPaint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int minScreenDimen;

        minScreenDimen = (GpsBarChartFragment.mWidth < GpsBarChartFragment.mHeight)
                ? GpsBarChartFragment.mWidth : GpsBarChartFragment.mHeight;

        drawHorizon(canvas, minScreenDimen);

        drawNorthIndicator(canvas, minScreenDimen);

        if (mElevs != null) {
            int numSats = mSvCount;

            for (int i = 0; i < numSats; i++) {
                if (mSnrs[i] > 0.0f && (mElevs[i] != 0.0f || mAzims[i] != 0.0f)) {
                    drawSatellite(canvas, minScreenDimen, mElevs[i], mAzims[i], mSnrs[i],
                            mPrns[i]);
                }
            }
        }
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        mOrientation = orientation;
        invalidate();
    }

    @Override
    public void gpsStart() {
    }

    @Override
    public void gpsStop() {
    }

    @Override
    public void onGpsStatusChanged(int event, GpsStatus status) {
    }

    @Override
    public void onLocationChanged(Location location) {
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
}
