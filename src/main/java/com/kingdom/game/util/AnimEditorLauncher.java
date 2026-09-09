package com.kingdom.game.util;

import javafx.application.Application;

/**
 * AnimEditorLauncher —— 敌人动画编辑器的可选启动器（普通类，不继承 Application）。
 * 直接运行 AnimEditorTool 亦可（其 main 已转发到嵌套 Application）。
 */
public final class AnimEditorLauncher {

    public static void main(String[] args) {
        Application.launch(AnimEditorTool.AnimEditorApp.class, args);
    }
}
