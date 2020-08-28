package com.armedarms.idealmedia.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.MotionEventCompat;

import org.jetbrains.annotations.NotNull;

public class Wave extends View {
    public final int DEFAULT_ABOVE_WAVE_ALPHA = 60;
    public final int DEFAULT_BLOW_WAVE_ALPHA = 40;

    private float[] mSourcePathPoints;
    private Path mAboveWavePath = new Path();
    private Path mBlowWavePath = new Path();
    private Path mMiddlePath = new Path();

    private Paint mAboveWavePaint = new Paint();
    private Paint mBlowWavePaint = new Paint();
    private Paint mMiddlePaint = new Paint();

    private int mAboveWaveColor;
    private int mBlowWaveColor;

    private float mWaveLength;
    private float mAboveOffset = 0.0f;

    private RefreshProgressRunnable mRefreshProgressRunnable;

    private IOnBandUpdateListener listener;

    private int left, right, bottom;
    // ω
    private double omega;

    public Wave(Context context) {
        super(context);
    }

    public Wave(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Wave(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Wave(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawPath(mBlowWavePath, mBlowWavePaint);
        canvas.drawPath(mAboveWavePath, mAboveWavePaint);
        canvas.drawPath(mMiddlePath, mMiddlePaint);
    }

    public void setAboveWaveColor(int aboveWaveColor) {
        this.mAboveWaveColor = aboveWaveColor;
    }

    public void setBlowWaveColor(int blowWaveColor) {
        this.mBlowWaveColor = blowWaveColor;
    }

    public void initializePainters() {
        mAboveWavePaint.setColor(mAboveWaveColor);
        mAboveWavePaint.setAlpha(DEFAULT_ABOVE_WAVE_ALPHA);
        mAboveWavePaint.setStyle(Paint.Style.FILL);
        mAboveWavePaint.setAntiAlias(true);

        mBlowWavePaint.setColor(mBlowWaveColor);
        mBlowWavePaint.setAlpha(DEFAULT_BLOW_WAVE_ALPHA);
        mBlowWavePaint.setStyle(Paint.Style.FILL);
        mBlowWavePaint.setAntiAlias(true);

        mMiddlePaint.setColor(0xffffffff);
        mMiddlePaint.setStyle(Paint.Style.STROKE);
    }

    private void calculatePath() {
        class Point {
            float x, y;
            float dx, dy;

            public Point(float x, float y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public String toString() {
                return x + ", " + y;
            }
        }

        mAboveWavePath.reset();
        mBlowWavePath.reset();
        mMiddlePath.reset();

        getWaveOffset();

        int h = getHeight();

        float y;
        mAboveWavePath.moveTo(left, bottom);
        mBlowWavePath.moveTo(left, bottom);
        mMiddlePath.moveTo(left, bottom / 2);

        Point[] points = new Point[mSourcePathPoints.length];
        for(int i = 0; i < mSourcePathPoints.length; i++)
            points[i] = new Point(i * right / (mSourcePathPoints.length - 1), h/2 - h/30 * (mSourcePathPoints[i]));

        for(int i = 0; i < mSourcePathPoints.length; i++){
            if(i >= 0){
                Point point = points[i];

                if (i == 0) {
                    Point next = points[i + 1];
                    point.dx = ((next.x - point.x) / 3);
                    point.dy = ((next.y - point.y) / 3);
                } else if (i == points.length - 1) {
                    Point prev = points[i - 1];
                    point.dx = ((point.x - prev.x) / 3);
                    point.dy = ((point.y - prev.y) / 3);
                } else {
                    Point next = points[i + 1];
                    Point prev = points[i - 1];
                    point.dx = ((next.x - prev.x) / 3);
                    point.dy = ((next.y - prev.y) / 3);
                }
            }
        }
        boolean first = true;
        for (int i = 0; i < mSourcePathPoints.length; i++) {
            Point point = points[i];
            if(first){
                first = false;
                mAboveWavePath.lineTo(point.x, point.y);
                mBlowWavePath.lineTo(point.x, point.y);
            }
            else{
                Point prev = points[i-1];
                mAboveWavePath.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, (float)(point.y + 10 * Math.sin(omega * point.x + mAboveOffset)));
                mBlowWavePath.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, (float)(point.y + 10 * Math.sin(omega * point.x + mAboveOffset + Math.PI/4)));
            }
        }
        mAboveWavePath.lineTo(right, bottom);
        mBlowWavePath.lineTo(right, bottom);
        mMiddlePath.lineTo(right, bottom / 2);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (View.GONE == visibility) {
            removeCallbacks(mRefreshProgressRunnable);
        } else {
            if (mWaveLength == 0)
                startWave();

            removeCallbacks(mRefreshProgressRunnable);
            mRefreshProgressRunnable = new RefreshProgressRunnable();
            post(mRefreshProgressRunnable);
        }
    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            case (MotionEvent.ACTION_DOWN):
            case (MotionEvent.ACTION_MOVE):
            case (MotionEvent.ACTION_UP):
                int band = Math.max(0, Math.min(mSourcePathPoints.length - 1, (int)(mSourcePathPoints.length * event.getX() / getWidth())));
                float value = 30 * (getHeight() - event.getY()) / getHeight();
                if (listener != null)
                    listener.OnBandUpdated(band, value);
                return true;
            case (MotionEvent.ACTION_CANCEL):
                return true;
            case (MotionEvent.ACTION_OUTSIDE):
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }

    public void setUpdateListener(IOnBandUpdateListener listener) {
        this.listener = listener;
    }

    public void startWave() {
        if (getWidth() != 0) {
            int width = getWidth();
            mWaveLength = width;
            left = 0;//getLeft();
            right = getRight() - getLeft();
            bottom = getBottom() - getTop();
            omega = Math.PI * 2 / mWaveLength;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        startWave();
    }

    private void getWaveOffset() {
        if (mAboveOffset > Float.MAX_VALUE - 100) {
            mAboveOffset = (float)Math.PI;
        } else {
            mAboveOffset += 0.2f;
        }
    }

    public void setFxValues(float[] sourcePath) {
        this.mSourcePathPoints = sourcePath;
    }

    public interface IOnBandUpdateListener {
        public void OnBandUpdated(int band, float value);
    }

    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (Wave.this) {
                long start = System.currentTimeMillis();

                calculatePath();

                invalidate();

                long gap = 16 - System.currentTimeMillis() - start;
                postDelayed(this, gap < 0 ? 0 : gap);
            }
        }
    }
}