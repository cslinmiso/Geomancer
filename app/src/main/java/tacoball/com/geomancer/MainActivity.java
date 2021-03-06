package tacoball.com.geomancer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * 前端程式進入點
 */
public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";

    private Fragment current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 配置 Android 繪圖資源，必須在 inflate 之前完成
        AndroidGraphicFactory.createInstance(getApplication());
        setContentView(R.layout.activity_main);

        // 配置廣播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE");
        this.registerReceiver(receiver, filter);

        // 配置 Fragment
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        try {
            // 使用者要求更新檢查
            boolean userRequest = MainUtils.hasUpdateRequest(this);
            MainUtils.clearUpdateRequest(this);

            // 必要檔案更新檢查
            int cnt = MainUtils.REQUIRED_FILES.length;
            int exists = 0;
            for (int i=0;i<cnt;i++) {
                File required = MainUtils.getFilePath(this, i);
                if (required.exists()) {
                    exists++;
                }
            }
            boolean needRequirements = (exists < cnt);

            String msg = String.format(Locale.getDefault(), "user=%s, sys=%s", userRequest, needRequirements);
            Log.d(TAG, msg);

            if (needRequirements || userRequest) {
                // 更新程式
                current = new UpdateToolFragment();
            } else {
                // 主畫面程式
                current = new MapViewFragment();
            }
            ft.add(R.id.frag_container, current);
        } catch(IOException ex) {
            // MainUtils.getFilePath() 發生錯誤
            Log.e(TAG, ex.getMessage());
        }

        ft.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void swapToUpdateUI() {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (current instanceof MapViewFragment) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.remove(current);
                    ft.add(R.id.frag_container, new UpdateToolFragment());
                    ft.commit();
                }
            }
        });
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE")) {
                Log.d(TAG, "Request update from broadcast.");
                MainUtils.clearUpdateRequest(MainActivity.this);
                swapToUpdateUI();
            }
        }

    };

}
