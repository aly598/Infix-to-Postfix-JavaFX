package com.example.infix_to_postfix;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.Stack;

public class infix_to_postfix extends Application {

    // ─── ocean theme palette ─────────────────────────────────────────────────
    private static final String DEEP_BG    = "#0b1622";
    private static final String PANEL_BG   = "#132238";
    private static final String TILE_BG    = "#1b3050";
    private static final String CYAN_ACC   = "#00c8e8";
    private static final String GOLD_ACC   = "#f0a830";
    private static final String LIGHT_TXT  = "#d8e8f4";
    private static final String DIM_TXT    = "#5888a0";
    private static final String ERR_CLR    = "#f06060";
    private static final String OK_CLR     = "#50e898";

    @Override
    public void start(Stage window) {

        // ── banner ──────────────────────────────────────────────────────────
        Label banner = makeLabel("⚙  Expression Solver", 24, FontWeight.EXTRA_BOLD, CYAN_ACC);
        Label tagline = makeLabel("infix  ➜  postfix  ➜  answer", 11, FontWeight.NORMAL, DIM_TXT);

        HBox topBar = new HBox(14, banner, tagline);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 0, 6, 0));

        // ── left panel : input area ─────────────────────────────────────────
        Label prompt = makeLabel("Type your expression below", 13, FontWeight.SEMI_BOLD, LIGHT_TXT);

        TextField exprField = new TextField();
        exprField.setPromptText("example:  ( 8 + 2 ) * 3 - 1");
        applyFieldStyle(exprField);

        Button solveBtn = buildButton("▶  Solve", CYAN_ACC, DEEP_BG);
        Button resetBtn = buildButton("↻  Reset", TILE_BG, DIM_TXT);

        VBox btnColumn = new VBox(8, solveBtn, resetBtn);
        btnColumn.setAlignment(Pos.CENTER);

        Label errMsg = makeLabel("", 11, FontWeight.NORMAL, ERR_CLR);
        errMsg.setWrapText(true);
        errMsg.setMaxWidth(230);

        VBox leftPanel = new VBox(14, prompt, exprField, btnColumn, errMsg);
        leftPanel.setPadding(new Insets(18));
        leftPanel.setPrefWidth(270);
        leftPanel.setStyle(
                "-fx-background-color:" + PANEL_BG + ";"
                        + "-fx-background-radius:16;"
                        + "-fx-border-color:" + DIM_TXT + "22;"
                        + "-fx-border-radius:16;");

        // ── right panel : results area ──────────────────────────────────────
        Label pfxHeader = makeLabel("◆  Postfix Form", 12, FontWeight.BOLD, GOLD_ACC);
        Label pfxValue  = makeLabel("...", 15, FontWeight.BOLD, CYAN_ACC);
        pfxValue.setWrapText(true);
        VBox pfxTile = buildTile(pfxHeader, pfxValue);

        Label ansHeader = makeLabel("★  Answer", 12, FontWeight.BOLD, GOLD_ACC);
        Label ansValue  = makeLabel("...", 30, FontWeight.EXTRA_BOLD, OK_CLR);
        VBox ansTile = buildTile(ansHeader, ansValue);

        VBox rightPanel = new VBox(14, pfxTile, ansTile);
        rightPanel.setPadding(new Insets(18));
        rightPanel.setPrefWidth(270);
        rightPanel.setStyle(
                "-fx-background-color:" + PANEL_BG + ";"
                        + "-fx-background-radius:16;"
                        + "-fx-border-color:" + DIM_TXT + "22;"
                        + "-fx-border-radius:16;");

        // ── middle section : left + right side by side ──────────────────────
        HBox midSection = new HBox(16, leftPanel, rightPanel);
        HBox.setHgrow(leftPanel,  Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // ── bottom panel : trace / steps ────────────────────────────────────
        Label traceHeader = makeLabel("📋  Conversion Trace", 12, FontWeight.BOLD, GOLD_ACC);

        TextArea traceArea = new TextArea();
        traceArea.setEditable(false);
        traceArea.setPrefRowCount(20);
        applyTraceStyle(traceArea);

        VBox bottomPanel = new VBox(8, traceHeader, traceArea);
        bottomPanel.setPadding(new Insets(18));
        bottomPanel.setStyle(
                "-fx-background-color:" + PANEL_BG + ";"
                        + "-fx-background-radius:16;"
                        + "-fx-border-color:" + DIM_TXT + "22;"
                        + "-fx-border-radius:16;");

        // ── divider ─────────────────────────────────────────────────────────
        Separator div = new Separator();
        div.setStyle("-fx-background-color:" + DIM_TXT + "33;");

        // ── root layout ────────────────────────────────────────────────────
        VBox root = new VBox(16, topBar, div, midSection, bottomPanel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:" + DEEP_BG + ";");

        // ── wiring : solve button ──────────────────────────────────────────
        solveBtn.setOnAction(ev -> {
            errMsg.setText("");
            String raw = exprField.getText().trim();
            if (raw.isEmpty()) {
                errMsg.setText("⚠ Enter an expression first!");
                return;
            }
            try {
                StringBuilder log = new StringBuilder();
                String pfx    = convertToPostfix(raw, log);
                double answer = computePostfix(pfx);

                pfxValue.setText(pfx);
                ansValue.setText(formatAnswer(answer));
                traceArea.setText(log.toString());
            } catch (Exception ex) {
                errMsg.setText("⚠ " + ex.getMessage());
                pfxValue.setText("...");
                ansValue.setText("...");
                traceArea.clear();
            }
        });

        // ── wiring : reset button ──────────────────────────────────────────
        resetBtn.setOnAction(ev -> {
            exprField.clear();
            pfxValue.setText("...");
            ansValue.setText("...");
            traceArea.clear();
            errMsg.setText("");
        });

        // ── enter shortcut ─────────────────────────────────────────────────
        exprField.setOnAction(ev -> solveBtn.fire());

        // ── stage setup ────────────────────────────────────────────────────
        window.setScene(new Scene(root));
        window.setTitle("Expression Solver");
        window.setResizable(false);
        window.show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CORE ALGORITHM 1 — Infix ➜ Postfix  (Shunting-yard)
    // ═════════════════════════════════════════════════════════════════════════
    private String convertToPostfix(String expr, StringBuilder log) {
        StringBuilder out  = new StringBuilder();
        Stack<Character> stk = new Stack<>();
        String[] parts = splitTokens(expr);

        log.append(String.format("%-18s %-28s %s%n", "TOKEN", "STACK", "OUTPUT"));
        log.append("═".repeat(66)).append("\n");

        for (String p : parts) {
            if (p.isEmpty()) continue;

            if (isNumeric(p)) {
                out.append(p).append(" ");
            } else if (p.equals("(")) {
                stk.push('(');
            } else if (p.equals(")")) {
                while (!stk.isEmpty() && stk.peek() != '(')
                    out.append(stk.pop()).append(" ");
                if (stk.isEmpty()) throw new IllegalArgumentException("Unbalanced parentheses.");
                stk.pop();
            } else if (isOp(p.charAt(0))) {
                while (!stk.isEmpty() && stk.peek() != '('
                        && priority(stk.peek()) >= priority(p.charAt(0)))
                    out.append(stk.pop()).append(" ");
                stk.push(p.charAt(0));
            } else {
                throw new IllegalArgumentException("Unrecognised token: " + p);
            }

            log.append(String.format("%-18s %-28s %s%n",
                    p, stk.toString(), out.toString().trim()));
        }

        while (!stk.isEmpty()) {
            char c = stk.pop();
            if (c == '(' || c == ')') throw new IllegalArgumentException("Unbalanced parentheses.");
            out.append(c).append(" ");
        }

        log.append("═".repeat(66)).append("\n");
        log.append("POSTFIX ➜ ").append(out.toString().trim());
        return out.toString().trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CORE ALGORITHM 2 — Evaluate Postfix
    // ═════════════════════════════════════════════════════════════════════════
    private double computePostfix(String postfix) {
        Stack<Double> nums = new Stack<>();
        for (String p : postfix.split("\\s+")) {
            if (p.isEmpty()) continue;
            if (isNumeric(p)) {
                nums.push(Double.parseDouble(p));
            } else if (isOp(p.charAt(0)) && p.length() == 1) {
                if (nums.size() < 2) throw new IllegalArgumentException("Malformed expression.");
                double right = nums.pop(), left = nums.pop();
                switch (p.charAt(0)) {
                    case '+' -> nums.push(left + right);
                    case '-' -> nums.push(left - right);
                    case '*' -> nums.push(left * right);
                    case '/' -> { if (right == 0) throw new ArithmeticException("Cannot divide by zero."); nums.push(left / right); }
                    case '^' -> nums.push(Math.pow(left, right));
                }
            }
        }
        if (nums.size() != 1) throw new IllegalArgumentException("Malformed expression.");
        return nums.pop();
    }

    // ── utility methods ────────────────────────────────────────────────────
    private int     priority(char op)   { return switch(op){ case '+','-'->1; case '*','/'->2; case '^'->3; default->0; }; }
    private boolean isOp(char c)        { return "+-*/^".indexOf(c) >= 0; }
    private boolean isNumeric(String s) { try { Double.parseDouble(s); return true; } catch (Exception e) { return false; } }

    private String formatAnswer(double v) {
        return (v == Math.floor(v) && !Double.isInfinite(v))
                ? String.valueOf((long) v)
                : String.format("%.4f", v);
    }

    private String[] splitTokens(String expr) {
        return expr.replaceAll("([+\\-*/^()])", " $1 ").trim().split("\\s+");
    }

    // ── UI builder helpers ─────────────────────────────────────────────────
    private Label makeLabel(String text, int size, FontWeight weight, String hex) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", weight, size));
        lbl.setTextFill(Color.web(hex));
        return lbl;
    }

    private void applyFieldStyle(TextField f) {
        f.setStyle(
                "-fx-background-color:" + TILE_BG + ";"
                        + "-fx-text-fill:" + LIGHT_TXT + ";"
                        + "-fx-prompt-text-fill:" + DIM_TXT + ";"
                        + "-fx-border-color:" + CYAN_ACC + "55;"
                        + "-fx-border-radius:12;"
                        + "-fx-background-radius:12;"
                        + "-fx-padding:12 16;"
                        + "-fx-font-size:14;");
    }

    private void applyTraceStyle(TextArea a) {
        a.setStyle(
                "-fx-control-inner-background:" + TILE_BG + ";"
                        + "-fx-text-fill:" + LIGHT_TXT + ";"
                        + "-fx-border-color:" + DIM_TXT + "33;"
                        + "-fx-border-radius:12;"
                        + "-fx-background-radius:12;"
                        + "-fx-font-family:monospace;"
                        + "-fx-font-size:11;");
    }

    private Button buildButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("System", FontWeight.BOLD, 13));
        btn.setStyle(
                "-fx-background-color:" + bg + ";"
                        + "-fx-text-fill:" + fg + ";"
                        + "-fx-background-radius:20;"
                        + "-fx-padding:10 28;"
                        + "-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private VBox buildTile(Label header, Label value) {
        VBox tile = new VBox(8, header, value);
        tile.setPadding(new Insets(16));
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.setStyle(
                "-fx-background-color:" + TILE_BG + ";"
                        + "-fx-background-radius:14;"
                        + "-fx-border-color:" + CYAN_ACC + "28;"
                        + "-fx-border-radius:14;");
        VBox.setVgrow(tile, Priority.ALWAYS);
        return tile;
    }

    public static void main(String[] args) { launch(args); }
}