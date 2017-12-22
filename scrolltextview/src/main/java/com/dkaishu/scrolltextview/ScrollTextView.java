package com.dkaishu.scrolltextview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * * 功能实现：
 * 0.纵向滚动
 * 1.字体大小颜色、滚动速度（时间）、停留时间、是否单行显示、单行显示是否带有省略号、
 * 2.滚动动画开始与停止的监听，点击事件的监听
 * Created by dks on 2017/11/14.
 */

public class ScrollTextView extends View implements View.OnClickListener {
    private static final String TAG = "ScrollTextView";
    /**
     * 默认文字颜色
     */
    private static final int DEFAULT_TEXT_COLOR = Color.BLACK;

    /**
     * 默认文字大小(单位sp)
     */
    private static final int DEFAULT_TEXT_SIZE = 16;

    /**
     * 单行模式
     */
    private static final boolean SINGLE_LINE = true;

    /**
     * 单行显示时，默认带省略号(单行模式下才有效)
     */
    private static final boolean ELLIPSIS = true;

    /**
     * 默认文字滚动时间（滚动控制速度）
     */
    private static final long DEFAULT_SCROLL_TIME = 500;

    /**
     * 默认文字切换间隔时间
     */
    private static final long DEFAULT_SPAN_TIME = 3000;

    /**
     * 文字滚动时间,默认500ms
     */
    private long scrollTime = DEFAULT_SCROLL_TIME;

    /**
     * 文字切换间隔时间,默认3000ms
     */
    private long spanTime = DEFAULT_SPAN_TIME;

    /**
     * 是否单行模式
     */
    private boolean isSingleLine;

    /**
     * 单行显示下是否自带省略号
     */
    private boolean isEllipsis;

    /**
     * 文字绘制画笔
     */
    private Paint mPaint;

    /**
     * 文字高度
     */
    private float mTextHeight;

    /**
     * 文字偏移距离
     */
    private float mTextOffsetY;

    /**
     * 滚动文字集合
     */
    private List<String> mContents;

    /**
     * 滚动文字点击监听集合
     */
    private List<OnScrollClickListener> mScrollClickListeners;

    /**
     * 文字滚动开始、结束监听集合
     */
    private List<OnScrollListener> mScrollListeners;

    /**
     * 文字信息集合
     */
    private Queue<List<TextInfo>> mTextInfos;

    /**
     * 省略号信息集合
     */
    private List<TextInfo> mEllipsisTextInfos;

    /**
     * 省略号宽度
     */
    private float mEllipsisTextWidth;

    /**
     * 滚动的高度
     */
    private int mTop;

    /**
     * 当前显示的文字信息
     */
    private List<TextInfo> mCurrentTextInfos;

    /**
     * 索引信息集合
     */
    private Map<List<TextInfo>, ListenersInfo> mIndexMap;

    public ScrollTextView(Context context) {
        this(context, null);
    }

