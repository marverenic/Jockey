package com.marverenic.music.view.bindingadapters;

import android.databinding.BindingAdapter;
import android.databinding.InverseBindingAdapter;
import android.databinding.InverseBindingListener;
import android.databinding.InverseBindingMethod;
import android.databinding.InverseBindingMethods;
import android.databinding.adapters.ListenerUtil;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.marverenic.music.R;

@InverseBindingMethods({
        @InverseBindingMethod(type = ViewPager.class, attribute = "page")
})
public class ViewPagerBindingAdapters {

    private ViewPagerBindingAdapters() {
        throw new UnsupportedOperationException("BindingAdapters cannot be instantiated");
    }

    @BindingAdapter("page")
    public static void setPage(ViewPager pager, int page) {
        pager.setCurrentItem(page, false);
    }

    @InverseBindingAdapter(attribute = "page")
    public static int getPage(ViewPager pager) {
        return pager.getCurrentItem();
    }

    @BindingAdapter(value = {"onPageChange", "pageAttrChanged"}, requireAll = false)
    public static void setPageListeners(ViewPager pager, OnPageChangeListener onPageChangeListener,
                                        InverseBindingListener inverseBindingListener) {

        OnPageChangeListener newListener;
        if (inverseBindingListener == null) {
            newListener = onPageChangeListener;
        } else {
            newListener = new OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {
                    if (onPageChangeListener != null) {
                        onPageChangeListener.onPageScrolled(position,
                                positionOffset, positionOffsetPixels);
                    }
                }

                @Override
                public void onPageSelected(int position) {
                    if (onPageChangeListener != null) {
                        onPageChangeListener.onPageSelected(position);
                    }

                    inverseBindingListener.onChange();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    if (onPageChangeListener != null) {
                        onPageChangeListener.onPageScrollStateChanged(state);
                    }
                }
            };
        }

        OnPageChangeListener oldListener =
                ListenerUtil.trackListener(pager, newListener, R.id.view_pager_listener);

        if (oldListener != null) {
            pager.removeOnPageChangeListener(oldListener);
        }

        if (newListener != null) {
            pager.addOnPageChangeListener(newListener);
        }
    }

}
