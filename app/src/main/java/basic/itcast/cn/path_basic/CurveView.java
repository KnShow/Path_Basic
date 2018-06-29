package basic.itcast.cn.path_basic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

/**
 * 基础贝塞尔曲线的绘制
 */
public class CurveView extends View {


    private int mWidth;//屏幕宽
    private int mHeight;//屏幕高
    private Paint mPaint;//画笔
    Point mStartPoint = new Point();//起始点
    Point mEndPoint = new Point();//结束点
    Point mControlPoint = new Point();//控制点(锚点)

    public CurveView(Context context) {
        this(context, null);
    }

    public CurveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mWidth = displayMetrics.widthPixels;
        mHeight = displayMetrics.heightPixels;

        mStartPoint.set(100, mHeight / 2);
        mEndPoint.set(mWidth - 100, mHeight / 2);
        mControlPoint.set(mWidth / 2, 100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawQuadraticBezier(canvas);
    }

    private void drawQuadraticBezier(Canvas canvas) {
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.FILL);
        //绘制一个小的实心圆作为控制点
        // mControlPoint.x,圆心X、Y轴坐标,半径，画笔
        canvas.drawCircle(mControlPoint.x, mControlPoint.y, 10, mPaint);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.FILL);
        /**
         * 绘制一个三角形，通过划线的方式
         */
        float[] lines = {mStartPoint.x, mStartPoint.y, mControlPoint.x, mControlPoint.y,
                mControlPoint.x, mControlPoint.y, mEndPoint.x, mEndPoint.y,
                mEndPoint.x, mEndPoint.y, mStartPoint.x, mStartPoint.y};
        canvas.drawLines(lines, mPaint);
        //贝塞尔曲线的绘制
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        Path path = new Path();
        path.moveTo(mStartPoint.x, mStartPoint.y);
        path.quadTo(mControlPoint.x, mControlPoint.y, mEndPoint.x, mEndPoint.y);
        canvas.drawPath(path, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) (event.getX());
                int moveY = (int) (event.getY());
                //控制点的坐标随着手指的移动而改变
                mControlPoint.x = moveX;
                mControlPoint.y = moveY;
                invalidate();//重绘
                break;
        }
        return true;
    }
}

