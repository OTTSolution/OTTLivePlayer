package com.xugaoxiang.djstava.live_vtm.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xugaoxiang.djstava.live_vtm.adapter.ProgramAdapter;
import com.xugaoxiang.djstava.live_vtm.bean.LiveBean;
import com.xugaoxiang.djstava.live_vtm.utils.NetWorkUtils;
import com.xugaoxiang.djstava.live_vtm.utils.PreUtils;
import com.xugaoxiang.djstava.live_vtm.view.MyListView;
import com.xugaoxiang.djstava.live_vtm.view.RotaProgressBar;
import com.xugaoxiang.djstava.live_vtm.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;

public class MainActivity extends Activity {

    @Bind(R.id.surface_view)
    VideoView mVideoView;
    @Bind(R.id.tv_black)
    TextView tvBlack;
    @Bind(R.id.tv_program_number)
    TextView tvProgramNumber;
    @Bind(R.id.tv_program_name)
    TextView tvProgramName;
    @Bind(R.id.tv_system_time)
    TextView tvSystemTime;
    @Bind(R.id.rl_display)
    RelativeLayout rlDisplay;
    @Bind(R.id.lv_program)
    MyListView lvProgram;
    @Bind(R.id.ll_program_list)
    LinearLayout llProgramList;
    @Bind(R.id.pb_loading)
    RotaProgressBar pbLoading;
    @Bind(R.id.tv_cache)
    TextView tvCache;
    @Bind(R.id.tv_net_state)
    TextView tvNetState;
    public static LiveBean liveBean;
    @Bind(R.id.tv_program)
    TextView tvProgram;
    private TranslateAnimation animIn;
    private int programIndex;

    private final static String PROGRAM_KEY = "programIndex";


    private final static int CODE_GONE_PROGRAMINFO = 1;
    private final static int CODE_HIDE_BLACK = 2;


    private static final String TAG = MainActivity.class.getName();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_GONE_PROGRAMINFO:
                    rlDisplay.setVisibility(View.INVISIBLE);
                    break;

