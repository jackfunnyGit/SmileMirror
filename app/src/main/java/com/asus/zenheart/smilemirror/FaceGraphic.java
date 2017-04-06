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
import android.graphics.drawable.BitmapDrawable;
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
    //TODO: it should be combined or extracted with those in SmileDegreeCounter
    private static final float SMILE_LEVEL_L4 = 0.7f;
    private static final float SMILE_LEVEL_L3 = 0.5f;
    private static final float SMILE_LEVEL_L2 = 0.3f;

    // ShihJie: default face window size
    private static final float ORIGINAL_FACE_WIDTH = 640.0f;
    private static final float ORIGINAL_FACE_HEIGHT = 800.0f;

    // ShihJie: face window scale size
    private static final float FACE_SCALE_SIZE = 0.8f;

    // ShihJie: smile duration
    private static final int SMILE_DURATION = 30;

    // ShihJie: paint shadow
    private static final float SHADOW_RADIUS = 2.5f;
    private static final float SHADOW_X_POSITION = 5.0f;
    private static final float SHADOW_Y_POSITION = 5.0f;

    // ShihJie: heart effects appear range
    private static final int HEART_APPEAR_HEIGHT = 400;

    // ShihJie: Each second per frame number
    private static final String CURVE_EFFECT = "effect_bling";
    private static final String HEART_EFFECT = "effect_heartbling";
    private static final String STAR_EFFECT = "effect_star_01";
    private static final String LEFT_STAR_EFFECT = "effect_star_03";
    private static final String RIGHT_STAR_EFFECT = "effect_star_02";
    private static final int SMILE_EFFECT_FRAME_NUMBER = 33;
    private static final int HEART_EFFECT_FRAME_NUMBER = 21;
    private static final int STAR_EFFECT_FRAME_NUMBER = 18;

    private Context mContext;
    private Resources mResources;
    private GraphicOverlay mGraphicOverlay;
    private Paint mBoxPaint;
    private Paint mFaceTextPaint;
    private volatile Face mFace;
    private int mFaceId;

    // ShihJie: Array used to store smile effect resources
    private int mSmileEffectFrame[];
    private int mLeftStarEffectFrame[];
    private int mRightStarEffectFrame[];
    private int mSmileEffectFrameCount = 0;
//    private BitmapFactory.Options mDoubleScaleOptions;
//    private BitmapFactory.Options mNoScaleOptions;

    // ShihJie: draw the crown or not
    private int smileCount = 0;
    private boolean mCrown = false;
    private boolean mEffect = false;

    /**
     * for eyeBlink feature and may be removed or fixed in the future
     */
    //TODO it will be used in the future
//    ShihJie: Eye blink effect resource
//    private static final int HEART_CHOICES[] = {
//            +R.drawable.effect_heart_01,
//            +R.drawable.effect_heart_02,
//            +R.drawable.effect_heart_03,
//            +R.drawable.effect_heart_04,
//            +R.drawable.effect_heart_05
//    };
//    ShihJie: Eye blink effect value
//    private static final double OPEN_THRESHOLD = 0.6;
//    private static final double CLOSE_THRESHOLD = 0.35;
//    ShihJie: Eyes open and close time(per/frame)
//    private int mEyeTime = 0;
//    ShihJie: Eyes blink mEyeState
//    private int mEyeState = 0;
//    private ArrayList<Bitmap> mBitmapList = new ArrayList<>();

    FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);
        mContext = context;
        mResources = context.getResources();
        mGraphicOverlay = overlay;
        initPaint(mContext);

        //TODO: fix in the future below(random for three different effects)
        randomSmileEffect();
        //TODO: For one device with one size resource do not need to scale.
