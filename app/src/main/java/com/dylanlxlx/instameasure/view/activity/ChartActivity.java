package com.dylanlxlx.instameasure.view.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.dylanlxlx.instameasure.R;
import com.dylanlxlx.instameasure.service.SensorService;
import com.dylanlxlx.instameasure.view.fragment.AccelerometerFragment;
import com.dylanlxlx.instameasure.view.fragment.GyroscopeFragment;
import com.dylanlxlx.instameasure.view.fragment.MagneticFieldFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Activity for displaying sensor data charts.
 * Uses ViewPager2 to navigate between three fragments showing different sensor data.
 */
public class ChartActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private SensorService sensorService;
    private boolean isSensorServiceBound = false;

    // Service connection
    private final ServiceConnection sensorServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            sensorService = binder.getService();
            isSensorServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSensorServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // Set up back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sensor Charts");
        }

        // Start and bind to the SensorService
        Intent sensorIntent = new Intent(this, SensorService.class);
        startService(sensorIntent);
        bindService(sensorIntent, sensorServiceConnection, Context.BIND_AUTO_CREATE);

        // Initialize ViewPager and TabLayout
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        // Set up adapter for ViewPager
        ChartPagerAdapter pagerAdapter = new ChartPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Accelerometer");
                    break;
                case 1:
                    tab.setText("Gyroscope");
                    break;
                case 2:
                    tab.setText("Magnetometer");
                    break;
            }
        }).attach();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isSensorServiceBound) {
            unbindService(sensorServiceConnection);
            isSensorServiceBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adapter for the ViewPager to manage the three chart fragments
     */
    private static class ChartPagerAdapter extends FragmentStateAdapter {

        public ChartPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new AccelerometerFragment();
                case 1:
                    return new GyroscopeFragment();
                case 2:
                    return new MagneticFieldFragment();
                default:
                    throw new IllegalArgumentException("Invalid position: " + position);
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Three tabs: Accelerometer, Gyroscope, Magnetometer
        }
    }
}