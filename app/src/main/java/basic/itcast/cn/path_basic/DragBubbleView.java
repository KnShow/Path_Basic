package basic.itcast.cn.path_basic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PointFEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

public class DragBubbleView extends View{

    /**
     * 气泡默认状态--静止
     */
    private final int BUBBLE_STATE_DEFAUL = 0;
    /**
     * 气泡相连
     */
    private final int BUBBLE_STATE_CONNECT = 1;
    /**
     * 气泡分离
     */
    private final int BUBBLE_STATE_APART = 2;
    /**
     * 气泡消失
     */
    private final int BUBBLE_STATE_DISMISS = 3;

    /**
     * 气泡半径
     */
    private float mBubbleRadius;
    /**
     * 气泡颜色
     */
    private int mBubbleColor;
    /**
     * 气泡消息文字
     */
    private String mTextStr;
    /**
     * 气泡消息文字颜色
     */
    private int mTextColor;
    /**
     * 气泡消息文字大小
     */
    private float mTextSize;
    /**
     * 不动气泡的半径
     */
    private float mBubStillRadius;
    /**
     * 可动气泡的半径
     */
    private float mBubMoveableRadius;
    /**
     * 不动气泡的圆心
     */
    private PointF mBubStillCenter;
    /**
     * 可动气泡的圆心
     */
    private PointF mBubMoveableCenter;
    /**
     * 气泡的画笔
     */
    private Paint mBubblePaint;
    /**
     * 贝塞尔曲线path
     */
    private Path mBezierPath;

    private Paint mTextPaint;

    private Rect mTextRect;

    private Paint mBurstPaint;

    private Rect mBurstRect;

    /**
     * 气泡状态标志   默认为静止状态
     */
    private int mBubbleState = BUBBLE_STATE_DEFAUL;
    /**
     * 两气泡圆心距离
     */
    private float mDist;
    /**
     * 气泡相连状态最大圆心距离
     *  两个控制点之间的连线
     *   气泡可以拖拽的最远距离
     */
    private float mMaxDist;
    /**
     * 手指触摸偏移量
     */
    private final float MOVE_OFFSET;

    /**
     *  气泡爆炸的bitmap数组
     */
    private Bitmap[] mBurstBitmapsArray;
    /**
     * 是否在执行气泡爆炸动画
     */
    private boolean mIsBurstAnimStart = false;

    /**
     * 当前气泡爆炸图片index
     */
    private int mCurDrawableIndex;

    /**
     *  气泡爆炸的图片id数组
     */
    private int[] mBurstDrawablesArray = {R.drawable.burst_1, R.drawable.burst_2
            , R.drawable.burst_3, R.drawable.burst_4, R.drawable.burst_5};

    public DragBubbleView(Context context) {
        this(context,null);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public DragBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.DragBubbleView,defStyleAttr,0);
        mBubbleRadius = array.getDimension(R.styleable.DragBubbleView_bubble_radius,mBubbleRadius);
        mBubbleColor = array.getColor(R.styleable.DragBubbleView_bubble_color, Color.RED);
        mTextStr = array.getString(R.styleable.DragBubbleView_bubble_text);
        mTextSize = array.getDimension(R.styleable.DragBubbleView_bubble_textSize,mTextSize);
        mTextColor = array.getColor(R.styleable.DragBubbleView_bubble_textColor, Color.WHITE);
        array.recycle();

        mBubStillRadius = mBubbleRadius;
        mBubMoveableRadius = mBubStillRadius;
        mMaxDist = 8 * mBubbleRadius;

        MOVE_OFFSET = mMaxDist / 4;

        //抗锯齿
        mBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBubblePaint.setColor(mBubbleColor);
        mBubblePaint.setStyle(Paint.Style.FILL);
        mBezierPath = new Path();

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextRect = new Rect();

        mBurstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBurstPaint.setFilterBitmap(true);
        //创建爆炸动画
        mBurstRect = new Rect();
        mBurstBitmapsArray = new Bitmap[mBurstDrawablesArray.length];
        for (int i = 0; i < mBurstDrawablesArray.length; i++) {
            //将气泡爆炸的drawable转为bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mBurstDrawablesArray[i]);
            mBurstBitmapsArray[i] = bitmap;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initView(w,h);
    }

