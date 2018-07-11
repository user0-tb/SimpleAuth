/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Modified Copyright 2018 Wilco van Beijnum.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wilco375.onetwoauthenticate.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.wilco375.onetwoauthenticate.R;

/**
 * Circular countdown indicator. The indicator is a filled arc which starts as a full circle ({@code
 * 360} degrees) and shrinks to {@code 0} degrees the less time is remaining.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class CountdownIndicator extends View {
    private final Paint mRemainingSectorPaint;
    private final Paint mBorderPaint;
    private final RectF mDrawingRect;

    /**
     * Countdown phase starting with {@code 1} when a full cycle is remaining and shrinking to
     * {@code 0} the closer the countdown is to zero.
     */
    private double mPhase;

    public CountdownIndicator(Context context) {
        this(context, null);
    }

    public CountdownIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        int color = getResources().getColor(R.color.theme_color);

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStrokeWidth(0); // hairline
        mBorderPaint.setStyle(Style.STROKE);
        mBorderPaint.setColor(color);
        mRemainingSectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRemainingSectorPaint.setColor(mBorderPaint.getColor());

        mDrawingRect = new RectF(1, 1, getWidth() - 1, getHeight() - 1);
    }

    public void setColor(int color) {
        mBorderPaint.setColor(color);
        mRemainingSectorPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float remainingSectorSweepAngle = (float) (mPhase * 360);
        float remainingSectorStartAngle = 270 - remainingSectorSweepAngle;

        // Update drawing rectangle size in case the size of the view has changed
        mDrawingRect.right = getWidth() - 1;
        mDrawingRect.bottom = getHeight() - 1;

        // Draw the sector/filled arc
        // We need to leave the leftmost column and the topmost row out of the drawingRect because
        // in anti-aliased mode drawArc and drawOval use these areas for some reason.
        if (remainingSectorStartAngle < 360) {
            canvas.drawArc(
                    mDrawingRect,
                    remainingSectorStartAngle,
                    remainingSectorSweepAngle,
                    true,
                    mRemainingSectorPaint);
        } else {
            // 360 degrees is equivalent to 0 degrees for drawArc, hence the drawOval below.
            canvas.drawOval(mDrawingRect, mRemainingSectorPaint);
        }

        // Draw the outer border
        canvas.drawOval(mDrawingRect, mBorderPaint);
    }

    /**
     * Sets the phase of this indicator.
     *
     * @param phase phase {@code [0, 1]}: {@code 1} when the maximum amount of time is remaining,
     *              {@code 0} when no time is remaining.
     */
    public void setPhase(double phase) {
        if ((phase < 0) || (phase > 1)) {
            throw new IllegalArgumentException("phase: " + phase);
        }

        mPhase = phase;
        invalidate();
    }
}
