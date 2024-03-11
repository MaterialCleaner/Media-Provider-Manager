/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright 2023 Green Mushroom
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

package me.gm.cleaner.plugin.ui.mediastore.imagepager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.Map;

/**
 * This Transition captures an ImageView's matrix before and after the
 * scene change and animates it during the transition.
 *
 * <p>In combination with ChangeBounds, ChangeImageTransform allows ImageViews
 * that change size, shape, or {@link android.widget.ImageView.ScaleType} to animate contents
 * smoothly.</p>
 */
public class CustomChangeImageTransform extends Transition implements PhotoViewTransition {

    private static final String PROPNAME_MATRIX = "android:changeImageTransform:matrix";
    private static final String PROPNAME_BOUNDS = "android:changeImageTransform:bounds";

    private static final String[] sTransitionProperties = {
            PROPNAME_MATRIX,
            PROPNAME_BOUNDS,
    };

    private static final TypeEvaluator<Matrix> NULL_MATRIX_EVALUATOR = new TypeEvaluator<Matrix>() {
        @Override
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            return null;
        }
    };

    private static final Property<ImageView, Matrix> ANIMATED_TRANSFORM_PROPERTY =
            new Property<ImageView, Matrix>(Matrix.class, "animatedTransform") {
                @Override
                public void set(ImageView view, Matrix matrix) {
                    view.animateTransform(matrix);
                }

                @Override
                public Matrix get(ImageView object) {
                    return null;
                }
            };

    public CustomChangeImageTransform() {
    }

    public CustomChangeImageTransform(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        if (!(view instanceof ImageView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        ImageView imageView = (ImageView) view;
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return;
        }
        Map<String, Object> values = transitionValues.values;

        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();

        Rect bounds = new Rect(left, top, right, bottom);
        values.put(PROPNAME_BOUNDS, bounds);
        if (PhotoViewTransition.isPhotoView(imageView)) {
            values.put(PROPNAME_MATRIX, copyImageMatrix(imageView));
        }
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    @NonNull
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private static Matrix calcCenterCropMatrix(@NonNull Rect bounds, @NonNull Drawable image) {
        final int imageWidth = image.getIntrinsicWidth();
        final int imageViewWidth = bounds.width();
        final float scaleX = ((float) imageViewWidth) / imageWidth;

        final int imageHeight = image.getIntrinsicHeight();
        final int imageViewHeight = bounds.height();
        final float scaleY = ((float) imageViewHeight) / imageHeight;

        final float maxScale = Math.max(scaleX, scaleY);

        final float width = imageWidth * maxScale;
        final float height = imageHeight * maxScale;
        final int tx = Math.round((imageViewWidth - width) / 2f);
        final int ty = Math.round((imageViewHeight - height) / 2f);

        final Matrix matrix = new Matrix();
        matrix.postScale(maxScale, maxScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    /**
     * Creates an Animator for ImageViews moving, changing dimensions, and/or changing
     * {@link android.widget.ImageView.ScaleType}.
     *
     * @param sceneRoot   The root of the transition hierarchy.
     * @param startValues The values for a specific target in the start scene.
     * @param endValues   The values for the target in the end scene.
     * @return An Animator to move an ImageView or null if the View is not an ImageView,
     * the Drawable changed, the View is not VISIBLE, or there was no change.
     */
    @Nullable
    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot,
                                   @Nullable TransitionValues startValues,
                                   final @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Rect startBounds = (Rect) startValues.values.get(PROPNAME_BOUNDS);
        Rect endBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
        if (startBounds == null || endBounds == null) {
            return null;
        }

        final ImageView endView = (ImageView) endValues.view;
        final boolean isAnimeViewPhotoView = PhotoViewTransition.isPhotoView(endView);
        final TransitionValues photoViewValues;
        if (isAnimeViewPhotoView) {
            photoViewValues = endValues;
        } else {
            photoViewValues = startValues;
        }
        final PhotoView photoView = (PhotoView) photoViewValues.view;

        Matrix matrix = (Matrix) photoViewValues.values.get(PROPNAME_MATRIX);
        Matrix centerCropMatrix;
        if (isAnimeViewPhotoView) {
            centerCropMatrix = calcCenterCropMatrix(startBounds, photoView.getDrawable());
        } else {
            centerCropMatrix = calcCenterCropMatrix(endBounds, photoView.getDrawable());
        }

        boolean matricesEqual = matrix != null && matrix.equals(centerCropMatrix);

        if (startBounds.equals(endBounds) && matricesEqual) {
            return null;
        }

        Drawable drawable = photoView.getDrawable();
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        ObjectAnimator animator;
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            animator = createNullAnimator(endView);
        } else {
            if (matrix == null) {
                matrix = IDENTITY_MATRIX;
            }
            if (centerCropMatrix == null) {
                centerCropMatrix = IDENTITY_MATRIX;
            }
            if (isAnimeViewPhotoView) {
                ANIMATED_TRANSFORM_PROPERTY.set(endView, centerCropMatrix);
                animator = createMatrixAnimator(endView, centerCropMatrix, matrix);
            } else {
                ANIMATED_TRANSFORM_PROPERTY.set(endView, matrix);
                animator = createMatrixAnimator(endView, matrix, centerCropMatrix);
            }
            if (!isAnimeViewPhotoView) {
                final MatrixAnimatorListener listener = new MatrixAnimatorListener(endView);
                animator.addListener(listener);
                addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        listener.onAnimationEnd(animator);
                        transition.removeListener(this);
                    }
                });

                endView.setImageDrawable(photoView.getDrawable());
            }
        }

        return animator;
    }

    @NonNull
    private ObjectAnimator createNullAnimator(@NonNull ImageView imageView) {
        return ObjectAnimator.ofObject(imageView, ANIMATED_TRANSFORM_PROPERTY,
                NULL_MATRIX_EVALUATOR, IDENTITY_MATRIX, IDENTITY_MATRIX);
    }

    private ObjectAnimator createMatrixAnimator(final ImageView imageView, Matrix startMatrix,
                                                final Matrix endMatrix) {
        return ObjectAnimator.ofObject(imageView, ANIMATED_TRANSFORM_PROPERTY,
                new TransitionUtils_MatrixEvaluator(), startMatrix, endMatrix);
    }

    @NonNull
    private static Matrix copyImageMatrix(@NonNull ImageView view) {
        final Drawable image = view.getDrawable();
        if (image.getIntrinsicWidth() > 0 && image.getIntrinsicHeight() > 0) {
            switch (view.getScaleType()) {
                case FIT_XY:
                    return fitXYMatrix(view);
                case CENTER_CROP:
                    return centerCropMatrix(view);
                default:
                    return new Matrix(view.getImageMatrix());
            }
        }
        return new Matrix(view.getImageMatrix());
    }

    /**
     * Calculates the image transformation matrix for an ImageView with ScaleType FIT_XY. This
     * needs to be manually calculated as the platform does not give us the value for this case.
     */
    private static Matrix fitXYMatrix(ImageView view) {
        final Drawable image = view.getDrawable();
        final Matrix matrix = new Matrix();
        matrix.postScale(
                ((float) view.getWidth()) / image.getIntrinsicWidth(),
                ((float) view.getHeight()) / image.getIntrinsicHeight());
        return matrix;
    }

    /**
     * Calculates the image transformation matrix for an ImageView with ScaleType CENTER_CROP. This
     * needs to be manually calculated for consistent behavior across all the API levels.
     */
    private static Matrix centerCropMatrix(ImageView view) {
        final Drawable image = view.getDrawable();
        final int imageWidth = image.getIntrinsicWidth();
        final int imageViewWidth = view.getWidth();
        final float scaleX = ((float) imageViewWidth) / imageWidth;

        final int imageHeight = image.getIntrinsicHeight();
        final int imageViewHeight = view.getHeight();
        final float scaleY = ((float) imageViewHeight) / imageHeight;

        final float maxScale = Math.max(scaleX, scaleY);

        final float width = imageWidth * maxScale;
        final float height = imageHeight * maxScale;
        final int tx = Math.round((imageViewWidth - width) / 2f);
        final int ty = Math.round((imageViewHeight - height) / 2f);

        final Matrix matrix = new Matrix();
        matrix.postScale(maxScale, maxScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    static final Matrix IDENTITY_MATRIX = new Matrix() {

        void oops() {
            throw new IllegalStateException("Matrix can not be modified");
        }

        @Override
        public void set(Matrix src) {
            oops();
        }

        @Override
        public void reset() {
            oops();
        }

        @Override
        public void setTranslate(float dx, float dy) {
            oops();
        }

        @Override
        public void setScale(float sx, float sy, float px, float py) {
            oops();
        }

        @Override
        public void setScale(float sx, float sy) {
            oops();
        }

        @Override
        public void setRotate(float degrees, float px, float py) {
            oops();
        }

        @Override
        public void setRotate(float degrees) {
            oops();
        }

        @Override
        public void setSinCos(float sinValue, float cosValue, float px, float py) {
            oops();
        }

        @Override
        public void setSinCos(float sinValue, float cosValue) {
            oops();
        }

        @Override
        public void setSkew(float kx, float ky, float px, float py) {
            oops();
        }

        @Override
        public void setSkew(float kx, float ky) {
            oops();
        }

        @Override
        public boolean setConcat(Matrix a, Matrix b) {
            oops();
            return false;
        }

        @Override
        public boolean preTranslate(float dx, float dy) {
            oops();
            return false;
        }

        @Override
        public boolean preScale(float sx, float sy, float px, float py) {
            oops();
            return false;
        }

        @Override
        public boolean preScale(float sx, float sy) {
            oops();
            return false;
        }

        @Override
        public boolean preRotate(float degrees, float px, float py) {
            oops();
            return false;
        }

        @Override
        public boolean preRotate(float degrees) {
            oops();
            return false;
        }

        @Override
        public boolean preSkew(float kx, float ky, float px, float py) {
            oops();
            return false;
        }

        @Override
        public boolean preSkew(float kx, float ky) {
            oops();
            return false;
        }

        @Override
        public boolean preConcat(Matrix other) {
            oops();
            return false;
        }

        @Override
        public boolean postTranslate(float dx, float dy) {
            oops();
            return false;
        }

        @Override
        public boolean postScale(float sx, float sy, float px, float py) {
            oops();
            return false;
        }

        @Override
        public boolean postScale(float sx, float sy) {
            oops();
            return false;
        }

        @Override
        public boolean postRotate(float degrees, float px, float py) {
            oops();
            return false;
        }

        @Override
        public boolean postRotate(float degrees) {
            oops();
            return false;
        }

        @Override
        public boolean postSkew(float kx, float ky, float px, float py) {
            oops();
            return false;
        }

        @Override
        public boolean postSkew(float kx, float ky) {
            oops();
            return false;
        }

        @Override
        public boolean postConcat(Matrix other) {
            oops();
            return false;
        }

        @Override
        public boolean setRectToRect(RectF src, RectF dst, ScaleToFit stf) {
            oops();
            return false;
        }

        @Override
        public boolean setPolyToPoly(float[] src, int srcIndex, float[] dst, int dstIndex,
                                     int pointCount) {
            oops();
            return false;
        }

        @Override
        public void setValues(float[] values) {
            oops();
        }

    };

    static class TransitionUtils_MatrixEvaluator implements TypeEvaluator<Matrix> {

        final float[] mTempStartValues = new float[9];

        final float[] mTempEndValues = new float[9];

        final Matrix mTempMatrix = new Matrix();

        @Override
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            startValue.getValues(mTempStartValues);
            endValue.getValues(mTempEndValues);
            for (int i = 0; i < 9; i++) {
                float diff = mTempEndValues[i] - mTempStartValues[i];
                mTempEndValues[i] = mTempStartValues[i] + (fraction * diff);
            }
            mTempMatrix.setValues(mTempEndValues);
            return mTempMatrix;
        }

    }

    private static class MatrixAnimatorListener extends AnimatorListenerAdapter {
        private final ImageView mView;
        private final Drawable mBackup;
        private boolean mLayerTypeChanged = false;

        public MatrixAnimatorListener(ImageView view) {
            mView = view;
            mBackup = view.getDrawable();
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (mView.hasOverlappingRendering() && mView.getLayerType() == View.LAYER_TYPE_NONE) {
                mLayerTypeChanged = true;
                mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mView.setImageDrawable(mBackup);
            if (mLayerTypeChanged) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }
}
