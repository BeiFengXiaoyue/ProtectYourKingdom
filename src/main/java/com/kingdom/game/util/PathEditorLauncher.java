package com.kingdom.game.util;

import javafx.application.Application;

/**
 * PathEditorLauncher —— 路径标注工具的可选启动器（普通类，不继承 Application）。
 *
 * 其实直接运行 PathEditorTool 也可以（其 main 已转发到嵌套的 Application）；
 * 本启动器等价，只是把“入口”再独立一层，便于部分 IDE 习惯。
 *
 * 运行方式：IDE 运行 com.kingdom.game.util.PathEditorLauncher。
 */
public final class PathEditorLauncher {

    public static void main(String[] args) {
        Application.launch(PathEditorTool.PathEditorToolApp.class, args);
    }
}
