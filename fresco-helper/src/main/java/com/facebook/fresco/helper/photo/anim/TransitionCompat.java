package com.facebook.fresco.helper.photo.anim;

import android.animation.TimeInterpolator;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.helper.utils.DensityUtil;

import java.util.ArrayList;

/**
 * Created by android_ls on 16/9/20.
 */
public class TransitionCompat {

    private Activity mActivity;
    private ArrayList<ViewOptions> mViewOptions;
    private int mCurrentPosition;

    private static TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private static final long mAnimTime = 300;
    private AnimListener mAnimListener;

    public TransitionCompat(final Activity activity) {
        mActivity = activity;
        Bundle bundle = activity.getIntent().getExtras();
        mViewOptions = bundle.getParcelableArrayList(ViewOptionsCompat.KEY_VIEW_OPTION_LIST);
    }

    public void setCurrentPosition(int mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }

    public void startTransition() {
        if(mCurrentPosition < 0 || mCurrentPosition >= mViewOptions.size()) {
           return;
        }

        ViewOptions viewOptions = mViewOptions.get(mCurrentPosition);
        if(viewOptions.thumbnail == null
                || !viewOptions.isInTheScreen
                || !viewOptions.isVerticalScreen) {
            return;
        }
        thumbnailScaleUpAnimation(viewOptions, true);
    }

    public void finishAfterTransition() {
        // 由于用户头像是包含在查看大图相册中的，而照片墙中不包含它，所以其在照片墙中的下标为-1
        if(mCurrentPosition > -1 && mCurrentPosition < mViewOptions.size()) {
            ViewOptions viewOptions = mViewOptions.get(mCurrentPosition);
            if (viewOptions.isInTheScreen && viewOptions.isVerticalScreen) {
                // 照片墙中的View，在屏幕上是可见的并且当前的屏幕是竖屏，才会有动画效果
                thumbnailScaleUpAnimation(viewOptions, false);
            } else {
                mActivity.finish();
            }
        } else {
            mActivity.finish();
        }
    }

    private void thumbnailScaleUpAnimation(ViewOptions viewOptions, final boolean isEnter) {
        if (isEnter) {
            startThumbnailAnimation(viewOptions);
        } else {
            endThumbnailAnimation(viewOptions);
        }

        // 执行屏幕的动画
        scaleUpAnimation(viewOptions, isEnter);

    }

    private ImageView getThumbnailOriginalImageView(ViewOptions viewOptions) {
        if (viewOptions == null || viewOptions.thumbnail == null) {
            return null;
        }

        SimpleDraweeView draweeView = new SimpleDraweeView(mActivity);
        GenericDraweeHierarchy hierarchy = draweeView.getHierarchy();
        hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);
        draweeView.setHierarchy(hierarchy);
        draweeView.setImageURI(viewOptions.thumbnail);
        return draweeView;
    }

    /**
     * 开始执行thumbnail的动画效果
     */
    private void startThumbnailAnimation(ViewOptions viewOptions) {
        final ImageView bitmapImageView = getThumbnailOriginalImageView(viewOptions);
        if(bitmapImageView == null) {
            return;
        }

        bitmapImageView.setVisibility(View.INVISIBLE);

        /**
         * 设定view开始时的大小和坐标位置
         */
        ViewGroup.LayoutParams orginalParams = new ViewGroup.LayoutParams(viewOptions.width, viewOptions.height);
        ViewGroup rootView = (ViewGroup)(mActivity.getWindow().getDecorView());
        rootView.addView(bitmapImageView, orginalParams);
        bitmapImageView.setX(viewOptions.startX);
        bitmapImageView.setY(viewOptions.startY);

        float fraction = 0.8f;
        final Rect finalBounds = new Rect(0, 0,
                (int)(DensityUtil.getDisplayWidth(mActivity) * fraction),
                (int)(DensityUtil.getDisplayHeight(mActivity) * fraction));

        mAnimListener = new AnimListener(bitmapImageView, null);
        final ViewAnim anim = new ViewAnim();
        anim.setDuration((long) (mAnimTime * fraction));
        anim.setTimeInterpolator(mInterpolator);
        anim.addAnimListener(mAnimListener);
        bitmapImageView.post(new Runnable() {

            @Override
            public void run() {
                anim.startViewSimpleAnim(bitmapImageView, finalBounds, 0, 0, 1f, 0f);
            }
        });
    }

    /**
     * 设定thumbnail结束动画
     */
    private void endThumbnailAnimation(ViewOptions viewOptions) {
        final ImageView bitmapImageView = getThumbnailOriginalImageView(viewOptions);
        if(bitmapImageView == null) {
            return;
        }

        bitmapImageView.setVisibility(View.INVISIBLE);

        /**
         * 开始设定view开始时的大小和坐标位置
         */
        ViewGroup.LayoutParams orginalParams = new ViewGroup.LayoutParams(
                DensityUtil.getDisplayWidth(mActivity),
                DensityUtil.getDisplayHeight(mActivity));

        ViewGroup rootView = (ViewGroup)(mActivity.getWindow().getDecorView());
        rootView.addView(bitmapImageView, orginalParams);
        bitmapImageView.setX(0);
        bitmapImageView.setY(0);

        final Rect finalBounds = new Rect(viewOptions.startX, viewOptions.startY,
                viewOptions.startX + viewOptions.width,
                viewOptions.startY + viewOptions.height);

        mAnimListener = new AnimListener(bitmapImageView, null);
        final ViewAnim anim = new ViewAnim();
        anim.setDuration(mAnimTime);
        anim.setTimeInterpolator(mInterpolator);
        anim.addAnimListener(mAnimListener);
        bitmapImageView.post(new Runnable() {

            @Override
            public void run() {
                anim.startViewSimpleAnim(bitmapImageView, finalBounds, 0, 0, 0f, 1f);
            }
        });
    }

    private void scaleUpAnimation(ViewOptions viewOptions, final boolean isEnter) {
        if (!viewOptions.isInTheScreen) {
            return;
        }

        if (viewOptions.isVerticalScreen != ViewOptionsCompat.isVerticalScreen(mActivity)) {
            return;
        }

        final SceneScaleUpAnimator anim = new SceneScaleUpAnimator(mActivity, viewOptions);
        anim.setAnimInterpolator(mInterpolator);
        anim.setAnimDuration(mAnimTime);
        mActivity.getWindow().getDecorView().post(new Runnable() {

            @Override
            public void run() {
                anim.playScreenAnim(isEnter);
            }
        });
    }

}
