package com.mediatek.factorymode.touchscreen;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;
/**
 * 对角
 * @author tengyi
 *
 */
public class LineTest2 extends Activity {

    public boolean mRun = false;

    private boolean mSuccess = true;

    public double mDiversity = 0;

    public List<List<Point>> mPts1 = new ArrayList<List<Point>>();;

    private List<Point> mTemPoints = new ArrayList<Point>();

    public List<List<Point>> mInput = new ArrayList<List<Point>>();

    private List<List<Point>> mSuperPts = new ArrayList<List<Point>>();

    public List<Point> mInputLeftTop = new ArrayList<Point>();

    public List<Point> mInputRightTop = new ArrayList<Point>();

    public List<Point> mInputLeftBottom = new ArrayList<Point>();

    public List<Point> mInputRightBottom = new ArrayList<Point>();

    public static final int CALCULATE_ID = Menu.FIRST;

    public static final int NEXTLINE_ID = Menu.FIRST + 1;

    private final static int SFVALUE = 160;

    private int mZoom = 4;

    private int mPadding = 80;

    private float mRectWidth;

    private float mRectHeight;

    private int flags = 0;

    private float mRatio = 0;

    private int mOkheight = 100;

    private String mResultString = "";

    private SharedPreferences mSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        DisplayMetrics dm = new DisplayMetrics();
        dm = this.getApplicationContext().getResources().getDisplayMetrics();
        mRectWidth = dm.widthPixels;
        mRectHeight = dm.heightPixels;
        mRatio = mRectWidth / mRectHeight;
        if ((480 == mRectWidth && 800 == mRectHeight)
                || (800 == mRectWidth && 480 == mRectHeight)) {
            mZoom = 2;
        }
        readLine();
        setContentView(new CanvasView(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALCULATE_ID, 0, "Calculate");
        menu.add(0, NEXTLINE_ID, 1, "NextLine");
        return true;
    }

    @Override
    protected void onDestroy() {
        releaseList();
        super.onDestroy();
    }

    public void CalculateDiversity() {
        Point cp = new Point(0, 0);
        if (mInput.size() == 0)
            return;
        distributeAllPoint(mInput);
        double error = 0.0;
        for (int j = 0; j < mInputLeftTop.size(); j++) {
            cp = mInputLeftTop.get(j);
            error += Math.abs(cp.x - cp.y * mRatio);
        }

        for (int j = 0; j < mInputRightTop.size(); j++) {
            cp = mInputRightTop.get(j);
            error += Math.abs(cp.x - (mRectWidth - cp.y * mRatio));
        }

        for (int j = 0; j < mInputLeftBottom.size(); j++) {
            cp = mInputLeftBottom.get(j);
            error += Math.abs(cp.x - (mRectWidth - cp.y * mRatio));
        }

        for (int j = 0; j < mInputRightBottom.size(); j++) {
            cp = mInputRightBottom.get(j);
            error += Math.abs(cp.x - cp.y * mRatio);
        }

        mDiversity = error
                / (mInputLeftTop.size() + mInputRightTop.size() + mInputLeftBottom.size() + mInputRightBottom
                        .size());
        mResultString = String.valueOf(mDiversity);

        int value = (int) Float.parseFloat(mResultString);
        if (value > SFVALUE) {
            flags = 1;
        }

        if (mInputLeftTop.size() < 5 || mInputRightTop.size() < 5 || mInputLeftBottom.size() < 5
                || mInputRightBottom.size() < 5) {
            mSuccess = false;
        }
        Utils.SetPreferences(getApplicationContext(), mSp, R.string.touchscreen2_name,
                mSuccess ? ((flags == 1) ? AppDefine.FT_FAILED : AppDefine.FT_SUCCESS)
                        : AppDefine.FT_FAILED);
        finish();
        mDiversity = 0.0;
    }

    public void releaseList() {
        mPts1 = null;
        mTemPoints = null;
        mSuperPts = null;
        mInput = null;
        mInputLeftTop = null;
        mInputRightTop = null;
        mInputLeftBottom = null;
        mInputRightBottom = null;
    }

    private boolean isBeginOrEndPoint(int x, int y) {
        if ((x < mPadding * 4 || x > mRectWidth - mPadding * 4)
                && (y < mPadding * 4 || y > mRectHeight - mPadding * 4)) {
            return true;
        }
        return false;
    }

    private void distributeAllPoint(List<List<Point>> lists) {
        Point aPoint;
        List<Point> list;
        for (int j = 0; j < lists.size(); j++) {
            list = lists.get(j);
            for (int i = 0; i < list.size(); i++) {
                aPoint = list.get(i);
                if (aPoint.x < mRectWidth / 2) {
                    if (aPoint.y < mRectHeight / 2) {
                        mInputLeftTop.add(aPoint);
                    } else {
                        mInputLeftBottom.add(aPoint);
                    }
                } else {
                    if (aPoint.y < mRectHeight / 2) {
                        mInputRightTop.add(aPoint);
                    } else {
                        mInputRightBottom.add(aPoint);
                    }
                }
            }
        }
    }

    private void readLine() {
        List<Point> temp = new ArrayList<Point>();
        temp.add(new Point(mPadding, mPadding));
        temp.add(new Point((int)mRectWidth - mPadding, (int)mRectHeight - mPadding));
        mPts1.add(temp);
        temp = new ArrayList<Point>();
        temp.add(new Point((int)mRectWidth - mPadding, mPadding));
        temp.add(new Point(mPadding, (int)mRectHeight - mPadding));
        mPts1.add(temp);
    }