    public ScrollTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScrollTextLayout, defStyleAttr, 0);
        int textColor = DEFAULT_TEXT_COLOR;
        float textSize = sp2px(context, DEFAULT_TEXT_SIZE);
        if (typedArray != null) {
            textColor = typedArray.getColor(R.styleable.ScrollTextLayout_textColor, textColor);
            textSize = typedArray.getDimension(R.styleable.ScrollTextLayout_textSize, textSize);
            isSingleLine = typedArray.getBoolean(R.styleable.ScrollTextLayout_singleLine, SINGLE_LINE);
            isEllipsis = typedArray.getBoolean(R.styleable.ScrollTextLayout_ellipsis, ELLIPSIS);
            typedArray.recycle();
        }

        mPaint = new Paint();
        mPaint.setColor(textColor);
        mPaint.setTextSize(textSize);
        mPaint.setAntiAlias(true);

        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom - fontMetrics.top;
        mTextOffsetY = -fontMetrics.top;

        mIndexMap = new HashMap<>();
        mTextInfos = new LinkedList<>();
        mEllipsisTextInfos = new ArrayList<>();

        setOnClickListener(this);
    }

    ValueAnimator mValueAnimator;

    Handler mHandler = new Handler();

    Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mTextInfos.size() > 1) {
                mValueAnimator = ValueAnimator.ofFloat(0.0f, -1.0f);
                mValueAnimator.setDuration(scrollTime);
                OnScrollListener sl = null;
                if (null != mIndexMap.get(mCurrentTextInfos))
                    sl = mIndexMap.get(mCurrentTextInfos).getScrollListener();
                final OnScrollListener finalSl = sl;
                mValueAnimator.addListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (finalSl != null) finalSl.onScrollStart(mCurrentTextInfos);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mTop = 0;
                        mCurrentTextInfos = mTextInfos.poll();
                        mTextInfos.offer(mCurrentTextInfos);
                        if (finalSl != null) finalSl.onScrollEnd(mCurrentTextInfos);
                        startTextScroll();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mTop = 0;
                    }
                });
            }

            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (Float) animation.getAnimatedValue();
                    mTop = (int) (value * (mTextHeight + getPaddingTop() + getPaddingBottom()));
                    invalidate();
                }

            });
            mValueAnimator.start();
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        int textMaxWidth = 0;
        if (mContents != null && mContents.size() > 0) {
            textMaxWidth = textTypeSetting(measuredWidth - getPaddingLeft() - getPaddingRight(), mContents);
            mCurrentTextInfos = mTextInfos.poll();
            mTextInfos.offer(mCurrentTextInfos);
        }
        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            measuredWidth = textMaxWidth + getPaddingLeft() + getPaddingRight();
        }
        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED) {
            measuredHeight = (int) (mTextHeight + getPaddingBottom() + getPaddingTop());
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
        startTextScroll();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mCurrentTextInfos != null && mCurrentTextInfos.size() > 0) {
            for (TextInfo textInfo : mCurrentTextInfos) {
                canvas.drawText(textInfo.text,
                        textInfo.x + getPaddingLeft(),
                        textInfo.y + getPaddingTop() + mTop,
                        mPaint);
            }
        }
        if (mTextInfos.size() > 1) {
            List<TextInfo> nextTextInfos = mTextInfos.peek();
            if (nextTextInfos != null && nextTextInfos.size() > 0) {
                for (TextInfo textInfo : nextTextInfos) {
                    canvas.drawText(textInfo.text, textInfo.x + getPaddingLeft(), textInfo.y + getPaddingTop() + mTop
                            + mTextHeight + getPaddingTop() + getPaddingBottom(), mPaint);
                }
            }
        }
    }

    /**
     * @param list 滚动内容集合
     */
    public void setTextContent(List<String> list) {
        setTextContent(list, null, null);
    }

    /**
     * @param list                 滚动内容集合
     * @param scrollClickListeners 滚动内容的点击监听集合 @nullable
     */
    public void setTextContent(List<String> list, List<OnScrollClickListener> scrollClickListeners) {
        setTextContent(list, scrollClickListeners, null);
    }

    /**
     * @param list                 滚动内容集合
     * @param scrollClickListeners 滚动内容的点击监听集合 @nullable
     * @param scrollListeners      滚动动画开始与停止监听集合 @nullable
     */
    public void setTextContent(List<String> list, List<OnScrollClickListener> scrollClickListeners
            , List<OnScrollListener> scrollListeners) {
        this.mContents = list;
        this.mScrollClickListeners = scrollClickListeners;
        this.mScrollListeners = scrollListeners;
        requestLayout();
        invalidate();
    }

    /**
     * 设置文字的滚动时间，以控制滚动速度，必须小于等于文字滚动的间隔时间spanTime
     *
     * @param scrollTime 单位毫秒，默认500ms
     */
    public void setScrollTime(long scrollTime) {
        this.scrollTime = scrollTime;
    }

    /**
     * 设置文字滚动的间隔时间
     *
     * @param spanTime 单位毫秒，默认3000ms,必须大于等于文字的滚动时间scrollTime
     */
    public void setSpanTime(long spanTime) {
        this.spanTime = spanTime;
    }

    /**
     * @param color 文字颜色
     */
    public void setTextColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    /**
     * @param size 文字大小
     */
    public void setTextSize(float size) {
        mPaint.setTextSize(size);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom - fontMetrics.top;
        mTextOffsetY = -fontMetrics.top;
        requestLayout();
        invalidate();
    }

    /**
     * 文字开始自动滚动,初始化会自动调用该方法(适当的时机调用)
     */
    public synchronized void startTextScroll() {
        if (spanTime <= scrollTime)
            throw new RuntimeException("spanTime must longer or same as scrollTime");
        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, spanTime);
    }

    /**
     * 文字暂停滚动(适当的时机调用)
     */
    public synchronized void stopTextScroll() {
        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public void onClick(View v) {
        if (mCurrentTextInfos != null && mScrollClickListeners != null) {
            OnScrollClickListener onScrollClickListener = mIndexMap.get(mCurrentTextInfos).getScrollClickListener();
            if (onScrollClickListener != null) {
                onScrollClickListener.onClick();
            }
        }
    }

    /**
     * 文字排版
     *
     * @param maxParentWidth
     * @param list           排版的滚动文字
     * @return 排版文字的宽度
     */
    private int textTypeSetting(float maxParentWidth, List<String> list) {
        // 清空数据及初始化数据
        mTextInfos.clear();
        mIndexMap.clear();
        mEllipsisTextInfos.clear();
        mEllipsisTextWidth = 0f;
        // 初始化省略号
        if (isSingleLine && isEllipsis) {
            String ellipsisText = "...";
            for (int i = 0; i < ellipsisText.length(); i++) {
                char ch = ellipsisText.charAt(i);
                float[] widths = new float[1];
                String srt = String.valueOf(ch);
                mPaint.getTextWidths(srt, widths);
                TextInfo textInfo = new TextInfo();
                textInfo.text = srt;
                textInfo.x = mEllipsisTextWidth;
                textInfo.y = mTextOffsetY;
                mEllipsisTextInfos.add(textInfo);
                mEllipsisTextWidth += widths[0];
            }
        }
        // 文字排版
        float maxWidth = 0;
        // 文字排版最大宽度
        float tempMaxWidth = 0f;
        int index = 0;
        for (String text : list) {
            if (isNullOrEmpty(text)) {
                continue;
            }
            // 排版文字当前的宽度
            float textWidth = 0;
            // 文字信息集合
            List<TextInfo> textInfos = new ArrayList<TextInfo>();
            if (isSingleLine) {
                // 临时文字信息集合
                List<TextInfo> tempTextInfos = new ArrayList<TextInfo>();
                // 单行排不下
                boolean isLess = false;
                // 省略号的起始位置
                float ellipsisStartX = 0;
                for (int j = 0; j < text.length(); j++) {
                    char ch = text.charAt(j);
                    float[] widths = new float[1];
                    String srt = String.valueOf(ch);
                    mPaint.getTextWidths(srt, widths);
                    TextInfo textInfo = new TextInfo();
                    textInfo.text = srt;
                    textInfo.x = textWidth;
                    textInfo.y = mTextOffsetY;
                    textWidth += widths[0];
                    if (textWidth <= maxParentWidth - mEllipsisTextWidth) // 当排版的宽度小于等于最大宽度去除省略号长度时
                    {
                        textInfos.add(textInfo);
                        ellipsisStartX = textWidth;
                    } else if (textWidth <= maxParentWidth) // 当排版宽度小于最大宽度时
                    {
                        tempTextInfos.add(textInfo);
                    } else
                    // 最大宽度排版不下
                    {
                        isLess = true;
                        break;
                    }
                }
                if (isLess) {
                    tempMaxWidth = maxParentWidth;
                    for (TextInfo ellipsisTextInfo : mEllipsisTextInfos) {
                        TextInfo textInfo = new TextInfo();
                        textInfo.text = ellipsisTextInfo.text;
                        textInfo.x = (ellipsisTextInfo.x + ellipsisStartX);
                        textInfo.y = ellipsisTextInfo.y;
                        textInfos.add(textInfo);
                    }
                } else {
                    tempMaxWidth = textWidth;
                    textInfos.addAll(tempTextInfos);
                }
                if (tempMaxWidth > maxWidth) {
                    maxWidth = tempMaxWidth;
                }
                mTextInfos.offer(textInfos);
                if (mScrollClickListeners != null && mScrollClickListeners.size() > index) {
                    mIndexMap.put(textInfos, new ListenersInfo(mScrollClickListeners.get(index), null));
                }
                if (mScrollListeners != null && mScrollListeners.size() > index) {
                    mIndexMap.get(textInfos).setScrollListener(mScrollListeners.get(index));
                }
                index++;
            } else {
                for (int j = 0; j < text.length(); j++) {
                    char ch = text.charAt(j);
                    float[] widths = new float[1];
                    String srt = String.valueOf(ch);
                    mPaint.getTextWidths(srt, widths);
                    TextInfo textInfo = new TextInfo();
                    textInfo.text = srt;
                    textInfo.x = textWidth;
                    textInfo.y = mTextOffsetY;
                    textWidth += widths[0];
                    if (textWidth > maxParentWidth) // 当排版宽度小于最大宽度时
                    {
                        tempMaxWidth = maxParentWidth;
                        mTextInfos.offer(textInfos);
                        if (mScrollClickListeners != null && mScrollClickListeners.size() > index) {
                            mIndexMap.put(textInfos, new ListenersInfo(mScrollClickListeners.get(index), null));
                        }
                        if (mScrollListeners != null && mScrollListeners.size() > index) {
                            mIndexMap.get(textInfos).setScrollListener(mScrollListeners.get(index));
                        }

                        textInfos = new ArrayList<TextInfo>();
                        textInfo.x = 0;
                        textInfo.y = mTextOffsetY;
                        textWidth = widths[0];
                    }
                    textInfos.add(textInfo);
                }
                if (textWidth > tempMaxWidth) {

                    tempMaxWidth = textWidth;
                }
                mTextInfos.offer(textInfos);
                if (tempMaxWidth > maxWidth) {
                    maxWidth = tempMaxWidth;
                }
                if (mScrollClickListeners != null && mScrollClickListeners.size() > index) {
                    mIndexMap.put(textInfos, new ListenersInfo(mScrollClickListeners.get(index), null));
                }
                if (mScrollListeners != null && mScrollListeners.size() > index) {
                    mIndexMap.get(textInfos).setScrollListener(mScrollListeners.get(index));
                }
                index++;
            }
        }
        return (int) maxWidth;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mValueAnimator != null && mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
        mHandler.removeCallbacks(mRunnable);
        mHandler = null;
    }

    /**
     * 描述: sp转px
     *
     * @param context 上下文
     * @param spValue sp值
     * @return 转换后对应的px值
     */
    private float sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return spValue * fontScale;
    }

    /**
     * @param text
     * @return true为空, fale不为空
     */
    private boolean isNullOrEmpty(String text) {
        if (text == null || "".equals(text.trim()) || text.trim().length() == 0 || "null".equals(text.trim())
                || "empty".equals(text)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 描述: 绘制文字信息
     */
    public static class TextInfo {
        /**
         * x坐标
         */
        float x;

        /**
         * y坐标
         */
        float y;

        /**
         * 内容
         */
        String text;

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "TextInfo{" +
                    "x=" + x +
                    ", y=" + y +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    /**
     * 监听者信息
     */
    private class ListenersInfo {
        /**
         * 滚动内容点击监听事件
         */
        private OnScrollClickListener scrollClickListener;

        /**
         * 内容滚动动画开始与结束的监听事件
         */
        private OnScrollListener scrollListener;

        public ListenersInfo(OnScrollClickListener scrollClickListener, OnScrollListener scrollListener) {
            this.scrollClickListener = scrollClickListener;
            this.scrollListener = scrollListener;
        }

        public OnScrollClickListener getScrollClickListener() {
            return scrollClickListener;
        }

        public void setScrollClickListener(OnScrollClickListener scrollClickListener) {
            this.scrollClickListener = scrollClickListener;
        }

        public OnScrollListener getScrollListener() {
            return scrollListener;
        }

        public void setScrollListener(OnScrollListener scrollListener) {
            this.scrollListener = scrollListener;
        }
    }

    /**
     * 描述: 滚动内容点击监听事件
     */
    public interface OnScrollClickListener {
        void onClick();
    }

    /**
     * 描述：内容滚动动画开始与结束的监听事件
     */
    public interface OnScrollListener {
        /**
         * @param passedTextInfos 动画开始前显示的文字
         */
        void onScrollStart(List<TextInfo> passedTextInfos);

        /**
         * @param incommingTextInfos
         */
        void onScrollEnd(List<TextInfo> incommingTextInfos);
    }
}
