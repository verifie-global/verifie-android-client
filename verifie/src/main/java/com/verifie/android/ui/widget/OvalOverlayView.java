package com.verifie.android.ui.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.verifie.android.R;

import java.util.concurrent.TimeUnit;

/**
 * Created by hiren on 10/01/16.
 */
public class OvalOverlayView extends View {
    private static final int DEFAULT_INDETERMINATE_PROGRESS_PERIOD = 2000;
    private static final float DEFAULT_INDETERMINATE_ARC_SIZE = 0.2f;

    private Bitmap bitmap;
    private ValueAnimator animator;
    private int colorAnimationOval;
    private float sweepAngle = -1;
    private Paint outsideStroke;
    private Paint animationPaint;
    private RectF ourSideOvalRect;
    private boolean drawDoneSign = false;
    private int width;
    private int height;
    private float tenPercentHeight;
    private float tenPercentWidth;
    private RectF ovalRect;
    private Animator.AnimatorListener animatorListener;

    private boolean mIndeterminate;
    private int mIndeterminatePeriod = DEFAULT_INDETERMINATE_PROGRESS_PERIOD;
    private float mIndeterminateArcSize = DEFAULT_INDETERMINATE_ARC_SIZE;

    private float mProgress;
    private long mLastProgressUpdate;
    private boolean drawProgress = false;

    public void setAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.animatorListener = animatorListener;
    }

    public OvalOverlayView(Context context) {
        super(context);
        init(context);
    }

    public OvalOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OvalOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OvalOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context) {
        colorAnimationOval = ContextCompat.getColor(context, R.color.blue_primary);
    }

    private void setProgressColorRes(@ColorRes int color) {
        setProgressColor(ContextCompat.getColor(getContext(), color));
    }

    private void setProgressColor(@ColorInt int color) {
        colorAnimationOval = color;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (bitmap == null) {
            createWindowFrame();
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
        if (sweepAngle > 0) {
            outsideStroke.setAntiAlias(true);
            outsideStroke.setColor(colorAnimationOval);
            canvas.drawArc(ourSideOvalRect, 270, sweepAngle, false, outsideStroke);
        }
        if (drawDoneSign) {
            drawThickSign(canvas);
        }

        if (drawProgress) {
            long now = System.currentTimeMillis();
            if (mIndeterminate) {
                long duration = now - mLastProgressUpdate;
                if (duration > mIndeterminatePeriod) {
                    mLastProgressUpdate = now;
                    duration = 0;
                }

                if (animationPaint == null) {
                    animationPaint = new Paint();
                    animationPaint.setAntiAlias(true);
                    animationPaint.setColor(colorAnimationOval);
                    animationPaint.setStyle(Paint.Style.STROKE);
                    animationPaint.setStrokeWidth(30f);
                    animationPaint.setStrokeCap(Paint.Cap.ROUND);
                }
                drawCircle(canvas, (float) duration / mIndeterminatePeriod, mIndeterminateArcSize, animationPaint);
                invalidate(); // animation loop
            } else {
                drawCircle(canvas, 0.0f, mProgress, outsideStroke);
            }
        }
    }

    protected void drawCircle(Canvas canvas, float start, float size, Paint paint) {
        canvas.drawArc(ourSideOvalRect, -90.0f + 360.0f * start, 360.0f * size, false, paint);
    }

    public void setIndeterminate(boolean yes) {
        if (mIndeterminate != yes) {
            mIndeterminate = yes;
            invalidate();
        }
    }

    public void setProgress(float progress) {
        mProgress = progress;
        invalidate();
    }

    private void drawThickSign(Canvas canvas) {
        int centerX = width / 2;
        int centerY = height / 2;
        Paint paint = new Paint();
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(25f);
        paint.setColor(colorAnimationOval);
        canvas.drawLine(centerX - tenPercentWidth, centerY, centerX, centerY + tenPercentHeight / 2, paint);
        canvas.drawLine(
                centerX, centerY + tenPercentHeight / 2,
                centerX + tenPercentWidth * 2, (float) (centerY - tenPercentHeight * 0.75),
                paint);
    }

    protected void createWindowFrame() {
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(bitmap);

        RectF outerRectangle = new RectF(0, 0, getWidth(), getHeight());
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        osCanvas.drawRect(outerRectangle, paint);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        width = getWidth();
        height = getHeight();
        ovalRect = new RectF();
        tenPercentWidth = width / 10;
        tenPercentHeight = height / 10;
        ovalRect.left = width - 9 * tenPercentWidth;
        ovalRect.top = height - 8 * tenPercentHeight;
        ovalRect.right = width - tenPercentWidth;
        ovalRect.bottom = height - 2 * tenPercentHeight;
        osCanvas.drawOval(ovalRect, paint);
        outsideStroke = new Paint();
        outsideStroke.setColor(Color.GRAY);
        outsideStroke.setStyle(Paint.Style.STROKE);
        outsideStroke.setStrokeWidth(30f);
        outsideStroke.setStrokeCap(Paint.Cap.ROUND);
        ourSideOvalRect = new RectF(ovalRect.left - 10, ovalRect.top - 10, ovalRect.right + 10, ovalRect.bottom + 10);
        osCanvas.drawOval(ourSideOvalRect, outsideStroke);
    }

    public void startAnimation(int seconds) {
        stopAnim();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(TimeUnit.SECONDS.toMillis(seconds));
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            drawProgress((float) animation.getAnimatedValue());
        });
        animator.addListener(animatorListener);
        animator.start();
    }

    public void startAnimationAsLoading() {
        stopAnim();
        setIndeterminate(true);
        drawProgress = true;
        invalidate();
    }

    public void stopAnim() {
        drawDoneSign = false;
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            animator = null;
            drawProgress(0f);
        }
    }

    private void drawProgress(float animatedValue) {
        sweepAngle = 360 * animatedValue;
        if (animatedValue == 1f) {
            if (animatorListener != null) {
                animatorListener.onAnimationEnd(animator);
            }
        }
        invalidate();
    }

    private void drawProgressLoading(float animatedValue) {
        sweepAngle = 360 * animatedValue;
        invalidate();
    }

    public void drawDone() {
        stopAnim();
        sweepAngle = 360f;
        drawDoneSign = true;
        invalidate();
    }

    public RectF getVisibleBounds() {
        return ovalRect;
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bitmap = null;
    }

    public boolean isRunningAnimation() {
        return animator != null && animator.isRunning();
    }
}