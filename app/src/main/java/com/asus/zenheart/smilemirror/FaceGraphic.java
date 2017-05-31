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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.Surface;

import com.asus.zenheart.smilemirror.Util.BitmapUtil;

import com.asus.zenheart.smilemirror.VideoTexture.SmileVideoTextureView;
import com.asus.zenheart.smilemirror.VideoTexture.VideoTextureView;
import com.asus.zenheart.smilemirror.ui.camera.CameraSource;
import com.asus.zenheart.smilemirror.ui.camera.CameraSourcePreview;
import com.asus.zenheart.smilemirror.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float BOX_STROKE_WIDTH = 2.0f;
    private static final int BOX_STROKE_ALPHA = 102;// 60% opacity

    private static final int VIDEO_EFFECT_INTERVAL_WIDTH = 363;
    private static final int VIDEO_EFFECT_INTERVAL_HEIGHT = 380;

    // ShihJie: add the value for L1 to L4
    //TODO: it should be combined or extracted with those in SmileDegreeCounter
    private static final float SMILE_LEVEL_L4 = 0.7f;
    private static final float SMILE_LEVEL_L3 = 0.5f;
    private static final float SMILE_LEVEL_L2 = 0.3f;

    // ShihJie: default face window size
    private static final float ORIGINAL_FACE_WIDTH = 680.0f;
    private static final float ORIGINAL_FACE_HEIGHT = 850.0f;

    private static final float VIDEO_FACE_WIDTH = 353.0f;
    private static final float VIDEO_FACE_HEIGHT = 441.0f;

    // ShihJie: face window scale size
    private static final float FACE_SCALE_SIZE = 0.8f;

    // ShihJie: smile duration
    private static final int SMILE_DURATION = 30;

    // ShihJie: paint shadow
    private static final float SHADOW_RADIUS = 2.5f;
    private static final float SHADOW_X_POSITION = 5.0f;
    private static final float SHADOW_Y_POSITION = 5.0f;

    private Context mContext;
    private SmileVideoTextureView mVideoView;
    private Resources mResources;
    private GraphicOverlay mGraphicOverlay;
    private CameraSourcePreview mCameraSourcePreview;
    private Paint mBoxPaint;
    private Paint mFaceTextPaint;
    private volatile Face mFace;
    private int mFaceId;

    // ShihJie: draw the crown or not
    private int smileCount = 0;
    private boolean mCrown = false;
    private boolean mEffect = false;
    private Matrix mMatrix;

    FaceGraphic(GraphicOverlay overlay, Context context, CameraSourcePreview cameraPreview,
            SmileVideoTextureView videoTextureView) {
        super(overlay);
        mContext = context;
        mResources = context.getResources();
        mGraphicOverlay = overlay;
        mCameraSourcePreview = cameraPreview;
        initPaint(mContext);
        mMatrix= new Matrix();
        mVideoView = videoTextureView;
        mVideoView.setCompletionListener(new VideoTextureView.VideoCompletionCallback() {
            @Override
            public void onCompletion() {
                mVideoView.setResourceId(mVideoView.getRandomEffect(), mVideoView.getCorrectShader());
                mVideoView.stopMediaPlayerAndRenderer();
            }
        });
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
     * Updates the texture view instance from the detection of the face.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateTextureView(SmileVideoTextureView effectTextureView) {
        mVideoView = effectTextureView;
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
        canvas.save();
        rotateCanvas(canvas, mCameraSourcePreview.getScreenRotation());
        // Declare each values in the face window.
        float smileScore = mSmileDegreeCounter.setIsRecording(getIsRecording()).
                setSmileDegree(face.getIsSmilingProbability()).getSimpleMovingAverage();
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

        // Resize the video texture view.
        resizeVideoTextureView(mVideoView, left, top, faceWindowWidth / ORIGINAL_FACE_WIDTH,
                mCameraSourcePreview.getScreenRotation(), mCameraSourcePreview.getHeight());
        final float smileTextMargin = mResources.getDimension(R.dimen.sm_smile_mode_smile_text_margin);

        // Draw the face effect.
        if (getAddingEffect()) {
            // Draw smile icon and smile text.
            drawSmileLevelIcon(mResources, canvas, faceWindowWidth, faceWindowHeight,
                    right, top);
            drawSmileText(mResources, canvas, mFaceTextPaint, face
                    .getIsSmilingProbability(), x, top - smileTextMargin);

            // play the smile effects in the singlePlayer mode.
            if (smileScore > SMILE_LEVEL_L4) {
                smileCount++;
                if (smileCount > SMILE_DURATION) {
                    mEffect = true;
                    smileCount = 0;
                } else {
                    mEffect = false;
                }
                if (mEffect && !mVideoView.isPlayingMediaPlayer()) {
                    mVideoView.post(new Runnable() {
                        @Override
                        public void run() {
                            mVideoView.playMediaPlayer();
                        }
                    });
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
        canvas.restore();
    }

    /**
     * scale and translate the texture view size by the value.
     *
     * @param view     The video texture view which you want to resize.
     * @param x        The x-line position in the view.
     * @param y        The y-line position in the view.
     * @param radio    The original video scale radio.
     * @param rotation The orientation of the device screen
     * @param height   The height of the view, used to error check the position of the effect.
     */
    private void resizeVideoTextureView(VideoTextureView view, float x, float y, float radio,
            int rotation, float height) {
        // TODO: In future, maybe need to rotate the video.
        float degree;
        float correctionUnit = height * 3 / 4;
        radio = radio * ORIGINAL_FACE_HEIGHT / VIDEO_FACE_HEIGHT;
        mMatrix.setScale(radio, radio);
        x = x - (VIDEO_EFFECT_INTERVAL_WIDTH * radio);
        y = y - (VIDEO_EFFECT_INTERVAL_HEIGHT * radio);

        if (rotation == 0) {
            degree = 0;
            mMatrix.postRotate(degree);
            mMatrix.postTranslate(x, y);
        } else if (rotation == 1) {
            degree = 90;
            x = x + (correctionUnit / 3);
            y = y - correctionUnit;
            mMatrix.postTranslate(x, y);
            mMatrix.postRotate(degree);
        } else if (rotation == 2) {
            degree = 180;
            x = x - correctionUnit;
            y = y - height;
            mMatrix.postTranslate(x, y);
            mMatrix.postRotate(degree);
        } else if (rotation == 3) {
            degree = 270;
            x = x - correctionUnit;
            mMatrix.postTranslate(x, y);
            mMatrix.postRotate(degree);
        }
        view.setTransform(mMatrix);
    }

    /**
     * {@link #mFaceTextPaint} is used to draw text above the face box
     * {@link #mBoxPaint}  is used to draw face box
     */
    private void initPaint(Context context) {
        final float faceTextSize = mResources.getDimensionPixelSize(R.dimen.sm_smile_mode_text_size);
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
        Bitmap output = BitmapUtil.decodeSampledBitmapFromResource(resources,
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
        final float topPadding = resources.getDimension(R.dimen.sm_smile_mode_crown_bottom_padding);
        BitmapDrawable crown = (BitmapDrawable) resources.getDrawable(crownResource, null);

        Bitmap output = BitmapUtil.decodeSampledBitmapFromResource(resources,
                crownResource, (int) (crown.getIntrinsicWidth() * (width / ORIGINAL_FACE_WIDTH)),
                (int) (crown.getIntrinsicHeight() * (height / ORIGINAL_FACE_HEIGHT)));
        canvas.drawBitmap(output, x - output.getWidth() / 2, y - output.getHeight() + topPadding,
                null);
        output.recycle();
    }

    /**
     * Rotate and translate the canvas to correspond to the screen rotation
     *
     * @param canvas   On which the face is drawn
     * @param rotation The orientation of the device screen
     */
    private void rotateCanvas(@NonNull Canvas canvas,
            @CameraSource.ORIENTATION int rotation) {
        final float rotationAngle = (float) rotation * 90;
        final float halfWidth = canvas.getWidth() / 2;
        final float halfHeight = canvas.getHeight() / 2;
        final float shift = Math.abs(halfHeight - halfWidth);
        canvas.rotate(rotationAngle, halfWidth, halfHeight);
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            canvas.translate(shift, shift);
        }

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