                case CODE_HIDE_BLACK:
                    tvBlack.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    };

    private TranslateAnimation exitAnim;
    private long lastTimeStamp;
    private NetworkReceiver receiver;
    private boolean lockLongPressKey;
    private long keyFlag = 1;
    private long currentTime;

    private int currentListItemID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LibsChecker.checkVitamioLibs(this))
            return;

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        lastTimeStamp = System.currentTimeMillis();
        setAdapter();
        programIndex = PreUtils.getInt(this, PROGRAM_KEY, 0);
        if (programIndex >= liveBean.getData().size()) {
            programIndex = 0;
        }

        initPlayer();
        initListener();
        if (!TextUtils.isEmpty(ServiceProgramActivity.language) && ServiceProgramActivity.language.equals("英文")) {
            tvProgram.setText("Programs");
        }

    }

    private void initListener() {
        mVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent == 100) {
                    hideLoading();
                    mp.start();
                } else {
                    showLoading();
                    tvCache.setText("缓冲: " + percent + "%");
                    if (mp.isPlaying()) {
                        mp.pause();
                    }
                }
            }
        });

        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {

                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        if (mVideoView.isPlaying()) {
                            mVideoView.pause();
                        }
                        showLoading();
                        break;

                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        hideLoading();
                        mp.start();
                        break;

                    case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                        break;

                    case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                        break;
                }

                return false;
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                handler.sendEmptyMessageDelayed(CODE_HIDE_BLACK, 500);
                handler.sendEmptyMessageDelayed(CODE_GONE_PROGRAMINFO, 5000);
                long lastTime = SystemClock.currentThreadTimeMillis();

                Log.e(TAG, "lastTime:" + lastTime + ",gapTime:" + (lastTime - currentTime));

                mediaPlayer.setBufferSize(512 * 1024);
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "播放出错！" + "what:" + what + ",extra:" + extra);
                Toast.makeText(MainActivity.this, "播放出错！what:" + what + ",extra:" + extra, Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(MainActivity.this, "播放完毕！", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    private void initPlayer() {
        Log.e(TAG, liveBean.getData().get(programIndex).getUrl());

        mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, VideoView.VIDEO_LAYOUT_FIT_PARENT);
        mVideoView.setVideoURI(Uri.parse(liveBean.getData().get(programIndex).getUrl()));
        mVideoView.setHardwareDecoder(true);
        mVideoView.requestFocus();

        currentTime = SystemClock.currentThreadTimeMillis();
        Log.e(TAG, "currentTime:" + currentTime);
    }

    public static void openLive(Context context, LiveBean liveBean) {
        MainActivity.liveBean = liveBean;
        context.startActivity(new Intent(context, MainActivity.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        keyFlag++;
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                togglePlaylist();
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (currentListItemID == 0) {
                    lvProgram.setSelection(MainActivity.liveBean.getData().size() - 1);
                }

                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                if (keyFlag % 2 == 0) {
                    previous();
                    showProgramInfo();
                } else {
                    return false;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (currentListItemID == MainActivity.liveBean.getData().size() - 1) {
                    lvProgram.setSelection(0);
                }

                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }

                if (keyFlag % 2 == 0) {
                    next();
                    showProgramInfo();
                } else {
                    return false;
                }
                break;

            case KeyEvent.KEYCODE_MENU:
                showProgramInfo();
                handler.sendEmptyMessageDelayed(CODE_GONE_PROGRAMINFO, 4000);
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        keyFlag = 1;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (lockLongPressKey) {
                    lockLongPressKey = false;
                    return true;
                }
                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                play();
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (lockLongPressKey) {
                    lockLongPressKey = false;
                    return true;
                }
                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                play();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void play() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }

        mVideoView.setVideoURI(Uri.parse(liveBean.getData().get(programIndex).getUrl()));
    }

    private void setAdapter() {
        if (MainActivity.liveBean != null) {
            final ProgramAdapter programAdapter = new ProgramAdapter(this);
            lvProgram.setAdapter(programAdapter);
        } else {
            Toast.makeText(this, "打开播放列表失败，请检查网络！", Toast.LENGTH_SHORT).show();
        }

        lvProgram.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position != programIndex) {
                    programIndex = position;
                    play();
                }
            }
        });

        lvProgram.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentListItemID = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void togglePlaylist() {
        if (animIn == null) {
            animIn = new TranslateAnimation(-llProgramList.getWidth(), 0f, 0f, 0f);
            animIn.setDuration(300);
        }
        animIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                llProgramList.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        llProgramList.startAnimation(animIn);
        lvProgram.setSelection(programIndex);
    }

    private void previous() {
        tvBlack.setVisibility(View.VISIBLE);
        programIndex--;
        if (programIndex < 0) {
            programIndex = liveBean.getData().size() - 1;
        }
        handler.removeMessages(CODE_GONE_PROGRAMINFO);
    }

    private void next() {
        tvBlack.setVisibility(View.VISIBLE);
        programIndex++;
        if (programIndex >= liveBean.getData().size()) {
            programIndex = 0;
        }
        handler.removeMessages(CODE_GONE_PROGRAMINFO);
    }

    private void showProgramInfo() {
        rlDisplay.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(ServiceProgramActivity.language) && ServiceProgramActivity.language.equals("英文")) {
            tvProgramName.setText(liveBean.getData().get(programIndex).getEn_name());
        } else {
            tvProgramName.setText(liveBean.getData().get(programIndex).getName());
        }
        tvProgramNumber.setText(liveBean.getData().get(programIndex).getNum());
        tvSystemTime.setText(getDate());
    }

    public String getDate() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd    HH:mm");
        return sDateFormat.format(new Date());
    }

    @Override
    public void onBackPressed() {
        if (llProgramList.getVisibility() == View.VISIBLE) {
            hideProgramList();
        } else {
            if (mVideoView != null) {
                mVideoView.stopPlayback();
                mVideoView = null;
            }

            finish();
        }
    }

    private void showLoading() {
        if (pbLoading.getVisibility() == View.INVISIBLE) {
            pbLoading.setVisibility(View.VISIBLE);
            tvCache.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (pbLoading.getVisibility() == View.VISIBLE) {
            pbLoading.setVisibility(View.INVISIBLE);
            tvCache.setVisibility(View.GONE);
        }
    }

    private void hideProgramList() {
        if (exitAnim == null) {
            exitAnim = new TranslateAnimation(0f, -llProgramList.getWidth(), 0f, 0f);
            exitAnim.setDuration(300);
        }
        exitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                llProgramList.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        llProgramList.startAnimation(exitAnim);
    }

    public void showNetworkInfo(String text) {
        if (tvNetState.getVisibility() == View.INVISIBLE) {
            tvNetState.setVisibility(View.VISIBLE);
        }

        tvNetState.setText(text);
    }

    public void hideNetworkInfo() {
        if (tvNetState.getVisibility() == View.VISIBLE) {
            tvNetState.setVisibility(View.INVISIBLE);
        }

        tvNetState.setText("");
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mVideoView.isPlaying()) {
            mVideoView.start();
        }
        showProgramInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();

        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreUtils.setInt(this, PROGRAM_KEY, programIndex);
        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        if (mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView = null;
            finish();
        }
    }

    class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetWorkUtils.getNetState(context)) {
                showNetworkInfo("网络已断开!");
                if (mVideoView != null) {
                    mVideoView.pause();
                }
            } else {
                hideNetworkInfo();
                if (!mVideoView.isPlaying()) {
                    mVideoView.start();
                }
            }
        }
    }
}
