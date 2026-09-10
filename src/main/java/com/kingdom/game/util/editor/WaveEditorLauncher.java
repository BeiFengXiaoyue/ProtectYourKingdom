package com.kingdom.game.util.editor;

import javafx.application.Application;

/**
 * WaveEditorLauncher —— 波次编辑器的可选启动器（普通类，不继承 Application）。
 *
 * 其实直接运行 WaveEditorTool 也可以（其 main 已转发到嵌套的 Application）；
 * 本启动器等价，只是把“入口”再独立一层，便于部分 IDE 习惯。
 *
 * 运行方式：IDE 运行 com.kingdom.game.util.editor.WaveEditorLauncher。
 */
public final class WaveEditorLauncher {

    public static void main(String[] args) {
        Application.launch(WaveEditorTool.WaveEditorApp.class, args);
    }
}
