package com.example.cyc.myapplication.ui;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cyc.myapplication.R;
import com.example.cyc.myapplication.service.MusicService;
import com.example.cyc.myapplication.utils.App;
import com.example.cyc.myapplication.utils.MediaUtil;
import com.example.cyc.myapplication.utils.MusicInfo;




import java.util.ArrayList;
import java.util.List;




/**
 * Created by panl on 14/10/30.
 */
public class MusicPlayingActivity extends BaseActivity {
    private TextView artistText, currentTimeText, totalTimeText;
    private ImageButton previousButton, playAndPauseButton, nextButton,
            repeatStateButton, favoriteButton;
    private SeekBar seekBar;
    private List<MusicInfo> musicInfoList;
    private static int musicNum;
    private int position;
    private int repeatState;
    private boolean favoriteState;
    private boolean isPlaying = true;
    private static int seekBarProgress;
    private long offset = 0;
    private long begin = 0;
    private long pauseTimeMills;
    private SharedPreferences sharedPreferences;

    private final static int PREVIOUS = 1;
    private final static int NEXT = 2;

    private UpdateTimeCallback updateTimeCallback = null;
    private Handler handler = new Handler();
    private MusicReceiver musicReceiver;
    private TextView lrcView;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_playing);
        initView();
        setViewText(position);
        setListener();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isPlaying", isPlaying);
        editor.commit();
        showLrc(position);

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
      //  MobclickAgent.onPause(this);
    }

    private void setViewText(int position) {
        MusicInfo musicInfo = musicInfoList.get(position);
        artistText.setText(musicInfo.getMusicArtist());
        totalTimeText.setText(MediaUtil.formatTime(musicInfoList.get(position).getMusicDuration()));
        if (isPlaying)
            playAndPauseButton.setBackgroundResource(R.mipmap.btn_pause_normal);

        favoriteState = sharedPreferences.getBoolean(App.FAVORITE_STATE + musicInfo.getMusicId(), false);
        if (favoriteState) {
            favoriteButton.setBackgroundResource(R.mipmap.action_favorite_selected);
        } else {
            favoriteButton.setBackgroundResource(R.mipmap.action_favorite_normal);
        }
        getSupportActionBar().setTitle(musicInfo.getMusicTitle());
    }

    /**
     * 初始化view控件
     */
    private void initView() {
        artistText = (TextView) findViewById(R.id.artist_text);
        favoriteButton = (ImageButton) findViewById(R.id.favorite_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playAndPauseButton = (ImageButton) findViewById(R.id.play_and_pause_button);
        nextButton = (ImageButton) findViewById(R.id.next_button);
        repeatStateButton = (ImageButton) findViewById(R.id.repeat_state_button);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentTimeText = (TextView) findViewById(R.id.current_time_text);
        totalTimeText = (TextView) findViewById(R.id.total_time_text);
        lrcView = (TextView) findViewById(R.id.lrc_view);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            // Set Navigation Toggle
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        } else {
            //throw new NullPointerException("Toolbar must be <include> in activity's layout!");
        }

        sharedPreferences = getSharedPreferences(App.APP_DATE, MODE_PRIVATE);

        repeatState = sharedPreferences.getInt("repeatState", App.allRepeat);
        switch (repeatState) {
            case App.allRepeat:
                repeatStateButton.setBackgroundResource(R.mipmap.btn_repeat_normal);
                break;
            case App.randomRepeat:
                repeatStateButton.setBackgroundResource(R.mipmap.btn_shuffle_normal);
                break;
            case App.singleRepeat:
                repeatStateButton.setBackgroundResource(R.mipmap.btn_singlerepeat_normal);
                break;
        }

        Intent intent = this.getIntent();
        position = intent.getIntExtra("position", 0);
        musicInfoList = (List) intent.getCharSequenceArrayListExtra("musicInfoList");
        musicNum = musicInfoList.size();
        begin = System.currentTimeMillis();
        updateTimeCallback = new UpdateTimeCallback(0);
        handler.post(updateTimeCallback);
        //playService(App.MEDIA_PLAY);
        //注册Receiver
        musicReceiver = new MusicReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(App.UPDATE_VIEW);
        registerReceiver(musicReceiver, filter);
    }

    /**
     * 启动service，播放相应的歌曲
     *
     * @param i
     */
    private void playService(int i) {
        Intent serviceIntent = new Intent(MusicPlayingActivity.this, MusicService.class);
        serviceIntent.putExtra("position", position);
        serviceIntent.putCharSequenceArrayListExtra("musicInfoList", (ArrayList) musicInfoList);
        serviceIntent.putExtra("MSG", i);
        startService(serviceIntent);
    }

    private void playService(int i, int progress) {
        Intent serviceIntent = new Intent(MusicPlayingActivity.this, MusicService.class);
        serviceIntent.putExtra("MSG", i);
        serviceIntent.putExtra("position", position);
        serviceIntent.putCharSequenceArrayListExtra("musicInfoList", (ArrayList) musicInfoList);
        serviceIntent.putExtra("progress", progress);
        startService(serviceIntent);
    }

    /**
     * 获得歌曲的位置，播放对应位置的歌曲
     *
     * @param flag
     */
    private void getMusicPosition(int flag) {
        if (flag == PREVIOUS) {
            if (position == 0) {
                position = musicNum - 1;
            } else {
                position -= 1;
            }
        } else if (flag == NEXT) {
            if (position == musicNum - 1) {
                position = 0;
            } else {
                position += 1;
            }
        }
    }

    /**
     * 给按钮设置监听器，所需要执行的操作
     */
    private void setListener() {
        //收藏按钮
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            @Override
            public void onClick(View v) {
                if (favoriteState) {
                    favoriteButton.setBackgroundResource(R.mipmap.action_favorite_normal);
                    favoriteState = false;
                } else {
                    favoriteButton.setBackgroundResource(R.mipmap.action_favorite_selected);
                    favoriteState = true;
                }
                editor.putBoolean(App.FAVORITE_STATE + musicInfoList.get(position).getMusicId(), favoriteState);
                editor.commit();
            }
        });
        //上一曲
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMusicPosition(PREVIOUS);
                //if (isPlaying)
                playService(App.MEDIA_PLAY);
                //else
                //playService(App.MEDIA_NEXT);
                isPlaying = true;
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isPlaying", isPlaying);
                editor.commit();

                playAndPauseButton.setBackgroundResource(R.mipmap.btn_pause_normal);
                updateTimeCallback = new UpdateTimeCallback(0);
                begin = System.currentTimeMillis();
                handler.post(updateTimeCallback);
                Intent sendIntent = new Intent(App.UPDATE_VIEW);
                sendIntent.putExtra("position", position);
                sendBroadcast(sendIntent);