//        mDoubleScaleOptions = new BitmapFactory.Options();
//        mDoubleScaleOptions.inScaled = false;
//        mDoubleScaleOptions.inSampleSize = 2;
//
//        mNoScaleOptions = new BitmapFactory.Options();
//        mNoScaleOptions.inScaled = false;
//        mNoScaleOptions.inSampleSize = 1;
    }

    /**
     * Random the effect array function
     */
    private void randomSmileEffect() {
        int random = (int) (Math.random() * 3);
        if (random == 0) {
            mSmileEffectFrame = getSmileEffectResourceIndex(mContext, HEART_EFFECT,
                    HEART_EFFECT_FRAME_NUMBER);
        } else if (random == 1) {
            mSmileEffectFrame = getSmileEffectResourceIndex(mContext, CURVE_EFFECT,
                    SMILE_EFFECT_FRAME_NUMBER);
        } else {
            mSmileEffectFrame = getSmileEffectResourceIndex(mContext, STAR_EFFECT,
                    STAR_EFFECT_FRAME_NUMBER);
            mLeftStarEffectFrame = getSmileEffectResourceIndex(mContext, LEFT_STAR_EFFECT,
                    STAR_EFFECT_FRAME_NUMBER);
            mRightStarEffectFrame = getSmileEffectResourceIndex(mContext, RIGHT_STAR_EFFECT,
                    STAR_EFFECT_FRAME_NUMBER);
        }
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

        // Declare each values in the face window.
        float smileScore = mSmileDegreeCounter.setIsRecording(getIsRecording()).
                setSmileDegree(face.getIsSmilingProbability()).getSimpleMovingAverage();
        float rightEye = face.getIsRightEyeOpenProbability();
        float leftEye = face.getIsLeftEyeOpenProbability();
        float faceWidth = face.getWidth();
        float faceHeight = face.getHeight();

        if (mGraphicOverlay.getMode() == GraphicOverlay.Mode.CONVERSATION) {
            return;
        }

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

        final float smileTextPadding = mResources.getDimension(R.dimen.smile_text_padding);
        // ++ShihJie: Draw face effect(one face).
        if (getAddingEffect()) {
            // Draw smile icon and smile text.
            drawSmileLevelIcon(mResources, canvas, faceWindowWidth, faceWindowHeight,
                    right, top);
            drawSmileText(mResources, canvas, mFaceTextPaint, face
                    .getIsSmilingProbability(), x, top - smileTextPadding);

            // Draw the smile effects in the singlePlayer mode.
            if (smileScore > SMILE_LEVEL_L4) {
                smileCount++;
                if (smileCount > SMILE_DURATION) {
                    mEffect = true;
                    smileCount = 0;
                }
            }
            if (mEffect) {
                if (mSmileEffectFrame.length <= STAR_EFFECT_FRAME_NUMBER) {
                    //TODO: Draw three part effect in the same time, and its place is different from other effects.
                    drawSmileEffectBitmap(context, canvas, faceWindowWidth, faceWindowHeight, x,
                            top - yOffset / 2);
                } else {
                    drawSmileEffectBitmap(context, canvas, faceWindowWidth, faceWindowHeight, x, y);
                }
                mSmileEffectFrameCount++;
                if (mSmileEffectFrameCount >= mSmileEffectFrame.length) {
                    mSmileEffectFrameCount = 0;
                    mEffect = false;
                    randomSmileEffect();
                }
            }
        } else {
            // Draw Crown in the multiPlayer mode.
            if (mCrown) {
                drawCrown(mResources, canvas, faceWindowWidth, faceWindowHeight, x, top);
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
    private void initPaint(Context context) {
        final float faceTextSize = mResources.getDimensionPixelSize(R.dimen.smile_text_size);
        final int shadowColor = ContextCompat.getColor(context, R.color.smile_text_shadow_color);
        mFaceTextPaint = new Paint();
        mFaceTextPaint.setTextSize(faceTextSize);
        mFaceTextPaint.setColor(Color.WHITE);
        mFaceTextPaint.setTextAlign(Paint.Align.CENTER);
        mFaceTextPaint.setShadowLayer(SHADOW_RADIUS, SHADOW_X_POSITION, SHADOW_Y_POSITION,
                shadowColor);

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
        final int smileLineShadowColor = ContextCompat
                .getColor(context, R.color.smile_window_line_shadow_color);
        final int smileLineShadowStroke = 1;

        if (smilingProbability >= SMILE_LEVEL_L4) {
            paint.setColor(smileLevelFourColor);
        } else if (smilingProbability < SMILE_LEVEL_L4 && smilingProbability >= SMILE_LEVEL_L3) {
            paint.setColor(smileLevelThreeColor);
        } else if (smilingProbability < SMILE_LEVEL_L3 && smilingProbability >= SMILE_LEVEL_L2) {
            paint.setColor(smileLevelTwoColor);
        } else if (smilingProbability < SMILE_LEVEL_L2) {
            paint.setColor(smileLevelOneColor);
        }
        paint.setShadowLayer(0, smileLineShadowStroke, smileLineShadowStroke, smileLineShadowColor);
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
     * @param context   Class for accessing an application's resources.
     * @param canvas    Draws the smile object in the camera preview.
     * @param width     Max effect frame number.
     * @param height    Max effect frame number.
     * @param positionX Max effect frame number.
     * @param positionY Max effect frame number.
     */
    private void drawSmileEffectBitmap(@NonNull Context context, @NonNull Canvas canvas,
            float width, float height, float positionX,
            float positionY) {
        final float scaleWidth = width / ORIGINAL_FACE_WIDTH;
        final float scaleHeight = height / ORIGINAL_FACE_HEIGHT;
        if (mSmileEffectFrame.length <= STAR_EFFECT_FRAME_NUMBER) {
            //TODO: fix in the future below(random for three different effects)
            BitmapDrawable leftEffect = (BitmapDrawable) context.getResources()
                    .getDrawable(mLeftStarEffectFrame[mSmileEffectFrameCount], null);
            BitmapDrawable rightEffect = (BitmapDrawable) context.getResources()
                    .getDrawable(mRightStarEffectFrame[mSmileEffectFrameCount], null);

            Bitmap leftOutput = decodeSampledBitmapFromResource(context.getResources(),
                    mLeftStarEffectFrame[mSmileEffectFrameCount],
                    (int) (scaleWidth * leftEffect.getIntrinsicWidth()),
                    (int) (scaleHeight * leftEffect.getIntrinsicHeight()));
            Bitmap rightOutput = decodeSampledBitmapFromResource(context.getResources(),
                    mRightStarEffectFrame[mSmileEffectFrameCount],
                    (int) (scaleWidth * rightEffect.getIntrinsicWidth()),
                    (int) (scaleHeight * rightEffect.getIntrinsicHeight()));

            canvas.drawBitmap(leftOutput, positionX - 2 * leftOutput.getWidth(),
                    positionY + (leftOutput.getHeight() / 2), null);
            canvas.drawBitmap(rightOutput, positionX + rightOutput.getWidth(),
                    positionY + (rightOutput.getHeight() / 2),
                    null);

            leftOutput.recycle();
            rightOutput.recycle();
        }
        BitmapDrawable smileEffect = (BitmapDrawable) context.getResources()
                .getDrawable(mSmileEffectFrame[mSmileEffectFrameCount], null);
        Bitmap output = decodeSampledBitmapFromResource(context.getResources(),
                mSmileEffectFrame[mSmileEffectFrameCount],
                (int) (scaleWidth * smileEffect.getIntrinsicWidth()),
                (int) (scaleHeight * smileEffect.getIntrinsicHeight()));
        canvas.drawBitmap(output, positionX - (output.getWidth() / 2), positionY, null);
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
     */
    private void drawSmileLevelIcon(@NonNull Resources resources, @NonNull Canvas canvas,
            float width, float height, float x, float y) {
        final float smileProbability = mFace.getIsSmilingProbability();
        final int smileLevelFour = R.drawable.l4;
        final int smileLevelThree = R.drawable.l3;
        final int smileLevelTwo = R.drawable.l2;
        final int smileLevelOne = R.drawable.l1;

        BitmapDrawable smileIcon;
        int smileIconId;

        if (smileProbability > SMILE_LEVEL_L4) {
            smileIcon = (BitmapDrawable) resources.getDrawable(smileLevelFour, null);
            smileIconId = smileLevelFour;
        } else if (smileProbability < SMILE_LEVEL_L4 && smileProbability > SMILE_LEVEL_L3) {
            smileIcon = (BitmapDrawable) resources.getDrawable(smileLevelThree, null);
            smileIconId = smileLevelThree;
        } else if (smileProbability < SMILE_LEVEL_L3 && smileProbability > SMILE_LEVEL_L2) {
            smileIcon = (BitmapDrawable) resources.getDrawable(smileLevelTwo, null);
            smileIconId = smileLevelTwo;
        } else {
            smileIcon = (BitmapDrawable) resources.getDrawable(smileLevelOne, null);
            smileIconId = smileLevelOne;
        }
        Bitmap output = decodeSampledBitmapFromResource(resources,
                smileIconId, (int) (smileIcon.getIntrinsicWidth() * (width / ORIGINAL_FACE_WIDTH)),
                (int) (smileIcon.getIntrinsicHeight() * (height / ORIGINAL_FACE_HEIGHT)));

        canvas.drawBitmap(output, x - output.getWidth(), y, null);
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
     * @param x         The X coordinate of the toast_text on the face window.
     * @param y         The Y coordinate of the toast_text on the face window.
     */
    private void drawCrown(@NonNull Resources resources, @NonNull Canvas canvas, float width,
            float height, float x, float y) {
        final int crownResource = R.drawable.crown;
        final float topPadding = resources.getDimension(R.dimen.crown_bottom_padding);
        BitmapDrawable crown = (BitmapDrawable) resources.getDrawable(crownResource, null);

        Bitmap output = decodeSampledBitmapFromResource(resources,
                crownResource, (int) (crown.getIntrinsicWidth() * (width / ORIGINAL_FACE_WIDTH)),
                (int) (crown.getIntrinsicHeight() * (height / ORIGINAL_FACE_HEIGHT)));
        canvas.drawBitmap(output, x - output.getWidth() / 2, y - output.getHeight() + topPadding,
                null);
        output.recycle();
    }

    /**
     * Smile effect each frame data.
     *
     * @param context Class for accessing an application's resources.
     * @param maxsize Max effect frame number.
     */
    private static int[] getSmileEffectResourceIndex(@NonNull Context context, String resource,
            int maxsize) {
        final String drawable = "drawable";
        final String resourceName = resource + "_%03d";
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
     * Calculate an inSampleSize for use in a {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link android.graphics.BitmapFactory}. This implementation calculates
     * the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap
     * having a width and height equal to or larger than the requested width and height.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Decode and sample down a bitmap from resources to the requested width and height.
     *
     * @param res       The resources object containing the image data
     * @param resId     The resource id of the image data
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     * that are equal to or greater than the requested width and height
     */
    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // TODO: options.inBitmap;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, resId, options), reqWidth,
                reqHeight, false);
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

