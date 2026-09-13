package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.IStatusObserver;
import com.kingdom.game.controller.IWaveStarter;
import com.kingdom.game.util.audio.SoundManager;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * HUD —— 顶部状态栏（实现 IStatusObserver，被后端回调刷新）。
 * 含：生命 / 金币 / 波次 / 公告消息 + 「开始波次」按钮（绑定 IWaveStarter）。
 * 另负责「漏怪扣生命」提示音的触发点：生命值下降时直调 SoundManager（见《音效扩展方法说明》§二）。
 */
public class HUD implements IStatusObserver {

    private final Label livesLabel = new Label();
    private final Label goldLabel = new Label();
    private final Label waveLabel = new Label();
    private final Label msgLabel = new Label();
    private final Label countdownLabel = new Label();
    private final Button startButton = new Button("开始波次");
    /** 「暂停」：由装配层经 setOnPause 接线（按钮只回调，暂停语义归装配层编排） */
    private final Button pauseButton = new Button("暂停");

    private final IWaveStarter waveStarter;
    private final IGameStateReader stateReader;

    private final HBox bar;
    private final AnimationTimer countdownTimer;
    private String lastCountdownText = "";
    private String lastButtonText = "开始波次";
    /** 暂停动作（装配层注入）；null = 未接线 */
    private Runnable onPause;
    /** 是否处于暂停态（本类只用于停自己的计时器与置灰按钮） */
    private boolean paused = false;
    /** 上一次收到的生命值；-1 = 尚未收到首次推送（避免初始推送误触发漏怪音） */
    private int lastLives = -1;

    public HUD(IWaveStarter waveStarter, IGameStateReader stateReader) {
        this.waveStarter = waveStarter;
        this.stateReader = stateReader;

        livesLabel.setFont(Font.font(16));
        livesLabel.setTextFill(Color.DARKRED);

        goldLabel.setFont(Font.font(16));
        goldLabel.setTextFill(Color.web("#b8860b"));

        waveLabel.setFont(Font.font(15));

        msgLabel.setFont(Font.font(13));
        msgLabel.setTextFill(Color.GRAY);
        msgLabel.setMaxWidth(320);

        countdownLabel.setFont(Font.font(13));
        countdownLabel.setTextFill(Color.WHITE);

        startButton.setFont(Font.font(14));
        startButton.setOnAction(e -> {
            if (waveStarter.canStartNextWave()) {
                waveStarter.startNextWave();
                refreshButtonState();
            }
        });

        pauseButton.setFont(Font.font(14));
        pauseButton.setOnAction(e -> {
            if (onPause != null) onPause.run();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar = new HBox(18, livesLabel, goldLabel, waveLabel, msgLabel, spacer, countdownLabel, startButton, pauseButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setStyle("-fx-background-color: #40454d;");

        // 每帧轮询 IGameStateReader：波间倒计时与"立即开始 +奖励"按钮（倒计时权威计时在后端，UI 只读拉取）
        countdownTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                hudTick();
            }
        };
        countdownTimer.start();
    }

    public Node getNode() { return bar; }

    /** 接线「暂停」按钮：由装配层在战场屏装配完成后调用 */
    public void setOnPause(Runnable onPause) {
        this.onPause = onPause;
    }

    /**
     * 暂停/恢复 HUD 侧计时。
     *
     * HUD 自持的 AnimationTimer 与战场循环相互独立：不停它的话，暂停期间倒计时标签会继续按真实
     * 时间走、按钮还会变成可点的「立即开始」，点下去真的会推进波次与金币（即"暂停中世界被推进"）。
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            countdownTimer.stop();
            startButton.setDisable(true);
            pauseButton.setDisable(true);   // 计时器停了不会再刷新它，这里显式置灰
        } else {
            countdownTimer.start();   // 下一帧 hudTick() 自会重算按钮可用态
        }
    }

    private void refreshWaveLabel() {
        waveLabel.setText("波次 " + stateReader.getCurrentWave() + " / " + stateReader.getTotalWaves());
        waveLabel.setTextFill(Color.WHITE);
    }

    private void refreshButtonState() {
        startButton.setDisable(!waveStarter.canStartNextWave());
    }

    /** 每帧同步：倒计时文字 / 按钮文案与可用态（仅在变化时刷新，避免无谓 setText） */
    private void hudTick() {
        // 终态或暂停中不再允许按「暂停」（终态时结束遮罩盖不住 HUD，只能在动作里挡）
        pauseButton.setDisable(paused || stateReader.isGameOver() || stateReader.isVictory());
        long ms = stateReader.getNextWaveCountdownMs();
        if (ms > 0) {
            int sec = (int) ((ms + 999) / 1000);
            String countdownText = sec + "s";
            if (!countdownText.equals(lastCountdownText)) {
                countdownLabel.setText(countdownText);
                lastCountdownText = countdownText;
            }
            String buttonText = "立即开始";
            if (!buttonText.equals(lastButtonText)) {
                startButton.setText(buttonText);
                lastButtonText = buttonText;
            }
            startButton.setDisable(false);
        } else {
            if (!lastCountdownText.isEmpty()) {
                countdownLabel.setText("");
                lastCountdownText = "";
            }
            if (!"开始波次".equals(lastButtonText)) {
                startButton.setText("开始波次");
                lastButtonText = "开始波次";
            }
            refreshButtonState();
        }
    }

    // ===== IStatusObserver 回调 =====
    @Override
    public void onGoldUpdated(int gold) {
        goldLabel.setText("金币 " + gold);
    }

    @Override
    public void onLivesUpdated(int lives) {
        // 漏怪扣生命提示音（《音效扩展方法说明》§二：交互侧在"生命减少"的回调处调用）。
        // 只在下降时触发：初始推送 / resetGame / switchLevel 都是重置为初值（只会升高或持平），不会误响
        if (lastLives >= 0 && lives < lastLives) {
            SoundManager.getInstance().onLifeLost();
        }
        lastLives = lives;
        livesLabel.setText("生命 " + lives);
    }

    @Override
    public void onWaveMessage(String message) {
        msgLabel.setText(message);
        refreshWaveLabel();
        refreshButtonState();
    }
}