//                String lrcPath = musicInfoList.get(position).getMusicPath()
//                        .substring(0,musicInfoList.get(position).getMusicPath().lastIndexOf("/"))
//                        +"/"+musicInfoList.get(position).getMusicTitle()+"_"
//                        +musicInfoList.get(position).getMusicArtist()+".lrc";
                //initLrcView(lrcPath);
                showLrc(position);
                //setViewText(position);
            }
        });
        //下一曲
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMusicPosition(NEXT);
                //if (isPlaying) {
                playService(App.MEDIA_PLAY);
                updateTimeCallback = new UpdateTimeCallback(0);
                begin = System.currentTimeMillis();
                handler.post(updateTimeCallback);
                //}
//                else {
//                    playService(App.MEDIA_NEXT);
//                    updateTimeCallback = new UpdateTimeCallback(0);
//                    handler.post(updateTimeCallback);
//                    handler.removeCallbacks(updateTimeCallback);
//                    pauseTimeMills = System.currentTimeMillis();
//                }
                isPlaying = true;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isPlaying", isPlaying);
                editor.commit();

                playAndPauseButton.setBackgroundResource(R.mipmap.btn_pause_normal);
                Intent sendIntent = new Intent(App.UPDATE_VIEW);
                sendIntent.putExtra("position", position);
                sendBroadcast(sendIntent);
                //setViewText(position);

