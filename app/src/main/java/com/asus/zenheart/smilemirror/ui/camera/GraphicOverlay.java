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

package com.asus.zenheart.smilemirror.ui.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.asus.zenheart.smilemirror.FaceGraphic;
import com.asus.zenheart.smilemirror.SmileDegreeCounter;
import com.google.android.gms.vision.face.Face;

import java.util.HashSet;
import java.util.Set;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.<p>
 * <p>
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.<p>
 * <p>
 * Associated {@link Graphic} items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 * <ol>
 * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 * supplied value from the preview scale to the view scale.</li>
 * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.</li>
 * </ol>
 */
public class GraphicOverlay extends View {
    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<Graphic> mGraphics = new HashSet<>();

    // Jack +++
    private boolean mIsRecording = false;
    private Mode mMode = Mode.SMILE;

    public enum Mode {SMILE, CONVERSATION}

    private boolean mAddingEffect = false;

    private static final float MIN_FACE_WINDOW_RATIO = 0.15f;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay,usually called by processingThread
     */
    public void add(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Removes a graphic from the overlay,usually called by processingThread
     */
    public void remove(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }
    // Jack +++

    /**
     * Set the state to tell if recording now
     */
    public void setRecordingState(boolean isRecording) {
        mIsRecording = isRecording;
    }

    /**
     * Get the state to tell if recording now
     */
    public boolean getRecordingState() {
        return mIsRecording;
    }

    public Graphic getGraphic() {

        for (Graphic graphic : mGraphics) {
            //return the first graphic because there is only one person in couch mode
            return graphic;
        }
        return null;
    }

    public void setMode(Mode mode) {
        mMode = mode;
    }

    public Mode getMode() {
        return mMode;
    }
    // Jack ---

    // ++ShihJie
    public boolean getAddingEffectState() {
        return mAddingEffect;
    }

    public void setAddingEffectState(boolean isAddingEffect) {
        mAddingEffect = isAddingEffect;
    }
    // --ShihJie

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            //TODO: Wating for multi-player mode UIRS lock down
            FaceGraphic Array[] = mGraphics.toArray(new FaceGraphic[mGraphics.size()]);

            //TODO: Waiting for multi-player mode UIRS lock down


            for (FaceGraphic aArray : Array) {
                Face face = aArray.getFaceData();
                if (face.getHeight() < (mPreviewHeight * MIN_FACE_WINDOW_RATIO) ||
                        face.getWidth() < (mPreviewWidth * MIN_FACE_WINDOW_RATIO)) {
                    mGraphics.remove(aArray);
                }
            }
            if (mGraphics.size() > 1) {
                // ShihJie: just only draw the crown on the face1 or face2
                float face1SmileProbability = Array[0].getFaceData().getIsSmilingProbability();
                float face2SmileProbability = Array[1].getFaceData().getIsSmilingProbability();
                if (face1SmileProbability > face2SmileProbability) {
                    Array[0].drawTheCrown();
                } else if (face1SmileProbability < face2SmileProbability) {
                    Array[1].drawTheCrown();
                }
            }
            for (Graphic graphic : mGraphics) {
                // ShihJie: more than one face
                setAddingEffectState(mGraphics.size() <= 1);
                graphic.draw(canvas, getContext());
            }
        }
    }

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the
     * graphics element.  Add instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public abstract static class Graphic {
        private GraphicOverlay mOverlay;
        protected SmileDegreeCounter mSmileDegreeCounter;

        private static final int MOVING_WINDOW_SIZE = 10;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
            mSmileDegreeCounter = new SmileDegreeCounter.Builder()
                    .setMovingWindowSize(MOVING_WINDOW_SIZE)
                    .create();
        }

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
         * the supplied value from the preview scale to the view scale.</li>
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.</li>
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas, Context context);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }

        public boolean getIsRecording() {
            return mOverlay.getRecordingState();
        }

        public SmileDegreeCounter getSmileDegreeCounter() {
            return mSmileDegreeCounter;
        }

        /**
         * Adjusts the smile effect from the preview's face detector to the face window
         */
        public boolean getAddingEffect() {
            return mOverlay.getAddingEffectState();
        }
    }
}
