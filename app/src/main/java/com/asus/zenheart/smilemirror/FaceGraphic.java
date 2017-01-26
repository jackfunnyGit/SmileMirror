/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.asus.zenheart.smilemirror.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

import java.util.ArrayList;
import java.util.Random;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float BOX_STROKE_WIDTH = 2.0f;
    private static final int BOX_STROKE_ALPHA = 102;// 60% opacity

    // ShihJie: add the value for L1 to L4
    private static final float SMILE_LEVEL_L4 = 0.7f;
    private static final float SMILE_LEVEL_L3 = 0.5f;
    private static final float SMILE_LEVEL_L2 = 0.3f;

    // ShihJie: default face window size
    private static final float ORIGINAL_FACE_WIDTH = 640.0f;
    private static final float ORIGINAL_FACE_HEIGHT = 800.0f;

    // ShihJie: face window scale size
    private static final float FACE_SCALE_SIZE = 0.8f;

    //ShihJie: paint shadow
    private static final float SHADOW_RADIUS = 2.5f;
    private static final float SHADOW_X_POSITION = 5.0f;
    private static final float SHADOW_Y_POSITION = 5.0f;

    // ShihJie: heart effects appear range
    private static final int HEART_APPEAR_HEIGHT = 400;

    // ShihJie: Each second per frame number
    private static final int SMILE_EFFECT_FRAME_NUMBER = 32;

    private Context mContext;
    private Resources mResources;
    private Paint mBoxPaint;
    private Paint mFaceTextPaint;
    private volatile Face mFace;
    private int mFaceId;


    private int mSmileEffectFrame[];// Store smile effect frame
    private int mSmileEffectFrameCount = 0;//Calculate which frame is showing now
    private BitmapFactory.Options mDoubleScaleOptions;
    private BitmapFactory.Options mNoScaleOptions;

    // ShihJie: Open or Close single face effect
    public static boolean sFaceEffect = true;

    // ShihJie: draw the crown or not
    private boolean mCrown = false;

    /**
     * for eyeBlink feature and may be removed or fixed in the future
     */
    //TODO it will be used in the future
    // ShihJie: Eye blink effect resource
    private static final int HEART_CHOICES[] = {
            +R.drawable.effect_heart_01,
            +R.drawable.effect_heart_02,
            +R.drawable.effect_heart_03,
            +R.drawable.effect_heart_04,
            +R.drawable.effect_heart_05
    };
    // ShihJie: Eye blink effect value
    private static final double OPEN_THRESHOLD = 0.6;
    private static final double CLOSE_THRESHOLD = 0.35;
    // ShihJie: Eyes open and close time(per/frame)
    private int mEyeTime = 0;
    // ShihJie: Eyes blink mEyeState
    private int mEyeState = 0;
    private ArrayList<Bitmap> mBitmapList = new ArrayList<>();

    FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);
        mContext = context;
        mResources = context.getResources();
        initPaint();
        mSmileEffectFrame = getSmileEffectResourceIndex(context, SMILE_EFFECT_FRAME_NUMBER);

        mDoubleScaleOptions = new BitmapFactory.Options();
        mDoubleScaleOptions.inScaled = false;
        mDoubleScaleOptions.inSampleSize = 2;

        mNoScaleOptions = new BitmapFactory.Options();
        mNoScaleOptions.inScaled = false;
        mNoScaleOptions.inSampleSize = 1;
    }

    void setId(int id) {
        mFaceId = id;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas, Context context) {
        Face face = mFace;

        if (face == null) {
            return;
        }
        if (FaceTrackerActivity.sIfDraw == FaceTrackerActivity.MODE_COUCH) {
            return;
        }

        // Declare each values in the face window.
        float smileScore = AverageUtil.movingAverage(face.getIsSmilingProbability());
        float rightEye = face.getIsRightEyeOpenProbability();
        float leftEye = face.getIsLeftEyeOpenProbability();
        float faceWidth = face.getWidth();
        float faceHeight = face.getHeight();

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + faceWidth / 2);
        float y = translateY(face.getPosition().y + faceHeight / 2);

        // Draws a bounding box around the face.
        float xOffset = scaleX(faceWidth / 2.0f) * FACE_SCALE_SIZE;
        float yOffset = scaleY(faceHeight / 2.0f) * FACE_SCALE_SIZE;
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        float faceWindowWidth = right - left;
        float faceWindowHeight = bottom - top;


        setDrawPaint(context, mBoxPaint, smileScore);
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        final float smileTextPadding = context.getResources().getDimensionPixelSize(
                R.dimen.smile_text_padding);
        // ++ShihJie: Draw face effect(one face).
        if (sFaceEffect) {
            // Draw smile icon and smile text.
            drawSmileLevelIcon(mResources, canvas, faceWindowWidth, faceWindowHeight,
                    right, top, mNoScaleOptions);
            drawSmileText(context.getResources(), canvas, mFaceTextPaint, face
                    .getIsSmilingProbability(), x, top - smileTextPadding);

            // Draw the smile effects in the singlePlayer mode.
            if (smileScore > SMILE_LEVEL_L4) {
                drawSmileEffectBitmap(context, canvas, faceWindowWidth, faceWindowHeight, x, y,
                        mDoubleScaleOptions);
                mSmileEffectFrameCount++;
                if (mSmileEffectFrameCount >= mSmileEffectFrame.length) {
                    mSmileEffectFrameCount = 0;
                }
            } else {
                mSmileEffectFrameCount = 0;
            }
        } else {
            // Draw Crown in the multiPlayer mode.
            if (mCrown) {
                drawCrown(mResources, canvas, faceWindowWidth, faceHeight,
                        smileTextPadding, x, top);
                // Reset the crown.
                mCrown = false;
            }
        }
        // --ShihJie


    }

    //TODO: This feature maybe will be used in the future and this function is not ready yet.
    private void drawEyeBlink() {
/*
        //++ShihJie: Draws blink picture, but now this feature is closed.
        // each effect shown 10 frame
        if (mEyeTime <= 0) {
            mEyeTime = 10;
        }
        switch (mEyeState) {
            case 0:
                if ((leftEye > OPEN_THRESHOLD) && (rightEye > OPEN_THRESHOLD)) {
                    // Both eyes are initially open

                    mEyeTime = 10;
                    mEyeState = 1;
                } else {
                    mEyeTime--;
                    mEyeState = 0;
                }
                break;

            case 1:
                if ((leftEye < CLOSE_THRESHOLD) && (rightEye < CLOSE_THRESHOLD)) {
                    // Both eyes become closed

                    mEyeTime = 10;
                    mEyeState = 2;
                } else {
                    mEyeTime--;
                    mEyeState = 1;
                }
                break;

            case 2:
                if ((leftEye > OPEN_THRESHOLD) && (rightEye > OPEN_THRESHOLD)) {
                    // Both eyes are open again

                    for (int i = 0; i < 5; i++) {
                        if (mBitmapList.size() < 5) {
                            Bitmap heart = BitmapFactory.decodeResource(context.getResources(),
                                    HEART_CHOICES[i]);
                            Bitmap output = Bitmap.createScaledBitmap(heart, heart.getWidth() / 40,
                                    heart.getHeight() / 40, false);
                            mBitmapList.add(output);
                        }
                    }
                    drawHeart(mBitmapList, canvas, 20, x, y);
                    mEyeTime = 10;
                    mEyeState = 0;
                } else {
                    mEyeTime--;
                    mEyeState = 2;
                }
                break;
        }
*/
    }

    /**
     * {@link #mFaceTextPaint} is used to draw text above the face box
     * {@link #mBoxPaint}  is used to draw face box
     */
    private void initPaint() {
        final float faceTextSize = mResources.getDimensionPixelSize(R.dimen.smile_text_size);
        mFaceTextPaint = new Paint();
        mFaceTextPaint.setTextSize(faceTextSize);
        mFaceTextPaint.setColor(Color.WHITE);
        mFaceTextPaint.setTextAlign(Paint.Align.CENTER);
        mFaceTextPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_X_POSITION, SHADOW_Y_POSITION,
                R.color.smile_text_shadow_color);

        mBoxPaint = new Paint();
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
        mBoxPaint.setAlpha(BOX_STROKE_ALPHA);
    }

    /**
     * Setup the drw paint color.
     *
     * @param context            Class for accessing an application's resources.
     * @param paint              The drawing paint.
     * @param smilingProbability The score of the face smile.
     */
    private void setDrawPaint(@NonNull Context context, @NonNull Paint paint,
                              float smilingProbability) {
        final int smileLevelOneColor = ContextCompat.getColor(context,
                R.color.smile_level_one_face_color);
        final int smileLevelTwoColor = ContextCompat.getColor(context,
                R.color.smile_level_two_face_color);
        final int smileLevelThreeColor = ContextCompat.getColor(context,
                R.color.smile_level_three_face_color);
        final int smileLevelFourColor = ContextCompat.getColor(context,
                R.color.smile_level_four_face_color);

        if (smilingProbability >= SMILE_LEVEL_L4) {
            paint.setColor(smileLevelFourColor);
        } else if (smilingProbability < SMILE_LEVEL_L4 && smilingProbability >= SMILE_LEVEL_L3) {
            paint.setColor(smileLevelThreeColor);
        } else if (smilingProbability < SMILE_LEVEL_L3 && smilingProbability >= SMILE_LEVEL_L2) {
            paint.setColor(smileLevelTwoColor);
        } else if (smilingProbability < SMILE_LEVEL_L2) {
            paint.setColor(smileLevelOneColor);
        }
    }

    /**
     * Blink effect.
     *
     * @param bitmapArrayList List of the picture animation.
     * @param canvas          Draws the smile object in the camera preview.
     * @param count           Numbers of the heart in the view.
     * @param positionX       The X coordinate of the center in the view.
     * @param positionY       The Y coordinate of the center in the view.
     */
    //TODO: This feature maybe will be used in the future and this function is not ready yet.
    private void drawHeart(@NonNull Canvas canvas, ArrayList<Bitmap> bitmapArrayList, int count,
                           float positionX, float positionY) {
        Random r = new Random();
        // Pop heart range
        int Low = -(HEART_APPEAR_HEIGHT);
        int High = HEART_APPEAR_HEIGHT;
        for (int i = 0; i < count; i++) {
            float x = r.nextInt(High - Low + 1) + Low;
            float y = r.nextInt(High - Low + 1) + Low;
            int z = (int) (Math.random() * 5);
            canvas.drawBitmap(bitmapArrayList.get(z), positionX + x, positionY + y, null);
        }
    }


    /**
     * Smile effect bitmap each frame data.
     *
     * @param context       Class for accessing an application's resources.
     * @param canvas        Draws the smile object in the camera preview.
     * @param width         Max effect frame number.
     * @param height        Max effect frame number.
     * @param drawPositionX Max effect frame number.
     * @param drawPositionY Max effect frame number.
     */
    private void drawSmileEffectBitmap(@NonNull Context context, @NonNull Canvas canvas,
                                       float width, float height, float drawPositionX,
                                       float drawPositionY, BitmapFactory.Options options) {
        final float scaleWidth = width / ORIGINAL_FACE_WIDTH;
        final float scaleHeight = height / ORIGINAL_FACE_HEIGHT;

        Bitmap smileEffect = BitmapFactory.decodeResource(context.getResources(),
                mSmileEffectFrame[mSmileEffectFrameCount], options);
        Bitmap output = Bitmap.createScaledBitmap(smileEffect,
                (int) (scaleWidth * smileEffect.getWidth()),
                (int) (scaleHeight * smileEffect.getHeight()), false);
        canvas.drawBitmap(output, drawPositionX - (output.getWidth() / 2), drawPositionY, null);
        smileEffect.recycle();
        output.recycle();
    }

    /**
     * Smile Icon on the upper top point.
     *
     * @param resources Class for accessing an application's resources.
     * @param canvas    Draws the smile object in the camera preview.
     * @param width     The face windows width.
     * @param height    The face windows height.
     * @param x         The X coordinate of the upper top point of the face window.
     * @param y         The Y coordinate of the upper top point of the face window.
     * @param options   The options which can set resource size.
     */
    private void drawSmileLevelIcon(@NonNull Resources resources, @NonNull Canvas canvas,
                                    float width, float height, float x, float y,
                                    BitmapFactory.Options options) {
        final float smileProbability = mFace.getIsSmilingProbability();
        final int smileLevelFour = R.drawable.l4;
        final int smileLevelThree = R.drawable.l3;
        final int smileLevelTwo = R.drawable.l2;
        final int smileLevelOne = R.drawable.l1;

        Bitmap smileIcon;
        Bitmap output;

        if (smileProbability > SMILE_LEVEL_L4) {
            smileIcon = BitmapFactory.decodeResource(resources, smileLevelFour, options);
        } else if (smileProbability < SMILE_LEVEL_L4 && smileProbability > SMILE_LEVEL_L3) {
            smileIcon = BitmapFactory.decodeResource(resources, smileLevelThree, options);
        } else if (smileProbability < SMILE_LEVEL_L3 && smileProbability > SMILE_LEVEL_L2) {
            smileIcon = BitmapFactory.decodeResource(resources, smileLevelTwo, options);
        } else {
            smileIcon = BitmapFactory.decodeResource(resources, smileLevelOne, options);
        }
        output = Bitmap.createScaledBitmap(smileIcon,
                (int) (smileIcon.getWidth() * (width / ORIGINAL_FACE_WIDTH)),
                (int) (smileIcon.getHeight() * (height / ORIGINAL_FACE_HEIGHT)), false);
        canvas.drawBitmap(output, x - output.getWidth(), y, null);
        smileIcon.recycle();
        output.recycle();
    }

    /**
     * Smile comment toast_text.
     *
     * @param resources        Class for accessing an application's resources.
     * @param canvas           Draws the smile object in the camera preview.
     * @param smileProbability The face smile score.
     * @param x                The X coordinate of the toast_text on the face window.
     * @param y                The Y coordinate of the toast_text on the face window.
     */
    private void drawSmileText(@NonNull Resources resources, @NonNull Canvas canvas, Paint paint,
                               float smileProbability, float x, float y) {
        final String smileL4 = resources.getString(R.string.smile_level_four_text);
        final String smileL3 = resources.getString(R.string.smile_level_three_text);
        final String smileL2 = resources.getString(R.string.smile_level_two_text);
        final String smileL1 = resources.getString(R.string.smile_level_one_text);

        String smileText;

        if (smileProbability > SMILE_LEVEL_L4) {
            smileText = smileL4;
        } else if (smileProbability < SMILE_LEVEL_L4 && smileProbability > SMILE_LEVEL_L3) {
            smileText = smileL3;
        } else if (smileProbability < SMILE_LEVEL_L3 && smileProbability > SMILE_LEVEL_L2) {
            smileText = smileL2;
        } else {
            smileText = smileL1;
        }
        canvas.drawText(smileText, x, y, paint);
    }

    /**
     * Crown will shown in multi player mode.
     *
     * @param resources Class for accessing an application's resources.
     * @param canvas    Draws the smile object in the camera preview.
     * @param width     The face windows width.
     * @param height    The face windows height.
     * @param padding   Padding with face window and crown
     * @param x         The X coordinate of the toast_text on the face window.
     * @param y         The Y coordinate of the toast_text on the face window.
     */
    private void drawCrown(@NonNull Resources resources, @NonNull Canvas canvas, float width,
                           float height, float padding, float x, float y) {
        final int crownResource = R.drawable.crown;

        Bitmap crown = BitmapFactory.decodeResource(resources, crownResource, mNoScaleOptions);
        Bitmap output = Bitmap.createScaledBitmap(crown, (int) (crown.getWidth() * (mFace
                        .getWidth() / ORIGINAL_FACE_WIDTH)),
                (int) (crown.getHeight() * (width / ORIGINAL_FACE_HEIGHT)),
                false);
        canvas.drawBitmap(output, x - output.getWidth() / 2, y - (padding *
                (height / ORIGINAL_FACE_HEIGHT)) - output.getHeight(), null);
        crown.recycle();
        output.recycle();
    }

    /**
     * Smile effect each frame data.
     *
     * @param context Class for accessing an application's resources.
     * @param maxsize Max effect frame number.
     */
    private static int[] getSmileEffectResourceIndex(@NonNull Context context, int maxsize) {
        final String drawable = "drawable";
        final String resourceName = "curve_bling_source_%03d";
        final Resources resources = context.getResources();
        final int resourceIds[] = new int[maxsize];
        String name;
        for (int i = 0; i < maxsize; i++) {
            name = String.format(resourceName, i);
            resourceIds[i] = resources.getIdentifier(name, drawable, context.getPackageName());
        }
        return resourceIds;
    }

    /**
     * Get the each face windows data in the view
     */
    public Face getFaceData() {
        return mFace;
    }

    /**
     * Draw the crown in this face
     */
    public void drawTheCrown() {
        mCrown = true;
    }
}

