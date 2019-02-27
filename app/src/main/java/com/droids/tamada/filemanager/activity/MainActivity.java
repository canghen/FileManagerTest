package com.droids.tamada.filemanager.activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.droids.tamada.filemanager.app.AppController;
import com.droids.tamada.filemanager.fragments.AudiosListFragment;
import com.droids.tamada.filemanager.fragments.ExternalStorageFragment;
import com.droids.tamada.filemanager.fragments.ImagesListFragment;
import com.droids.tamada.filemanager.fragments.InternalStorageFragment;
import com.droids.tamada.filemanager.fragments.SettingsFragment;
import com.droids.tamada.filemanager.fragments.VideosListFragment;
import com.droids.tamada.filemanager.helper.ArcProgress;
import com.droids.tamada.filemanager.helper.MediaScannerUtil;
import com.droids.tamada.filemanager.helper.PreferManager;
import com.example.satish.filemanager.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final String TAG_INTERNAL_STORAGE = "INTERNAL STORAGE";
    public static String FG_TAG = TAG_INTERNAL_STORAGE;
    public static int navItemIndex = 0;
    private String[] activityTitles;
    private Handler mHandler;
    public static ButtonBackPressListener buttonBackPressListener;
    private PreferManager preferManager;
    private int i = 5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        preferManager = new PreferManager(AppController.getInstance().getApplicationContext());
        activityTitles = getResources().getStringArray(R.array.nav_item_activity_titles);
        if (savedInstanceState == null) {
            navItemIndex = 0;
            FG_TAG = TAG_INTERNAL_STORAGE;
            loadHomeFragment();
            if (preferManager.isFirstTimeLaunch()) {
                final Dialog homeGuideDialog = new Dialog(MainActivity.this, android.R.style.Theme_Translucent_NoTitleBar);
                homeGuideDialog.setContentView(R.layout.custom_guide_dialog);
                homeGuideDialog.show();
                RelativeLayout layout = (RelativeLayout) homeGuideDialog.findViewById(R.id.guide_layout);
                layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        preferManager.setFirstTimeLaunch(false);
                        homeGuideDialog.dismiss();
                    }
                });
            }
        }
        Button button_scan = findViewById(R.id.scan);
        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] filePaths = new String[]{Environment.getExternalStorageDirectory().getPath()};
                File[] files = new File[filePaths.length];
                for (int i = 0; i < filePaths.length; i++) {
                    files[i] = new File(filePaths[i]);
                    Log.e("======","========" + files[i].isDirectory());
                }
                new MediaScannerUtil(MainActivity.this).scanFiles(files);
            }
        });
    }


    private void setActivityTitle() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(activityTitles[navItemIndex]);
    }

    private void loadHomeFragment() {
        setActivityTitle();
        invalidateOptionsMenu();

        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                // update the main_internal_storage content by replacing fragments
                Fragment fragment = getHomeFragment();
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.frame, fragment, FG_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        };

        // If mPendingRunnable is not null, then add to the message queue
        if (mPendingRunnable != null) {
            mHandler.post(mPendingRunnable);
        }
    }

    private Fragment getHomeFragment() {
        switch (navItemIndex) {
            case 0:
                return new InternalStorageFragment();
            case 1:
                return new ExternalStorageFragment();
            case 2:
                return new ImagesListFragment();
            case 3:
                return new AudiosListFragment();
            case 4:
                return new VideosListFragment();
            case 5:
                return new SettingsFragment();
            default:
                return new InternalStorageFragment();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (navItemIndex > 1) {
                navItemIndex = 0;
                FG_TAG = TAG_INTERNAL_STORAGE;
                loadHomeFragment();
            } else {
                buttonBackPressListener.onButtonBackPressed(navItemIndex);
            }
        }
    }

    public interface ButtonBackPressListener {
        void onButtonBackPressed(int navItemIndex);
    }
}