//                String lrcPath = musicInfoList.get(position).getMusicPath()
//                        .substring(0,musicInfoList.get(position).getMusicPath().lastIndexOf("/"))
//                        +"/"+musicInfoList.get(position).getMusicTitle()+"_"
//                        +musicInfoList.get(position).getMusicArtist()+".lrc";
                //initLrcView(lrcPath);
                showLrc(position);
            }
        });
        //实现音乐的暂停和播放
        playAndPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    playService(App.MEDIA_PAUSE);
                    handler.removeCallbacks(updateTimeCallback);
                    pauseTimeMills = System.currentTimeMillis();
                    playAndPauseButton.setBackgroundResource(R.mipmap.btn_play_normal);
                } else {
                    playService(App.MEDIA_CONTINUE);
                    begin = System.currentTimeMillis() - pauseTimeMills + begin;
                    handler.post(updateTimeCallback);
                    playAndPauseButton.setBackgroundResource(R.mipmap.btn_pause_normal);
                }
                isPlaying = isPlaying ? false : true;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isPlaying", isPlaying);
                editor.commit();
            }
        });
        //实现进度条随音乐播放移动，实现快进，快退功能
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true) {
                    Log.i("Progress------>", progress + "");
                    offset = (int) (progress * musicInfoList.get(position).getMusicDuration()) / 100;
                    seekBarProgress = progress;

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playService(App.MEDIA_SEEKTO, seekBarProgress);
                begin = System.currentTimeMillis();
                updateTimeCallback = new UpdateTimeCallback(seekBarProgress);
                isPlaying = true;

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isPlaying", isPlaying);
                editor.commit();

                String lrcPath = musicInfoList.get(position).getMusicPath()
                        .substring(0, musicInfoList.get(position).getMusicPath().lastIndexOf("/"))
                        + "/" + musicInfoList.get(position).getMusicTitle() + "_"
                        + musicInfoList.get(position).getMusicArtist() + ".lrc";

                playAndPauseButton.setBackgroundResource(R.mipmap.btn_pause_normal);
                handler.post(updateTimeCallback);
            }
        });
        //切换歌曲循环状态
        repeatStateButton.setOnClickListener(new View.OnClickListener() {

            SharedPreferences.Editor editor = sharedPreferences.edit();

            @Override
            public void onClick(View v) {
                if (repeatState == App.allRepeat) {
                    repeatState = App.randomRepeat;
                    repeatStateButton.setBackgroundResource(R.mipmap.btn_shuffle_normal);
                    editor.putInt("repeatState", repeatState);
                    editor.commit();
                } else if (repeatState == App.randomRepeat) {
                    repeatState = App.singleRepeat;
                    repeatStateButton.setBackgroundResource(R.mipmap.btn_singlerepeat_normal);
                    editor.putInt("repeatState", repeatState);
                    editor.commit();
                } else if (repeatState == App.singleRepeat) {
                    repeatState = App.allRepeat;
                    repeatStateButton.setBackgroundResource(R.mipmap.btn_repeat_normal);
                    editor.putInt("repeatState", repeatState);
                    editor.commit();
                }

            }
        });
    }

    /**
     * 接受service发出的广播
     */
    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(App.UPDATE_VIEW)) {
                position = intent.getIntExtra("position", 0);
                setViewText(position);
                //lrcView = (LyricView)findViewById(R.id.lrc_view);
                showLrc(position);
                handler.removeCallbacks(updateTimeCallback);
                begin = System.currentTimeMillis();
                updateTimeCallback = new UpdateTimeCallback(0);
                handler.post(updateTimeCallback);
            }
        }
    }

    /**
     * 更新进度条
     */
    class UpdateTimeCallback implements Runnable {

        int seekTo = 0;

        public UpdateTimeCallback(int seekTo) {
            this.seekTo = seekTo;
        }

        @Override
        public void run() {
            //计算当前歌曲已经播放的时间
            offset = System.currentTimeMillis() - begin + seekTo;
            try {
                if (offset <= musicInfoList.get(position).getMusicDuration()) {
                    //更新进度条
                    seekBarProgress = (int) Math.floor(
                            offset * 100 / musicInfoList.get(position).getMusicDuration());
                    seekBar.setProgress(seekBarProgress);
                    currentTimeText.setText(MediaUtil.formatTime(offset));
                }
            } catch (Exception e) {

            }
            handler.postDelayed(updateTimeCallback, 10);

        }
    }

    /**
     * 用于判断歌词是否成功保存并执行相应操作
     */
    Handler lrcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == App.SUCCESS) {
                String lrcPath = musicInfoList.get(position).getMusicPath()
                        .substring(0, musicInfoList.get(position).getMusicPath().lastIndexOf("/"))
                        + "/" + musicInfoList.get(position).getMusicTitle() + "_"
                        + musicInfoList.get(position).getMusicArtist() + ".lrc";
            } else {
                Toast.makeText(MusicPlayingActivity.this, "没有找到歌曲资源", Toast.LENGTH_SHORT).show();
                lrcView.setText("没有找到歌词");

            }
        }
    };

    /**
     * 用于下载并显示歌词
     */
    public void showLrc(int position) {
    lrcView.setText("此功能待开发");
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            MusicPlayingActivity.this.finish();
            this.overridePendingTransition(R.anim.activity_close, 0);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            this.overridePendingTransition(R.anim.activity_close, 0);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(musicReceiver);
    }
}