package com.dylanlxlx.instameasure.view.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.service.SensorService;
import com.dylanlxlx.instameasure.view.fragment.AccelerometerFragment;
import com.dylanlxlx.instameasure.view.fragment.GyroscopeFragment;
import com.dylanlxlx.instameasure.view.fragment.MagneticFieldFragment;

public class ChartActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private SensorService sensorService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            sensorService = binder.getService();
            setupViewPager();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sensorService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        viewPager = findViewById(R.id.view_pager);
        Intent intent = new Intent(this, SensorService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupViewPager() {
        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0: return new AccelerometerFragment(sensorService);
                    case 1: return new GyroscopeFragment(sensorService);
                    case 2: return new MagneticFieldFragment(sensorService);
                    default: return null;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0: return "加速度计";
                    case 1: return "陀螺仪";
                    case 2: return "磁场计";
                    default: return "";
                }
            }
        };
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}