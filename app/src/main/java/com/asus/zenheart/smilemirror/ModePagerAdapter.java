package com.asus.zenheart.smilemirror;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.GUIView.CounterView;
import com.asus.zenheart.smilemirror.GUIView.DownCountView;
import com.asus.zenheart.smilemirror.GUIView.VerticalScrollTextView;
import com.asus.zenheart.smilemirror.Util.AnimationUtil;
import com.asus.zenheart.smilemirror.Util.GalleryUtil;
import com.asus.zenheart.smilemirror.Util.PrefsUtils;
import com.asus.zenheart.smilemirror.editor.SpeechEditorActivity;

import java.util.ArrayList;

public class ModePagerAdapter extends PagerAdapter {

    private static final String LOG_TAG = "ModePagerAdapter";

    private Context mContext;
    private ArrayList<View> mViewList;
    private LayoutInflater mLayoutInflater;
    private ViewGroup mContainer;
    private int[] mIndexOfLayout;

    private ActivityCallback mCallback;

    private ViewHolder mViewHolder;
    private TextView mTitleToast;

    public ModePagerAdapter(Context context, int[] index, ViewGroup container) {
        mContext = context;
        mContainer = container;
        mLayoutInflater = LayoutInflater.from(mContext);
        mIndexOfLayout = index;
        mViewList = new ArrayList<>(index.length);
        if (context instanceof ActivityCallback) {
            mCallback = (ActivityCallback) context;
        }

    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = initView(container, position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewList.get(position));
    }

