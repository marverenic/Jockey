package com.marverenic.music.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.marverenic.music.R;

import java.lang.reflect.Field;

import timber.log.Timber;

public class NumberPickerDialogFragment extends DialogFragment {

    private static final String KEY_TITlE = "NumberPickerDialogFragment.TITLE";
    private static final String KEY_MESSAGE = "NumberPickerDialogFragment.MESSAGE";
    private static final String KEY_MIN_VAL = "NumberPickerDialogFragment.MIN_VALUE";
    private static final String KEY_MAX_VAL = "NumberPickerDialogFragment.MAX_VALUE";
    private static final String KEY_DEFAULT_VAL = "NumberPickerDialogFragment.DEFAULT_VALUE";
    private static final String KEY_SAVED_VAL = "NumberPickerDialogFragment.SAVED_VALUE";
    private static final String KEY_WRAP_SELECTOR = "NumberPickerDialogFragment.WRAP_SELECTOR";
    private static final String KEY_RESULT_FRAGMENT = "NumberPickerDialogFragment.RESULT_FRAGMENT";

    private NumberPicker mNumberPicker;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View contentView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_number_picker, null);

        TextView messageText = (TextView) contentView.findViewById(R.id.dialog_number_message);
        mNumberPicker = (NumberPicker) contentView.findViewById(R.id.dialog_number_picker);

        String title = getArguments().getString(KEY_TITlE);
        String message = getArguments().getString(KEY_MESSAGE);
        int minValue = getArguments().getInt(KEY_MIN_VAL, 0);
        int maxValue = getArguments().getInt(KEY_MAX_VAL, Integer.MAX_VALUE);
        boolean wrapSelectorWheel = getArguments().getBoolean(KEY_WRAP_SELECTOR, true);

        messageText.setText(message);
        mNumberPicker.setMinValue(minValue);
        mNumberPicker.setMaxValue(maxValue);
        mNumberPicker.setWrapSelectorWheel(wrapSelectorWheel);

        if (savedInstanceState == null) {
            int defaultValue = getArguments().getInt(KEY_DEFAULT_VAL, minValue);
            mNumberPicker.setValue(defaultValue);
        } else {
            mNumberPicker.setValue(savedInstanceState.getInt(KEY_SAVED_VAL));
        }

        setNumberPickerAccentColor(getContext(), mNumberPicker);

        return new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(contentView)
                .setPositiveButton(R.string.action_done, (dialogInterface, i) -> onValueSelected())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static void setNumberPickerAccentColor(Context context, NumberPicker picker) {
        TypedArray arr = context.getTheme().obtainStyledAttributes(new int[]{R.attr.colorAccent});
        int accentColor = arr.getColor(0, Color.TRANSPARENT);
        arr.recycle();

        try {
            Field selectionDivider = picker.getClass().getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            selectionDivider.set(picker, new ColorDrawable(accentColor));
        } catch (Exception exception) {
            Timber.e(exception, "Failed to set NumberPicker color");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SAVED_VAL, mNumberPicker.getValue());
    }

    private void onValueSelected() {
        int value = mNumberPicker.getValue();
        Activity parent = getActivity();

        String resultFragmentTag = getArguments().getString(KEY_RESULT_FRAGMENT);
        Fragment resultFragment = getFragmentManager().findFragmentByTag(resultFragmentTag);

        if (resultFragmentTag != null && resultFragment instanceof OnNumberPickedListener) {
            ((OnNumberPickedListener) resultFragment).onNumberPicked(value);
        } else if (parent instanceof OnNumberPickedListener) {
            ((OnNumberPickedListener) parent).onNumberPicked(value);
        } else {
            String targetClassName = (resultFragmentTag == null)
                    ? parent.getClass().getSimpleName()
                    : resultFragmentTag.getClass().getSimpleName();

            Timber.w("%s does not implement OnNumberPickedListener. Ignoring chosen value.",
                    targetClassName);
        }
    }

    public interface OnNumberPickedListener {
        void onNumberPicked(int chosen);
    }

    public static class Builder {

        private FragmentManager mFragmentManager;

        private String mTitle;
        private String mMessage;
        private String mResultFragment;
        private int mMin;
        private int mMax;
        private int mDefault;
        private boolean mWrapSelectorWheel;

        public Builder(AppCompatActivity activity) {
            mFragmentManager = activity.getSupportFragmentManager();
            mResultFragment = null;
        }

        public Builder(Fragment fragment) {
            mFragmentManager = fragment.getFragmentManager();
            mResultFragment = fragment.getTag();
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Builder setMinValue(int min) {
            mMin = min;
            return this;
        }

        public Builder setMaxValue(int max) {
            mMax = max;
            return this;
        }

        public Builder setDefaultValue(int value) {
            mDefault = value;
            return this;
        }

        public Builder setWrapSelectorWheel(boolean wrapSelectorWheel) {
            mWrapSelectorWheel = wrapSelectorWheel;
            return this;
        }

        public void show(String tag) {
            Bundle args = new Bundle();
            args.putString(KEY_TITlE, mTitle);
            args.putString(KEY_MESSAGE, mMessage);
            args.putString(KEY_RESULT_FRAGMENT, mResultFragment);
            args.putInt(KEY_MIN_VAL, mMin);
            args.putInt(KEY_MAX_VAL, mMax);
            args.putInt(KEY_DEFAULT_VAL, mDefault);
            args.putBoolean(KEY_WRAP_SELECTOR, mWrapSelectorWheel);

            NumberPickerDialogFragment dialogFragment = new NumberPickerDialogFragment();
            dialogFragment.setArguments(args);

            dialogFragment.show(mFragmentManager, tag);
        }
    }
}
