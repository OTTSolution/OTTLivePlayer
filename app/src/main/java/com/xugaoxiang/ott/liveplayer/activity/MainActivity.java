package com.xugaoxiang.ott.liveplayer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xugaoxiang.ott.liveplayer.adapter.ProgramAdapter;
import com.xugaoxiang.ott.liveplayer.bean.LiveBean;
import com.xugaoxiang.ott.liveplayer.utils.NetWorkUtils;
import com.xugaoxiang.ott.liveplayer.utils.PreUtils;
import com.xugaoxiang.ott.liveplayer.view.MyListView;
import com.xugaoxiang.ott.liveplayer.view.RotaProgressBar;
import com.xugaoxiang.ott.liveplayer.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
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
    @Bind(R.id.tv_speed)
    TextView tvSpeed;
    @Bind(R.id.tv_net_state)
    TextView tvNetState;
    public static LiveBean liveBean;
    @Bind(R.id.tv_program)
    TextView tvProgram;
    private TranslateAnimation animIn;
    private int programIndex;

    private final static String PROGRAM_KEY = "programIndex";

    private final static int CODE_SHOWLOADING = 1;

    private final static int CODE_STOP_SHOWLOADING = 2;

    private final static int CODE_GONE_PROGRAMINFO = 3;

    private final static int CODE_NET_STATE = 4;

    private final static int CODE_HIDE_BLACK = 5;

    private final static int CODE_VIDEO_ERROR = 6;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_SHOWLOADING:
                    showLoading();
                    handler.sendEmptyMessageDelayed(CODE_SHOWLOADING, 1000);
                    break;
                case CODE_STOP_SHOWLOADING:
                    hideLoading();
                    handler.removeMessages(CODE_SHOWLOADING);
                    break;
                case CODE_GONE_PROGRAMINFO:
                    rlDisplay.setVisibility(View.INVISIBLE);
                    break;
                case CODE_NET_STATE:
                    tvNetState.setVisibility(View.INVISIBLE);
                    break;
                case CODE_HIDE_BLACK:
                    tvBlack.setVisibility(View.INVISIBLE);
                    break;
                case CODE_VIDEO_ERROR:
                    hideLoading();
                    handler.removeMessages(CODE_SHOWLOADING);
                    tvNetState.setVisibility(View.VISIBLE);
                    tvNetState.setText("无法播放该节目，请检查网络！");
                    break;
            }
        }
    };
    private TranslateAnimation exitAnim;
    private long lastTotalRxBytes;
    private long lastTimeStamp;
    private NetworkReceiver receiver;
    private int i;
    private boolean isLongPressKey;
    private boolean lockLongPressKey;
    private long keyFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Vitamio.isInitialized(getApplicationContext());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        lastTotalRxBytes = getTotalRxBytes();
        lastTimeStamp = System.currentTimeMillis();
        setAdapter();
        programIndex = PreUtils.getInt(this, PROGRAM_KEY, 0);
        if (programIndex >= liveBean.getData().size()) {
            programIndex = 0;
        }
        showLoading();
        playVideo();
        initListener();
        if (!TextUtils.isEmpty(GetServiceProgramList.language) && GetServiceProgramList.language.equals("英文")){
            tvProgram.setText("Programs");
        }
    }

    private void initListener() {
        mVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {

            }
        });
        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                handler.sendEmptyMessageDelayed(CODE_HIDE_BLACK, 500);
                handler.sendEmptyMessageDelayed(CODE_GONE_PROGRAMINFO, 5000);
                handler.sendEmptyMessage(CODE_STOP_SHOWLOADING);
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                handler.sendEmptyMessage(CODE_VIDEO_ERROR);
                return false;
            }
        });
    }

    private void playNext() {
        tvNetState.setVisibility(View.VISIBLE);
        tvNetState.setText("无法播放该节目,正在为您播放下一个节目!");
        next();
        showProgramInfo();
        cutProgram();
        handler.sendEmptyMessageDelayed(CODE_NET_STATE, 2000);
    }

    private void playVideo() {
        mVideoView.setVideoPath(liveBean.getData().get(programIndex).getUrl());
        mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, VideoView.VIDEO_LAYOUT_FIT_PARENT);
        mVideoView.start();
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
                togglePlaylist();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                if (keyFlag % 2 == 0){
                    previous();
                    showProgramInfo();
                }else {
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                if (keyFlag % 2 == 0){
                    next();
                    showProgramInfo();
                }else {
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
                if(lockLongPressKey){
                    lockLongPressKey = false;
                    return true;
                }
                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                cutProgram();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if(lockLongPressKey){
                    lockLongPressKey = false;
                    return true;
                }
                if (llProgramList.getVisibility() == View.VISIBLE) {
                    return false;
                }
                cutProgram();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void cutProgram() {
        handler.sendEmptyMessage(CODE_SHOWLOADING);
        if (tvNetState.getVisibility() == View.VISIBLE){
            tvNetState.setVisibility(View.INVISIBLE);
        }
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
        mVideoView.setVideoPath(liveBean.getData().get(programIndex).getUrl());
        mVideoView.start();
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
                    cutProgram();
                }
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
        if (!TextUtils.isEmpty(GetServiceProgramList.language) && GetServiceProgramList.language.equals("英文")) {
            tvProgramName.setText(liveBean.getData().get(programIndex).getEn_name());
        } else {
            tvProgramName.setText(liveBean.getData().get(programIndex).getName());
        }
        tvProgramNumber.setText(liveBean.getData().get(programIndex).getNum());
        tvSystemTime.setText(getDtate());
    }

    public String getDtate() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd    HH:mm");
        return sDateFormat.format(new Date());
    }

    @Override
    public void onBackPressed() {
        if (llProgramList.getVisibility() == View.VISIBLE) {
            exitProgramList();
        } else {
            finish();
        }
    }

    private void showLoading() {
        if (pbLoading.getVisibility() == View.INVISIBLE) {
            pbLoading.setVisibility(View.VISIBLE);
            tvSpeed.setVisibility(View.VISIBLE);
        }
        tvSpeed.setText(getNetSpeed());
    }

    private String getNetSpeed() {
        long nowTotalRxBytes = getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long speed = 0;
        long s = nowTimeStamp - lastTimeStamp;
        if (s <= 0) {
            s = 1;
        }
        speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (s));//毫秒转换

        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return String.valueOf(speed) + "KB/s";
    }

    private long getTotalRxBytes() {
        return TrafficStats.getUidRxBytes(getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
    }

    private void hideLoading() {
        pbLoading.setVisibility(View.INVISIBLE);
        tvSpeed.setVisibility(View.GONE);
    }

    private void exitProgramList() {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (!mVideoView.isPlaying()) {
            mVideoView.start();
        }
        showProgramInfo();
        registerNetReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    private void registerNetReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreUtils.setInt(this, PROGRAM_KEY, programIndex);
        unregisterReceiver(receiver);
        mVideoView.stopPlayback();
    }

    class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetWorkUtils.getNetState(context)) {
                tvNetState.setVisibility(View.VISIBLE);
                tvNetState.setText("网络已断开!");
            } else {
                if (tvNetState.getVisibility() == View.VISIBLE) {
                    tvNetState.setText("网络已连接!");
                    handler.sendEmptyMessageDelayed(CODE_NET_STATE, 4000);
                }
            }
        }
    }
}