    @Override
    public int getCount() {
        return mIndexOfLayout.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private View initView(ViewGroup container, int position) {
        View view = null;
        if (mIndexOfLayout[position] == R.layout.smile_mode) {
            view = mLayoutInflater.inflate(R.layout.smile_mode, container, false);
            final ImageView imageViewBack = (ImageView) view.findViewById(R.id.back_image_view);
            mTitleToast = (TextView) view.findViewById(R.id.title_toast);
            showTitleToast();
            imageViewBack.getDrawable().setAutoMirrored(true);
            imageViewBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback != null) {
                        mCallback.finishActivity();
                    }
                }
            });
        } else if (mIndexOfLayout[position] == R.layout.coach_mode) {
            view = mLayoutInflater.inflate(R.layout.coach_mode, container, false);

            mViewHolder = new ViewHolder(view, mContext, mContainer);
            refreshViewContent();
            view.setTag(mViewHolder);
        } else {
            Log.e(LOG_TAG, "UNSPECIFIED mode is not expected !!!");
        }

        if (view != null) {
            mViewList.add(view);
        }
        return view;
    }

    public void showTitleToast() {
        AnimationUtil.showToast(mTitleToast, R.string.sm_app_name, 0);
        mTitleToast.setVisibility(View.INVISIBLE);
    }

    public void refreshViewContent() {
        if (mViewHolder == null) {
            return;
        }
        mViewHolder.refreshViewContent();
    }

    public void resetGuiElementState() {
        if (mViewHolder == null) {
            return;
        }
        mViewHolder.resetGuiElement();
        mViewHolder.downCountView.stopCount();
        mViewHolder.countPageView.setVisibility(View.GONE);
        mViewHolder.isRecording = false;
    }


    public void hideGuiElementInCoach() {
        if (mViewHolder == null) {
            return;
        }
        mViewHolder.hideGuiElementInCoach();
    }

    public void showGuiElement() {
        if (mViewHolder == null) {
            return;
        }
        mViewHolder.showGuiElement();
    }

    public interface ActivityCallback {

        void hideChartPage();

        void showChartPage(String timeText);

        void startRecord();

        void stopRecord();

        void finishActivity();

        void showTitleToast();
    }

    //TODO: rename in the future
    public static class ViewHolder {
        public final ImageView imageViewRecord;
        public final Button imageViewPlay;
        public final ImageView dragView;
        public final CounterView countView;
        public final ImageView imageViewList;
        public final ImageView imageViewSetting;
        public final ImageView imageViewBack;
        public final VerticalScrollTextView scrollTextView;
        public final View countPageView;
        public final View controllerView;
        public final DownCountView downCountView;
        public final PseudoToolBar pseudoToolBar;
        public boolean isRecording;

        private Context mContext;
        private ActivityCallback mCallback;
        private ViewGroup mContainer;

        public ViewHolder(@NonNull View view, @NonNull Context context, ViewGroup container) {
            mContext = context;
            mContainer = container;
            imageViewRecord = (ImageView) view.findViewById(R.id.video_image_view);
            imageViewPlay = (Button) view.findViewById(R.id.image_play);
            dragView = (ImageView) view.findViewById(R.id.image_drag);
            countView = (CounterView) view.findViewById(R.id.count_view);
            imageViewList = (ImageView) view.findViewById(R.id.image_list);
            imageViewSetting = (ImageView) view.findViewById(R.id.image_setting);
            imageViewBack = (ImageView) view.findViewById(R.id.image_back);
            scrollTextView = (VerticalScrollTextView) view
                    .findViewById(R.id.vertical_scroll_textview);
            countPageView = inflateCountPageView();
            downCountView = (DownCountView) countPageView.findViewById(R.id.down_count_view);
            pseudoToolBar = (PseudoToolBar) view.findViewById(R.id.pseudo_toolbar);
            controllerView = view.findViewById(R.id.controller_bar);
            if (context instanceof ActivityCallback) {
                mCallback = (ActivityCallback) context;
            }

            initViewListener();
        }

        private void initViewListener() {
            initImageViewPlay();
            initImageViewList();
            initImageViewRecord();
            initImageSetting();
            initImageViewBack();
            initDragView();
            initCountPageView();
            initControllerView();
        }

        private void initImageSetting() {
            imageViewSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context wrapper = new ContextThemeWrapper(mContext, R.style.PopupMenuTheme);
                    final PopupMenu popupmenu = new PopupMenu(wrapper, v);
                    popupmenu.inflate(R.menu.coach_mode_title_bar_menu);
                    final MenuItem item = popupmenu.getMenu()
                            .findItem(R.id.coach_mode_auto_record_checkbox);
                    item.setChecked(PrefsUtils
                            .getBooleanPreference(mContext, PrefsUtils.PREFS_AUTO_RECORDING, true));
                    popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int itemId = item.getItemId();
                            if (itemId == R.id.coach_mode_auto_record_checkbox) {
                                if (item.isChecked()) {
                                    showAlertDialog(item);
                                } else {
                                    PrefsUtils.setBooleanPreference(mContext,
                                            PrefsUtils.PREFS_AUTO_RECORDING, true);
                                    item.setChecked(true);
                                }
                            }
                            return true;
                        }

                    });
                    popupmenu.show();

                }
            });

        }

        private void initImageViewPlay() {
            if (imageViewPlay == null) {
                return;
            }
            imageViewPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isRecording) {
                        resetGuiElement();
                        if (mCallback != null) {
                            mCallback.stopRecord();
                            mCallback.showChartPage(countView.getTimeText());
                        }
                        isRecording = false;
                    } else {
                        countPageView.setVisibility(View.VISIBLE);
                        downCountView.startCount();
                    }
                }
            });

        }

        private void initImageViewList() {
            if (imageViewList == null) {
                return;
            }
            imageViewList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, SpeechEditorActivity.class);
                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mContext.startActivity(intent);
                }
            });
        }

        private void initImageViewRecord() {
            if (imageViewRecord != null) {
                imageViewRecord.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GalleryUtil.intentToGallery(mContext);
                    }
                });
            }
        }

        private void initImageViewBack() {
            if (imageViewBack != null) {
                imageViewBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.finishActivity();
                    }
                });
            }
        }

        private void initDragView() {
            if (dragView == null) {
                return;
            }
            dragView.setOnTouchListener(new View.OnTouchListener() {
                int downY;
                int moveY;
                int scrollY;
                int layoutHeight;
                int heightMax;
                View parentView;
                RelativeLayout.LayoutParams param;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downY = (int) event.getRawY();
                            param = (RelativeLayout.LayoutParams) scrollTextView.getLayoutParams();
                            parentView = (View) v.getParent().getParent();
                            heightMax = parentView.getHeight() - controllerView.getHeight() -
                                    pseudoToolBar.getHeight();
                            //save the origin layout height when when user touches down
                            layoutHeight = param.height;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            moveY = (int) event.getRawY();
                            scrollY = moveY - downY;
                            int height = layoutHeight + scrollY;
                            if (height < 0) {
                                height = 0;
                            } else if (height > heightMax) {
                                height = heightMax;
                            }
                            param.height = height;
                            scrollTextView.setLayoutParams(param);
                            return true;
                        case MotionEvent.ACTION_UP:
                            return true;
                    }
                    return true;
                }
            });
        }

        private void initCountPageView() {
            downCountView.setOnFinishedListener(
                    new DownCountView.OnFinishedLister() {
                        @Override
                        public void onFinished() {
                            imageViewRecord.setVisibility(View.INVISIBLE);
                            scrollTextView.setRepeatMode(false)
                                    .start(VerticalScrollTextView.FIRST_DELAY_TIME_MILLS);
                            dragView.setImageResource(R.drawable.drag_disable);
                            imageViewPlay.setText(R.string.teleprompter_button_text_stop);
                            dragView.setEnabled(false);
                            countView.starCount();
                            countView.setVisibility(View.VISIBLE);
                            countPageView.setVisibility(View.GONE);
                            if (mCallback != null) {
                                mCallback.startRecord();
                            }
                            isRecording = true;

                        }
                    });
            countPageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    //TODO: implement if need to cancel countdownView
                    //do nothing to absorb user's touch event to prevent unexpected
                    //touch during the counting time
                    return true;
                }
            });
        }

        private void initControllerView() {
            controllerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //do nothing to absorb onclick event
                }
            });
        }

        private View inflateCountPageView() {
            final View countPageView = LayoutInflater.from(mContext)
                    .inflate(R.layout.down_count_page, mContainer, false);
            countPageView.setVisibility(View.GONE);
            mContainer.addView(countPageView);
            return countPageView;
        }

        private void showAlertDialog(final MenuItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.sm_alert_dialog_auto_recording_title);
            builder.setMessage(R.string.sm_alert_dialog_auto_recording_message);
            builder.setPositiveButton(R.string.editor_check_yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PrefsUtils.setBooleanPreference(mContext,
                                    PrefsUtils.PREFS_AUTO_RECORDING, false);
                            item.setChecked(false);
                        }
                    });
            builder.setNegativeButton(R.string.editor_check_cancel, null);
            builder.create().show();

        }

        /**
         * To reset the gui element to the initial state
         */
        public void resetGuiElement() {
            imageViewRecord.setVisibility(View.VISIBLE);
            imageViewPlay.setText(R.string.teleprompter_button_text_start);
            dragView.setEnabled(true);
            countView.setVisibility(View.INVISIBLE);
            countView.stopCount();
            scrollTextView.stop();
        }

        public void refreshViewContent() {
            scrollTextView.loadContentToView(
                    PrefsUtils.getLongPreference(mContext, PrefsUtils.PREFS_SPEECH_ID, 1));
            scrollTextView.updateTextStyle();
        }

        public void hideGuiElementInCoach() {
            pseudoToolBar.setInterceptTouchEvent(true);
        }

        public void showGuiElement() {
            pseudoToolBar.setInterceptTouchEvent(false);
        }


    }

}

