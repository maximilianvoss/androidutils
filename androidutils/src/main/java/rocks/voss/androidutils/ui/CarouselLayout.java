package rocks.voss.androidutils.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import rocks.voss.androidutils.R;

public class CarouselLayout extends LinearLayout {
    private final GestureDetector gestureDetector;
    private int active = 0;
    private Paint indicatorColor = new Paint();

    public CarouselLayout(Context context) {
        this(context, null);
    }

    public CarouselLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarouselLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CarouselLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWillNotDraw(false);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CarouselLayout, defStyleAttr, defStyleRes);
        indicatorColor.setColor(a.getColor(R.styleable.CarouselLayout_indicatorColor, Color.WHITE));
        a.recycle();

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float direction = e1.getX() - e2.getX();
                if (direction > 0) {
                    active++;
                    if (active >= getChildCount()) {
                        active = 0;
                    }
                } else {
                    active--;
                    if (active < 0) {
                        active = getChildCount() - 1;
                    }
                }
                updateSlide();
                invalidate();
                return true;
            }
        });
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawIndicator(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void updateSlide() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null) {
                if (i == active) {
                    child.setVisibility(VISIBLE);
                } else {
                    child.setVisibility(GONE);
                }
            }
        }
    }

    private void drawIndicator(Canvas canvas) {
        int count = getChildCount();
        float radius = 10f;
        float size = radius * 4;
        float start = (getWidth() - size * (count - 1)) / 2;

        for (int i = 0; i < count; i++) {
            if (active == i) {
                indicatorColor.setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                indicatorColor.setStyle(Paint.Style.STROKE);
            }
            canvas.drawCircle(start + size * i, canvas.getHeight() - 20, radius, indicatorColor);
        }
    }
}
