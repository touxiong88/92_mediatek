
package com.mediatek.factorymode.touchscreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class PointTest extends Activity implements View.OnTouchListener {
    MyView mMyView = null;

    public boolean mRun = false;

    public Random rand;

    public Point PrePoint;

    public double mPointError = 0.0;

    public Bitmap mBitmap;

    public int mBitmapPad = 0;

    private int mRectWidth;

    private int mRectHeight;

    private final static int radius = 20;

    private int dx = 20;

    private int dy = 20;

    private List<Rect> mList = null;

    private List<CircleEntity> mDrawList = null;

    private int mCount = 0;

    private SharedPreferences mSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DisplayMetrics dm = new DisplayMetrics();
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        dm = this.getApplicationContext().getResources().getDisplayMetrics();

        mRectWidth = dm.widthPixels;
        mRectHeight = dm.heightPixels;

        setContentView(mMyView = new MyView(this));
        mMyView.setOnTouchListener(this);
        mList = new ArrayList<Rect>();
        initRect(0, dx * 2, 0, dy * 2);
        initRect(mRectWidth / 2 - dx, mRectWidth / 2 + dx, 0, dy * 2);
        initRect(mRectWidth - dx * 2, mRectWidth, 0, dy * 2);
        initRect(0, dx * 2, mRectHeight / 2 - dy, mRectHeight / 2 + dy);
        initRect(mRectWidth / 2 - dx, mRectWidth / 2 + dx, mRectHeight / 2 - dy, mRectHeight / 2
                + dy);
        initRect(mRectWidth - dx * 2, mRectWidth, mRectHeight / 2 - dy, mRectHeight / 2 + dy);
        initRect(0, dx * 2, mRectHeight - dy * 2, mRectHeight);
        initRect(mRectWidth / 2 - dx, mRectWidth / 2 + dx, mRectHeight - dy * 2, mRectHeight);
        initRect(mRectWidth - dx * 2, mRectWidth, mRectHeight - dy * 2, mRectHeight);

        mDrawList = new ArrayList<CircleEntity>();
        initCircle(dx, dy);
        initCircle(mRectWidth / 2, dy);
        initCircle(mRectWidth - dx, dy);
        initCircle(dx, mRectHeight / 2);
        initCircle(mRectWidth / 2, mRectHeight / 2);
        initCircle(mRectWidth - dx, mRectHeight / 2);
        initCircle(dx, mRectHeight - dy);
        initCircle(mRectWidth / 2, mRectHeight - dy);
        initCircle(mRectWidth - dx, mRectHeight - dy);
    }

    public class CircleEntity {
        CircleEntity(int x, int y) {
            dx = x;
            dy = y;
            isselect = false;
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setARGB(255, 255, 0, 0);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2);
        }

        Paint mPaint;

        int dx;

        int dy;

        boolean isselect;
    }

    public void initCircle(int dx, int dy) {
        CircleEntity ce = new CircleEntity(dx, dy);
        mDrawList.add(ce);
    }

    public void initRect(int left, int right, int top, int bottom) {
        Rect rect = new Rect();
        rect.left = left;
        rect.right = right;
        rect.top = top;
        rect.bottom = bottom;
        mList.add(rect);
    }

    public boolean IsCollision(int x, int y, int count) {
        if (count < 9) {
            Rect rect = mList.get(count);
            if (rect.left < x && rect.right > x && rect.top < y && rect.bottom > y) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public class MyView extends View {
        public MyView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (int i = 0; i < mDrawList.size(); i++) {
                CircleEntity ce = mDrawList.get(i);
                canvas.drawCircle(ce.dx, ce.dy, radius, ce.mPaint);
            }
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (MotionEvent.ACTION_DOWN == event.getAction()) {
            int xTouch = (int) event.getX();
            int yTouch = (int) event.getY();
            boolean b = IsCollision(xTouch, yTouch, mCount);
            Log.i("hewei", "onTouch");
            if (b == true) {
                CircleEntity ce = mDrawList.get(mCount);
                ce.mPaint.setARGB(255, 0, 255, 0);
                if (mCount >= 8) {
                    Intent intent = new Intent();
                    intent.setClassName(this, "com.mediatek.factorymode.touchscreen.LineTest");
                    startActivity(intent);
                    finish();
                    return true;
                } else {
                    mCount++;
                }
            }
        }
        mMyView.invalidate();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Utils.SetPreferences(this, mSp, R.string.touchscreen1_name, AppDefine.FT_FAILED);
            finish();
        }
        return true;
    }
}
