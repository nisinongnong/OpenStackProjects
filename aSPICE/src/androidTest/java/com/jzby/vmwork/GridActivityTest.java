package com.jzby.vmwork;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.action.ViewActions.click;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Created by T530 on 2017/5/25.
 */
@RunWith(AndroidJUnit4.class)
public class GridActivityTest {
    @Rule
    public ActivityTestRule<GridActivity> mActivityRule = new ActivityTestRule<>(
            GridActivity.class);

    @Test
    public void getItemData(){
        onData(allOf(is(instanceOf(Map.class)))).atPosition(1).perform(click());
        onData(allOf(is(instanceOf(Map.class)))).atPosition(2).perform(click());
    }


}
