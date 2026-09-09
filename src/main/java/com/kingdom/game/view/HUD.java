package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.IStatusObserver;
import com.kingdom.game.controller.IWaveStarter;
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
 */
public class HUD implements IStatusObserver {

    private final Label livesLabel = new Label();
    private final Label goldLabel = new Label();
    private final Label waveLabel = new Label();
    private final Label msgLabel = new Label();
    private final Label countdownLabel = new Label();
    private final Button startButton = new Button("开始波次");

    private final IWaveStarter waveStarter;
    private final IGameStateReader stateReader;

    private final HBox bar;
    private final AnimationTimer countdownTimer;
    private String lastCountdownText = "";
    private String lastButtonText = "开始波次";

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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar = new HBox(18, livesLabel, goldLabel, waveLabel, msgLabel, spacer, countdownLabel, startButton);
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

    private void refreshWaveLabel() {
        waveLabel.setText("波次 " + stateReader.getCurrentWave() + " / " + stateReader.getTotalWaves());
        waveLabel.setTextFill(Color.WHITE);
    }

    private void refreshButtonState() {
        startButton.setDisable(!waveStarter.canStartNextWave());
    }

    /** 每帧同步：倒计时文字 / 按钮文案与可用态（仅在变化时刷新，避免无谓 setText） */
    private void hudTick() {
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
        livesLabel.setText("生命 " + lives);
    }

    @Override
    public void onWaveMessage(String message) {
        msgLabel.setText(message);
        refreshWaveLabel();
        refreshButtonState();
    }
}