    /**
     * 初始化气泡位置
     * @param w
     * @param h
     */
    private void initView(int w, int h) {

        //设置两气泡圆心初始坐标
        if(mBubStillCenter == null){
            mBubStillCenter = new PointF(w / 2,h / 2);
        }else{
            mBubStillCenter.set(w / 2,h / 2);
        }

        if(mBubMoveableCenter == null){
            mBubMoveableCenter = new PointF(w / 2,h / 2);
        }else{
            mBubMoveableCenter.set(w / 2,h / 2);
        }
        mBubbleState = BUBBLE_STATE_DEFAUL;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
            {
                if(mBubbleState != BUBBLE_STATE_DISMISS){
                    //x和y平方和的二次方根
//                    Math.hypot(x, y);	 √（x²+y²）
                    //触摸点相对于其所在组件原点的x坐标
                    mDist = (float) Math.hypot(event.getX() - mBubStillCenter.x,
                            event.getY() - mBubStillCenter.y);
                    //mBubbleRadius默认为不动气泡的半径
                    if(mDist < mBubbleRadius + MOVE_OFFSET){
                        // 加上MOVE_OFFSET是为了方便拖拽
                        mBubbleState = BUBBLE_STATE_CONNECT;
                    }else{
                        mBubbleState = BUBBLE_STATE_DEFAUL;
                    }
                }
            }
            break;

            case MotionEvent.ACTION_MOVE:
            {
                //因为静止状态下不用重新计算
                if(mBubbleState != BUBBLE_STATE_DEFAUL){
                    //因为当前移动到的点就是我们大圆的圆心
                    mBubMoveableCenter.x = event.getX();
                    mBubMoveableCenter.y = event.getY();
                    //计算圆心距,勾股定理
                    mDist = (float) Math.hypot(event.getX() - mBubStillCenter.x,
                            event.getY() - mBubStillCenter.y);
                    //相连状态下计算各点位置， 只有相连状态下才会计算 不动圆的半径
                    if(mBubbleState == BUBBLE_STATE_CONNECT){
                        // 减去MOVE_OFFSET是为了让不动气泡半径到一个较小值时就直接消失
                        // 或者说是进入分离状态  mMaxDist气泡相连状态下的最大距离
                        if(mDist < mMaxDist - MOVE_OFFSET){
                            //不动汽泡的半径随着圆心距的增大而变小
                            mBubStillRadius = mBubbleRadius - mDist / 8;
                        }else{
                            //分离状态
                            mBubbleState = BUBBLE_STATE_APART;
                        }
                    }
                    invalidate();
                }
            }
            break;

            case MotionEvent.ACTION_UP:
            {
                if(mBubbleState == BUBBLE_STATE_CONNECT){
                    //相连状态下松手,执行恢复动画
                    startBubbleRestAnim();
                }else if(mBubbleState == BUBBLE_STATE_APART){
                    if(mDist < 2 * mBubbleRadius){
                        startBubbleRestAnim();
                    }else{
                        startBubbleBurstAnim();
                    }
                }
            }
            break;
        }
        return true;
    }

    private void startBubbleBurstAnim() {
        //气泡改为消失状态
        mBubbleState = BUBBLE_STATE_DISMISS;
        mIsBurstAnimStart = true;
        //做一个int型属性动画，从0~mBurstDrawablesArray.length结束
        ValueAnimator anim = ValueAnimator.ofInt(0, mBurstDrawablesArray.length);
        //LinearInterpolator 以常量速率改变
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(500);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //设置当前绘制的爆炸图片index
                mCurDrawableIndex = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //修改动画执行标志
                mIsBurstAnimStart = false;
            }
        });
        anim.start();

    }

    private void startBubbleRestAnim() {
        ValueAnimator anim = ValueAnimator.ofObject(new PointFEvaluator(),
                new PointF(mBubMoveableCenter.x,mBubMoveableCenter.y),
                new PointF(mBubStillCenter.x,mBubStillCenter.y));

        anim.setDuration(200);
        //OvershootInterpolator    向前甩一定值后再回到原来位置  可以实现类似弹跳效果
        anim.setInterpolator(new OvershootInterpolator(5f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBubMoveableCenter = (PointF) animation.getAnimatedValue();
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBubbleState = BUBBLE_STATE_DEFAUL;
            }
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1、画静止状态
        // 2、画相连状态
        // 3、画分离状态
        // 4、画消失状态---爆炸动画

        // 1、画拖拽的气泡 和 文字
        if(mBubbleState != BUBBLE_STATE_DISMISS){
            //绘制移动气泡,默认状态下 移动气泡就等于静止气泡
            canvas.drawCircle(mBubMoveableCenter.x,mBubMoveableCenter.y,
                    mBubMoveableRadius,mBubblePaint);

            mTextPaint.getTextBounds(mTextStr,0,mTextStr.length(),mTextRect);

            canvas.drawText(mTextStr,mBubMoveableCenter.x - mTextRect.width() / 2,
                    mBubMoveableCenter.y + mTextRect.height() / 2,mTextPaint);
        }
        // 2、画相连的气泡状态
        if(mBubbleState == BUBBLE_STATE_CONNECT)
        {
            // 1、画静止气泡
            canvas.drawCircle(mBubStillCenter.x,mBubStillCenter.y,
                    mBubStillRadius,mBubblePaint);
            // 2、画相连曲线
            // 计算控制点坐标，两个圆心的中点    控制点坐标
            int iAnchorX = (int) ((mBubStillCenter.x + mBubMoveableCenter.x) / 2);
            int iAnchorY = (int) ((mBubStillCenter.y + mBubMoveableCenter.y) / 2);
            //根据三角函数计算其他两个起始点和两个终点的坐标
            float cosTheta = (mBubMoveableCenter.x - mBubStillCenter.x) / mDist;
            float sinTheta = (mBubMoveableCenter.y - mBubStillCenter.y) / mDist;
            //上半贝塞尔曲线起始点的x，y轴坐标  A
            float iBubStillStartX = mBubStillCenter.x - mBubStillRadius * sinTheta;
            float iBubStillStartY = mBubStillCenter.y + mBubStillRadius * cosTheta;
            //
            float iBubMoveableEndX = mBubMoveableCenter.x - mBubMoveableRadius * sinTheta;
            float iBubMoveableEndY = mBubMoveableCenter.y + mBubMoveableRadius * cosTheta;
            float iBubMoveableStartX = mBubMoveableCenter.x + mBubMoveableRadius * sinTheta;
            float iBubMoveableStartY = mBubMoveableCenter.y - mBubMoveableRadius * cosTheta;
            float iBubStillEndX = mBubStillCenter.x + mBubStillRadius * sinTheta;
            float iBubStillEndY = mBubStillCenter.y - mBubStillRadius * cosTheta;
            //绘制贝塞尔曲线  清除掉path里的线条和曲线，但是不会改变它的fill－type  填充或者空心
            mBezierPath.reset();
            // 画上半弧
            mBezierPath.moveTo(iBubStillStartX,iBubStillStartY);
            mBezierPath.quadTo(iAnchorX,iAnchorY,iBubMoveableEndX,iBubMoveableEndY);
            // 画上半弧     方法实现的仅仅是两点连成一线的绘制线路(相当于从上一条线路移动到了下一跳线路)  quadTo
            mBezierPath.lineTo(iBubMoveableStartX,iBubMoveableStartY);
            mBezierPath.quadTo(iAnchorX,iAnchorY,iBubStillEndX,iBubStillEndY);
            mBezierPath.close();
            canvas.drawPath(mBezierPath,mBubblePaint);
        }

        // 3、画消失状态---爆炸动画

        if(mIsBurstAnimStart){//动画是否在执行  默认为false   设置爆炸区域的坐标
            mBurstRect.set((int)(mBubMoveableCenter.x - mBubMoveableRadius),
                    (int)(mBubMoveableCenter.y - mBubMoveableRadius),
                    (int)(mBubMoveableCenter.x + mBubMoveableRadius),
                    (int)(mBubMoveableCenter.y + mBubMoveableRadius));
            //绘制当前爆炸动画的bitMap 根据动画插值器计算得出
            canvas.drawBitmap(mBurstBitmapsArray[mCurDrawableIndex],null,
                    mBurstRect,mBubblePaint);
        }


        /*// 2、画相连状态
            // 1、画静止气泡

            // 2、画文字

            // 3、画相连曲线

            // 4、画拖拽气泡


        // 3、画分离状态

            // 1、画文字
            // 2、画拖拽气泡

        // 4、画消失状态---爆炸动画*/
    }

    public void reset() {
        initView(getWidth(),getHeight());

        invalidate();
    }
}
