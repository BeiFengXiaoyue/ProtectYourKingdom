package com.kingdom.game.util.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniJson —— 极简 JSON 解析器（仅支持对象/数组/字符串/数字/布尔/null 子集）。
 *
 * 原为 MapRoute 的包私有内部类；为按功能拆包后仍能被 map/anim 各包复用，
 * 抽出为本包公共类。项目不引第三方 JSON 库，统一用它读写配置 JSON。
 *
 * 用法：{@code Object root = new MiniJson(json).parse();}
 * 返回结构为 Map&lt;String,Object&gt; / List&lt;Object&gt; / String / Double / Boolean / null。
 */
public final class MiniJson {

    private final String s;
    private int pos;

    public MiniJson(String s) {
        this.s = s;
    }

    public Object parse() {
        skipWs();
        Object v = parseValue();
        skipWs();
        if (pos < s.length()) {
            throw new IllegalArgumentException("JSON 尾部有多余内容: " + s.substring(pos));
        }
        return v;
    }

    private Object parseValue() {
        skipWs();
        if (pos >= s.length()) throw new IllegalArgumentException("JSON 意外结束");
        char c = s.charAt(pos);
        switch (c) {
            case '{': return parseObject();
            case '[': return parseArray();
            case '"': return parseString();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
                throw new IllegalArgumentException("无法识别的字符: " + c);
        }
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // '{'
        skipWs();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            if (peek() != '"') throw new IllegalArgumentException("对象的 key 必须是字符串");
            String key = parseString();
            skipWs();
            if (peek() != ':') throw new IllegalArgumentException("缺少 ':'");
            pos++;
            Object value = parseValue();
            map.put(key, value);
            skipWs();
            char c = s.charAt(pos++);
            if (c == ',') continue;
            if (c == '}') break;
            throw new IllegalArgumentException("对象内应为 ',' 或 '}'");
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++; // '['
        skipWs();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            list.add(parseValue());
            skipWs();
            char c = s.charAt(pos++);
            if (c == ',') continue;
            if (c == ']') break;
            throw new IllegalArgumentException("数组内应为 ',' 或 ']'");
        }
        return list;
    }

    private String parseString() {
        pos++; // '"'
        StringBuilder sb = new StringBuilder();
        while (pos < s.length()) {
            char c = s.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= s.length()) break;
                char e = s.charAt(pos++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u': {
                        if (pos + 4 <= s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        break;
                    }
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("字符串未闭合");
    }

    private Double parseNumber() {
        int start = pos;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                pos++;
            } else {
                break;
            }
        }
        return Double.parseDouble(s.substring(start, pos));
    }

    private void expect(String token) {
        if (!s.startsWith(token, pos)) {
            throw new IllegalArgumentException("期望 " + token + " 实际: " + s.substring(pos));
        }
        pos += token.length();
    }

    private char peek() {
        skipWs();
        if (pos >= s.length()) throw new IllegalArgumentException("JSON 意外结束");
        return s.charAt(pos);
    }

    private void skipWs() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
            else break;
        }
    }
}