    public void readPoints() {
        int x, y;
        List<Point> v = new ArrayList<Point>();
        Point p;
        for (int j = 0; j < mRectHeight - mPadding * 2; j++) {
            x = mPadding;
            y = j + mPadding;
            p = new Point(x, y);
            v.add(p);
        }
        mSuperPts.add(v);
        v = new ArrayList<Point>();

        for (int j = 0; j < mRectWidth - mPadding * 2; j++) {
            x = j + mPadding;
            y = mPadding;
            p = new Point(x, y);
            v.add(p);
        }
        mSuperPts.add(v);
        v = new ArrayList<Point>();

        for (int j = 0; j < mRectHeight - mPadding * 2; j++) {
            x = (int) (mRectWidth - mPadding);
            y = j + mPadding;
            p = new Point(x, y);
            v.add(p);
        }
        mSuperPts.add(v);
        v = new ArrayList<Point>();

        for (int j = 0; j < mRectWidth - mPadding * 2; j++) {
            x = j + mPadding;
            y = (int) (mRectHeight - mPadding);
            p = new Point(x, y);
            v.add(p);
        }
        mSuperPts.add(v);
        v = new ArrayList<Point>();

    }

    class CanvasView extends View {
        private Paint mLinePaint = null;

        private Paint mTextPaint = null;

        private Paint mRectPaint = null;

        private Paint mOkPaint = null;

        private Rect mRect = null;

        CanvasView(Context c) {
            super(c);
            mLinePaint = new Paint();
            mLinePaint.setAntiAlias(true);
            mLinePaint.setStrokeCap(Paint.Cap.ROUND);
            mLinePaint.setStrokeWidth(16);
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(12.0f * mZoom);
            mTextPaint.setARGB(255, 0, 0, 0);
            mRect = new Rect(0, 0, (int)mRectWidth, (int)mRectHeight);
            mRectPaint = new Paint();
            mRectPaint.setARGB(255, 255, 255, 255);
            mOkPaint = new Paint();
            mOkPaint.setTextSize(20);
            mOkPaint.setARGB(255, 0, 0, 255);
            mOkPaint.setAntiAlias(true);
            mOkPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void onDraw(Canvas canvas) {
            canvas.drawColor(0xFFFFFF);

            int i;
            Point p1, p2;
            int height = (int) (mRectHeight / 4);
            canvas.drawRect(mRect, mRectPaint);

            mLinePaint.setARGB(255, 0, 0, 255);
            List<Point> temp;
            for (int j = 0; j < mPts1.size(); j++) {
                temp = mPts1.get(j);
                p1 = temp.get(0);
                p2 = temp.get(1);
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mLinePaint);
            }

            mLinePaint.setARGB(255, 255, 0, 0);
            for (i = 0; i < mTemPoints.size() - 2; i++) {
                p1 = mTemPoints.get(i);
                p2 = mTemPoints.get(i + 1);
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mLinePaint);
            }

            for (int j = 0; j < mInput.size(); j++) {
                List<Point> lscp = mInput.get(j);
                for (int k = 0; k < lscp.size() - 1; k++) {
                    p1 = lscp.get(k);
                    p2 = lscp.get(k + 1);
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mLinePaint);
                }
            }
            if (!mResultString.equals("")) {
                canvas.drawText(
                        getResources().getString(R.string.Offset) + mResultString,
                        20 * mZoom, height, mTextPaint);
            }
            canvas.drawCircle(mRectWidth / 2, mOkheight, 50, mOkPaint);
            canvas.drawText(getResources().getString(R.string.Result),
                    mRectWidth / 2 - 40, mOkheight - 55, mOkPaint);

        }

        @Override
        protected void onSizeChanged(int i, int j, int k, int l) {
            super.onSizeChanged(i, j, k, l);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mSuccess
                            && !(x < mRectWidth / 2 + 50 && x > mRectWidth / 2 - 50
                                    && y < mOkheight + 50 && y > mOkheight - 50))
                        mSuccess = isBeginOrEndPoint(x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    mTemPoints.add(new Point(x, y));
                    break;

                case MotionEvent.ACTION_UP:
                    if (x < mRectWidth / 2 + 50 && x > mRectWidth / 2 - 50
                            && y < mOkheight + 50 && y > mOkheight - 50) {
                        mTemPoints.clear();
                        CalculateDiversity();
                        break;
                    } else if (mSuccess) {
                        mSuccess = isBeginOrEndPoint(x, y);
                    }
                    if (!mSuccess) {
                        showDialog(getString(R.string.Error),
                                getString(R.string.DrawError), true, true);
                    }
                    mInput.add(mTemPoints);
                    mTemPoints = new ArrayList<Point>();
                    break;
            }
            invalidate();
            return true;
        }

        private void showDialog(String title, String msg, boolean posi, boolean negati) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(LineTest2.this);
            dialog.setTitle(title).setMessage(msg);
            if (posi) {
                dialog.setPositiveButton(getString(R.string.Again),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mInput.clear();
                                mTemPoints.clear();
                                mSuccess = true;
                                invalidate();
                            }
                        });
            }
            if (negati) {
                dialog.setNegativeButton(getString(R.string.GoOn),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
            }
            dialog.show();
        }
    }
}
