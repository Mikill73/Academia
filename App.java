cat > src/main/java/com/academia/app/MainActivity.java << 'EOF'
package com.academia.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.text.InputType;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Vibrator;
import java.io.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout mainLayout;
    private LinearLayout treinoHojePanel;
    private TextView treinoHojeNome;
    private Button btnIniciarTreino;
    private LinearLayout cardTreinoPanel;
    private LinearLayout exerciciosContainer;
    private LinearLayout dadosContainer;
    private LinearLayout timerPanel;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView timerLabel;
    private TextView cardTitle;
    private Button configBtn;
    private boolean modoConfig = false;
    private boolean isActive = false;
    private boolean aguardandoTimer = false;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private int timerRestante = 0;
    private JSONObject configData;
    private JSONObject treinoAtual;
    private int exercicioAtualIndex = 0;
    private int serieAtualIndex = 0;
    private static final String ARQUIVO_DADOS = "academia_dados.json";
    private String[] DIAS_SEMANA = {"Segunda", "Terca", "Quarta", "Quinta", "Sexta", "Sabado", "Domingo"};
    private Context context;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        verificarPermissoes();
        carregarDados();
        setupUI();
        carregarEstadoBotao();
        atualizarTreinoHoje();
        renderDados();
    }

    private void verificarPermissoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    private void setupUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0d0d0d"));
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        LinearLayout topPanel = new LinearLayout(this);
        topPanel.setOrientation(LinearLayout.HORIZONTAL);
        topPanel.setGravity(android.view.Gravity.CENTER_VERTICAL);
        topPanel.setPadding(0, 0, 0, dpToPx(16));

        TextView appTitle = new TextView(this);
        appTitle.setText("Academia");
        appTitle.setTextColor(Color.parseColor("#ffffff"));
        appTitle.setTextSize(22);
        appTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        appTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topPanel.addView(appTitle);

        configBtn = new Button(this);
        configBtn.setText("⚙");
        configBtn.setBackground(null);
        configBtn.setTextColor(Color.parseColor("#888888"));
        configBtn.setTextSize(20);
        configBtn.setOnClickListener(v -> {
            modoConfig = !modoConfig;
            renderDados();
        });
        topPanel.addView(configBtn);

        mainLayout.addView(topPanel);

        treinoHojePanel = new LinearLayout(this);
        treinoHojePanel.setOrientation(LinearLayout.VERTICAL);
        treinoHojePanel.setBackgroundColor(Color.parseColor("#1a1a1a"));
        treinoHojePanel.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        GradientDrawable border = new GradientDrawable();
        border.setStroke(1, Color.parseColor("#2a2a2a"));
        border.setColor(Color.parseColor("#1a1a1a"));
        treinoHojePanel.setBackground(border);
        treinoHojePanel.setVisibility(View.VISIBLE);

        TextView hojeLabel = new TextView(this);
        hojeLabel.setText("TREINO DE HOJE");
        hojeLabel.setTextColor(Color.parseColor("#666666"));
        hojeLabel.setTextSize(11);
        hojeLabel.setGravity(android.view.Gravity.CENTER);
        treinoHojePanel.addView(hojeLabel);

        treinoHojeNome = new TextView(this);
        treinoHojeNome.setText("Nenhum treino programado");
        treinoHojeNome.setTextColor(Color.parseColor("#ffffff"));
        treinoHojeNome.setTextSize(18);
        treinoHojeNome.setTypeface(null, android.graphics.Typeface.BOLD);
        treinoHojeNome.setGravity(android.view.Gravity.CENTER);
        treinoHojeNome.setPadding(0, dpToPx(8), 0, dpToPx(8));
        treinoHojePanel.addView(treinoHojeNome);

        btnIniciarTreino = new Button(this);
        btnIniciarTreino.setText("INICIAR TREINO");
        btnIniciarTreino.setBackgroundColor(Color.parseColor("#00cc00"));
        btnIniciarTreino.setTextColor(Color.parseColor("#ffffff"));
        btnIniciarTreino.setTypeface(null, android.graphics.Typeface.BOLD);
        btnIniciarTreino.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12));
        btnIniciarTreino.setEnabled(false);
        btnIniciarTreino.setOnClickListener(v -> toggleTreino());
        treinoHojePanel.addView(btnIniciarTreino);

        mainLayout.addView(treinoHojePanel);

        cardTreinoPanel = new LinearLayout(this);
        cardTreinoPanel.setOrientation(LinearLayout.VERTICAL);
        cardTreinoPanel.setBackgroundColor(Color.parseColor("#0a0a0a"));
        cardTreinoPanel.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        cardTreinoPanel.setVisibility(View.GONE);
        cardTreinoPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout treinoHeader = new LinearLayout(this);
        treinoHeader.setOrientation(LinearLayout.HORIZONTAL);
        treinoHeader.setPadding(0, 0, 0, dpToPx(16));

        cardTitle = new TextView(this);
        cardTitle.setText("TREINO EM ANDAMENTO");
        cardTitle.setTextColor(Color.parseColor("#8bc34a"));
        cardTitle.setTextSize(16);
        cardTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        cardTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        treinoHeader.addView(cardTitle);

        Button btnParar = new Button(this);
        btnParar.setText("PARAR");
        btnParar.setBackgroundColor(Color.parseColor("#ff3333"));
        btnParar.setTextColor(Color.parseColor("#ffffff"));
        btnParar.setTypeface(null, android.graphics.Typeface.BOLD);
        btnParar.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        btnParar.setOnClickListener(v -> toggleTreino());
        treinoHeader.addView(btnParar);

        cardTreinoPanel.addView(treinoHeader);

        exerciciosContainer = new LinearLayout(this);
        exerciciosContainer.setOrientation(LinearLayout.VERTICAL);
        exerciciosContainer.setPadding(0, dpToPx(8), 0, 0);
        exerciciosContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        cardTreinoPanel.addView(exerciciosContainer);

        timerPanel = new LinearLayout(this);
        timerPanel.setOrientation(LinearLayout.VERTICAL);
        timerPanel.setVisibility(View.GONE);
        cardTreinoPanel.addView(timerPanel);

        LinearLayout progressPanel = new LinearLayout(this);
        progressPanel.setOrientation(LinearLayout.VERTICAL);
        progressPanel.setPadding(0, dpToPx(12), 0, 0);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8bc34a")));
        progressPanel.addView(progressBar);

        progressText = new TextView(this);
        progressText.setText("0/0 concluidos");
        progressText.setTextColor(Color.parseColor("#888888"));
        progressText.setTextSize(12);
        progressText.setGravity(android.view.Gravity.CENTER);
        progressPanel.addView(progressText);

        cardTreinoPanel.addView(progressPanel);

        mainLayout.addView(cardTreinoPanel);

        dadosContainer = new LinearLayout(this);
        dadosContainer.setOrientation(LinearLayout.VERTICAL);
        dadosContainer.setPadding(0, dpToPx(12), 0, 0);

        ScrollView scrollDados = new ScrollView(this);
        scrollDados.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        scrollDados.addView(dadosContainer);
        mainLayout.addView(scrollDados);

        setContentView(mainLayout);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void carregarDados() {
        try {
            File file = new File(getFilesDir(), ARQUIVO_DADOS);
            if (file.exists()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
                String jsonStr = sb.toString();
                if (!jsonStr.isEmpty()) {
                    configData = new JSONObject(jsonStr);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            configData = new JSONObject();
            JSONObject academia = new JSONObject();
            academia.put("inicio", JSONObject.NULL);
            JSONObject peso = new JSONObject();
            peso.put("atual", JSONObject.NULL);
            peso.put("historico", new JSONArray());
            peso.put("meta", JSONObject.NULL);
            peso.put("intervalo", 7);
            peso.put("ultimoRegistro", JSONObject.NULL);
            academia.put("peso", peso);
            academia.put("diasDescanso", new JSONArray());
            academia.put("objetivos", new JSONArray());
            JSONObject roupas = new JSONObject();
            roupas.put("camisas", new JSONArray());
            roupas.put("calcas", new JSONArray());
            roupas.put("tenis", new JSONArray());
            academia.put("roupas", roupas);
            academia.put("combinacoes", new JSONObject());
            academia.put("treinos", new JSONArray());
            academia.put("treinoConcluido", new JSONObject());
            academia.put("botaoAtivo", false);
            configData.put("academia", academia);
            salvarDados();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void salvarDados() {
        try {
            File file = new File(getFilesDir(), ARQUIVO_DADOS);
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(configData.toString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTodayName() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("pt", "BR"));
        String dia = sdf.format(new Date());
        return dia.substring(0, 1).toUpperCase() + dia.substring(1).toLowerCase();
    }

    private String getTodayKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(new Date());
    }

    private JSONArray getTodayTreinos() {
        try {
            String hoje = getTodayName();
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONArray result = new JSONArray();
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject treino = treinos.getJSONObject(i);
                String diaTreino = treino.getString("dia").trim();
                if (diaTreino.equalsIgnoreCase(hoje)) {
                    result.put(treino);
                }
            }
            return result;
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void atualizarTreinoHoje() {
        runOnUiThread(() -> {
            JSONArray treinos = getTodayTreinos();
            if (treinos.length() > 0) {
                try {
                    JSONObject treino = treinos.getJSONObject(0);
                    treinoHojeNome.setText(treino.getString("nome"));
                    btnIniciarTreino.setEnabled(true);
                    btnIniciarTreino.setBackgroundColor(Color.parseColor("#00cc00"));
                    btnIniciarTreino.setText("INICIAR TREINO");
                } catch (JSONException e) {
                    treinoHojeNome.setText("Erro ao carregar");
                    btnIniciarTreino.setEnabled(false);
                }
            } else {
                treinoHojeNome.setText("Nenhum treino programado");
                btnIniciarTreino.setEnabled(false);
                btnIniciarTreino.setBackgroundColor(Color.parseColor("#444444"));
            }
        });
    }

    private boolean verificarPeso() {
        try {
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return false;
            String ultimoStr = peso.getString("ultimoRegistro");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date ultimo = sdf.parse(ultimoStr);
            long diff = System.currentTimeMillis() - ultimo.getTime();
            long diffDays = diff / (24 * 60 * 60 * 1000);
            int intervalo = peso.getInt("intervalo");
            return diffDays >= intervalo;
        } catch (Exception e) {
            return false;
        }
    }

    private int getDiasDesdePesagem() {
        try {
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return 999;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date ultimo = sdf.parse(peso.getString("ultimoRegistro"));
            long diff = System.currentTimeMillis() - ultimo.getTime();
            return (int) (diff / (24 * 60 * 60 * 1000));
        } catch (Exception e) {
            return 999;
        }
    }

    private String getUltimaPesagemData() {
        try {
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return "Nunca";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date data = parser.parse(peso.getString("ultimoRegistro"));
            return sdf.format(data);
        } catch (Exception e) {
            return "Nunca";
        }
    }

    private int getDiasFrequentados() {
        try {
            JSONObject academia = configData.getJSONObject("academia");
            if (academia.isNull("inicio")) return 0;
            String inicioStr = academia.getString("inicio");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date inicio = sdf.parse(inicioStr);
            long diff = System.currentTimeMillis() - inicio.getTime();
            return (int) (diff / (24 * 60 * 60 * 1000));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDataBR(String dataStr) {
        if (dataStr == null || dataStr.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd/MM/yyyy");
            Date date = sdf.parse(dataStr);
            return sdfOut.format(date);
        } catch (Exception e) {
            return dataStr;
        }
    }

    private void mostrarConfirmacao(String titulo, String msg, Runnable onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);
        builder.setMessage(msg);
        builder.setPositiveButton("Sim", (dialog, which) -> {
            if (onConfirm != null) onConfirm.run();
        });
        builder.setNegativeButton("Nao", null);
        builder.show();
    }

    private void mostrarConfirmacaoUnico(String titulo, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void mostrarAvisoPeso() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hora de Pesar!");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Peso atual (kg)");
        builder.setView(input);
        builder.setMessage("Ja faz " + getDiasDesdePesagem() + " dias desde a ultima pesagem (" + getUltimaPesagemData() + "). Registre seu novo peso.");
        builder.setPositiveButton("Registrar", (dialog, which) -> {
            try {
                double val = Double.parseDouble(input.getText().toString());
                if (val <= 0) throw new NumberFormatException();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                String hoje = sdf.format(new Date());
                JSONArray historico = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
                JSONObject novo = new JSONObject();
                novo.put("peso", val);
                novo.put("data", hoje);
                historico.put(novo);
                configData.getJSONObject("academia").getJSONObject("peso").put("atual", val);
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                configData.getJSONObject("academia").getJSONObject("peso").put("ultimoRegistro", sdf2.format(new Date()));
                salvarDados();
                renderDados();
                if (treinoAtual != null) renderTreinoCard();
            } catch (Exception ex) {
                Toast.makeText(this, "Peso invalido.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void limparTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler = null;
            timerRunnable = null;
        }
        timerRestante = 0;
        aguardandoTimer = false;
        timerPanel.setVisibility(View.GONE);
    }

    private void iniciarTimer(int segundos, Runnable callback) {
        limparTimer();
        timerRestante = segundos;
        aguardandoTimer = true;
        timerPanel.setVisibility(View.VISIBLE);
        timerPanel.removeAllViews();

        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setBackgroundColor(Color.parseColor("#0d0d0d"));
        display.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        GradientDrawable border = new GradientDrawable();
        border.setStroke(2, Color.parseColor("#ffaa00"));
        border.setColor(Color.parseColor("#0d0d0d"));
        display.setBackground(border);

        TextView label = new TextView(this);
        label.setText("DESCANSANDO");
        label.setTextColor(Color.parseColor("#ffaa00"));
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setGravity(android.view.Gravity.CENTER);
        display.addView(label);

        timerLabel = new TextView(this);
        timerLabel.setText(String.format("%02d:%02d", segundos/60, segundos%60));
        timerLabel.setTextColor(Color.parseColor("#8bc34a"));
        timerLabel.setTextSize(36);
        timerLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        timerLabel.setGravity(android.view.Gravity.CENTER);
        display.addView(timerLabel);

        timerPanel.addView(display);

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timerRestante--;
                if (timerRestante <= 0) {
                    limparTimer();
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(1500);
                    }
                    if (callback != null) callback.run();
                } else {
                    timerLabel.setText(String.format("%02d:%02d", timerRestante/60, timerRestante%60));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void renderTreinoCard() {
        if (treinoAtual == null) {
            exerciciosContainer.removeAllViews();
            return;
        }

        try {
            JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
            if (exercicios.length() == 0) {
                exerciciosContainer.removeAllViews();
                TextView empty = new TextView(this);
                empty.setText("Nenhum exercicio definido.");
                empty.setTextColor(Color.parseColor("#666666"));
                empty.setGravity(android.view.Gravity.CENTER);
                empty.setPadding(0, dpToPx(20), 0, dpToPx(20));
                exerciciosContainer.addView(empty);
                return;
            }

            if (exercicioAtualIndex >= exercicios.length()) {
                exerciciosContainer.removeAllViews();
                TextView done = new TextView(this);
                done.setText("TREINO CONCLUIDO!");
                done.setTextColor(Color.parseColor("#8bc34a"));
                done.setTextSize(20);
                done.setTypeface(null, android.graphics.Typeface.BOLD);
                done.setGravity(android.view.Gravity.CENTER);
                done.setPadding(0, dpToPx(40), 0, dpToPx(40));
                exerciciosContainer.addView(done);
                return;
            }

            JSONObject ex = exercicios.getJSONObject(exercicioAtualIndex);
            JSONArray series = ex.has("series") ? ex.getJSONArray("series") : new JSONArray();
            
            if (series.length() == 0) {
                exerciciosContainer.removeAllViews();
                TextView empty = new TextView(this);
                empty.setText("Nenhuma serie definida.");
                empty.setTextColor(Color.parseColor("#666666"));
                empty.setGravity(android.view.Gravity.CENTER);
                empty.setPadding(0, dpToPx(20), 0, dpToPx(20));
                exerciciosContainer.addView(empty);
                return;
            }

            if (serieAtualIndex >= series.length()) {
                serieAtualIndex = 0;
                exercicioAtualIndex++;
                renderTreinoCard();
                return;
            }

            JSONObject serie = series.getJSONObject(serieAtualIndex);
            int seriesFeitas = serie.has("_feita") && serie.getBoolean("_feita") ? 1 : 0;
            boolean isDone = seriesFeitas >= 1;

            exerciciosContainer.removeAllViews();

            TextView exercicioNome = new TextView(this);
            exercicioNome.setText(ex.getString("exercise"));
            exercicioNome.setTextColor(Color.parseColor("#ffffff"));
            exercicioNome.setTextSize(20);
            exercicioNome.setTypeface(null, android.graphics.Typeface.BOLD);
            exercicioNome.setGravity(android.view.Gravity.CENTER);
            exerciciosContainer.addView(exercicioNome);

            TextView serieInfo = new TextView(this);
            String warmupText = serie.has("warmup") && serie.getBoolean("warmup") ? " [AQUECIMENTO]" : "";
            serieInfo.setText("Serie " + (serieAtualIndex + 1) + "/" + series.length() + warmupText);
            serieInfo.setTextColor(Color.parseColor("#aaaaaa"));
            serieInfo.setTextSize(14);
            serieInfo.setGravity(android.view.Gravity.CENTER);
            exerciciosContainer.addView(serieInfo);

            View separator = new View(this);
            separator.setBackgroundColor(Color.parseColor("#2a2a2a"));
            separator.setMinimumHeight(1);
            separator.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            separator.setPadding(0, dpToPx(12), 0, dpToPx(12));
            exerciciosContainer.addView(separator);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#1a1a1a"));
            card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
            GradientDrawable border = new GradientDrawable();
            border.setStroke(1, Color.parseColor("#2a2a2a"));
            border.setColor(Color.parseColor("#1a1a1a"));
            card.setBackground(border);

            TextView detalhes = new TextView(this);
            detalhes.setText(serie.getInt("reps") + " repeticoes");
            detalhes.setTextColor(Color.parseColor("#eeeeee"));
            detalhes.setTextSize(18);
            detalhes.setGravity(android.view.Gravity.CENTER);
            card.addView(detalhes);

            TextView loadLabel = new TextView(this);
            loadLabel.setText(serie.getDouble("load") + " kg");
            loadLabel.setTextColor(Color.parseColor("#8bc34a"));
            loadLabel.setTextSize(16);
            loadLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            loadLabel.setGravity(android.view.Gravity.CENTER);
            card.addView(loadLabel);

            if (serie.has("metaCarga") && !serie.isNull("metaCarga")) {
                TextView metaLabel = new TextView(this);
                metaLabel.setText("Meta: " + serie.getDouble("metaCarga") + " kg");
                metaLabel.setTextColor(Color.parseColor("#ffaa00"));
                metaLabel.setTextSize(14);
                metaLabel.setGravity(android.view.Gravity.CENTER);
                card.addView(metaLabel);
            }

            if (!isDone && !aguardandoTimer) {
                Button btnPronto = new Button(this);
                btnPronto.setText("PRONTO");
                btnPronto.setBackgroundColor(Color.parseColor("#1a3a1a"));
                btnPronto.setTextColor(Color.parseColor("#8bc34a"));
                btnPronto.setTypeface(null, android.graphics.Typeface.BOLD);
                btnPronto.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12));
                btnPronto.setOnClickListener(v -> {
                    if (aguardandoTimer) return;
                    try {
                        JSONObject serieAtual = series.getJSONObject(serieAtualIndex);
                        serieAtual.put("_feita", true);

                        boolean todasFeitas = true;
                        for (int i = 0; i < series.length(); i++) {
                            if (!series.getJSONObject(i).has("_feita") || !series.getJSONObject(i).getBoolean("_feita")) {
                                todasFeitas = false;
                                break;
                            }
                        }

                        if (todasFeitas) {
                            if (!serieAtual.has("warmup") || !serieAtual.getBoolean("warmup")) {
                                mostrarEvolucaoDialog(exercicioAtualIndex, serieAtualIndex);
                            } else {
                                salvarProgressoEAtualizar(exercicioAtualIndex);
                            }
                        } else {
                            int descanso = serieAtual.has("descanso") && !serieAtual.isNull("descanso") ? serieAtual.getInt("descanso") : 0;
                            if (descanso > 0) {
                                aguardandoTimer = true;
                                renderTreinoCard();
                                iniciarTimer(descanso, () -> {
                                    aguardandoTimer = false;
                                    serieAtualIndex++;
                                    renderTreinoCard();
                                });
                            } else {
                                serieAtualIndex++;
                                renderTreinoCard();
                            }
                            salvarDados();
                        }
                    } catch (JSONException ex2) {
                        ex2.printStackTrace();
                    }
                });
                card.addView(btnPronto);
            } else if (isDone) {
                TextView doneLabel = new TextView(this);
                doneLabel.setText("CONCLUIDO");
                doneLabel.setTextColor(Color.parseColor("#8bc34a"));
                doneLabel.setTextSize(16);
                doneLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                doneLabel.setGravity(android.view.Gravity.CENTER);
                card.addView(doneLabel);
            } else if (aguardandoTimer) {
                TextView waitLabel = new TextView(this);
                waitLabel.setText("Aguardando descanso...");
                waitLabel.setTextColor(Color.parseColor("#ffaa00"));
                waitLabel.setTextSize(14);
                waitLabel.setGravity(android.view.Gravity.CENTER);
                card.addView(waitLabel);
            }

            exerciciosContainer.addView(card);

            int totalSeries = series.length();
            int doneSeries = 0;
            for (int i = 0; i < series.length(); i++) {
                if (series.getJSONObject(i).has("_feita") && series.getJSONObject(i).getBoolean("_feita")) {
                    doneSeries++;
                }
            }
            int total = exercicios.length();
            int done = 0;
            for (int i = 0; i < total; i++) {
                JSONObject e = exercicios.getJSONObject(i);
                if (e.has("series")) {
                    JSONArray s = e.getJSONArray("series");
                    boolean allDone = true;
                    for (int j = 0; j < s.length(); j++) {
                        if (!s.getJSONObject(j).has("_feita") || !s.getJSONObject(j).getBoolean("_feita")) {
                            allDone = false;
                            break;
                        }
                    }
                    if (allDone) done++;
                }
            }
            int pct = total > 0 ? (done * 100) / total : 0;
            progressBar.setProgress(pct);
            progressText.setText(done + "/" + total + " exercicios concluidos");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void mostrarEvolucaoDialog(int exIdx, int serieIdx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Evolucao de Carga");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        try {
            JSONObject ex = treinoAtual.getJSONArray("exercicios").getJSONObject(exIdx);
            JSONArray series = ex.getJSONArray("series");
            JSONObject serie = series.getJSONObject(serieIdx);
            
            TextView exName = new TextView(this);
            exName.setText("Registre a evolucao para " + ex.getString("exercise"));
            exName.setTextColor(Color.parseColor("#aaaaaa"));
            exName.setTextSize(13);
            layout.addView(exName);

            TextView loadLabel = new TextView(this);
            loadLabel.setText("Carga atual (kg)");
            loadLabel.setTextColor(Color.parseColor("#888888"));
            loadLabel.setTextSize(12);
            layout.addView(loadLabel);

            final EditText loadField = new EditText(this);
            loadField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            loadField.setText(String.valueOf(serie.getDouble("load")));
            loadField.setBackgroundColor(Color.parseColor("#0d0d0d"));
            loadField.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(loadField);

            TextView repsLabel = new TextView(this);
            repsLabel.setText("Repeticoes atuais");
            repsLabel.setTextColor(Color.parseColor("#888888"));
            repsLabel.setTextSize(12);
            layout.addView(repsLabel);

            final EditText repsField = new EditText(this);
            repsField.setInputType(InputType.TYPE_CLASS_NUMBER);
            repsField.setText(String.valueOf(serie.getInt("reps")));
            repsField.setBackgroundColor(Color.parseColor("#0d0d0d"));
            repsField.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(repsField);

            TextView note = new TextView(this);
            note.setText("Deixe em branco se nao houve evolucao");
            note.setTextColor(Color.parseColor("#666666"));
            note.setTextSize(10);
            layout.addView(note);

            builder.setView(layout);

            builder.setPositiveButton("Registrar", (dialog, which) -> {
                try {
                    double load = Double.parseDouble(loadField.getText().toString());
                    int reps = Integer.parseInt(repsField.getText().toString());
                    if (load <= 0 || reps < 1) throw new NumberFormatException();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    String hoje = sdf.format(new Date());
                    JSONArray history;
                    if (serie.has("loadHistory")) {
                        history = serie.getJSONArray("loadHistory");
                    } else {
                        history = new JSONArray();
                        serie.put("loadHistory", history);
                    }
                    JSONObject novo = new JSONObject();
                    novo.put("load", load);
                    novo.put("reps", reps);
                    novo.put("date", hoje);
                    history.put(novo);
                    serie.put("load", load);
                    serie.put("reps", reps);
                    salvarProgressoEAtualizar(exIdx);
                } catch (Exception ex2) {
                    Toast.makeText(this, "Valores invalidos.", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Pular", (dialog, which) -> {
                salvarProgressoEAtualizar(exIdx);
            });

            builder.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void salvarProgressoEAtualizar(int idx) {
        try {
            String hojeKey = getTodayKey();
            configData.getJSONObject("academia").put("treino_" + hojeKey, treinoAtual);
            salvarDados();

            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject t = treinos.getJSONObject(i);
                if (t.getString("nome").equals(treinoAtual.getString("nome")) &&
                    t.getString("dia").equals(treinoAtual.getString("dia"))) {
                    if (t.has("exercicios")) {
                        JSONArray exs = t.getJSONArray("exercicios");
                        JSONObject exOriginal = treinoAtual.getJSONArray("exercicios").getJSONObject(idx);
                        for (int j = 0; j < exs.length(); j++) {
                            JSONObject e = exs.getJSONObject(j);
                            if (e.getString("exercise").equals(exOriginal.getString("exercise"))) {
                                if (exOriginal.has("series")) {
                                    e.put("series", exOriginal.getJSONArray("series"));
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            salvarDados();
            serieAtualIndex = 0;
            exercicioAtualIndex = idx + 1;
            renderTreinoCard();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void iniciarTreino() {
        JSONArray treinos = getTodayTreinos();
        if (treinos.length() == 0) {
            mostrarConfirmacaoUnico("Aviso", "Nenhum treino programado para hoje.");
            return;
        }

        try {
            String hojeKey = getTodayKey();
            JSONObject treinoConcluido = configData.getJSONObject("academia").getJSONObject("treinoConcluido");
            if (treinoConcluido.has(hojeKey) && treinoConcluido.getBoolean(hojeKey)) {
                mostrarConfirmacao("Treino Concluido", "Voce ja concluiu o treino de hoje. Deseja refaze-lo?", () -> {
                    try {
                        treinoConcluido.put(hojeKey, false);
                        salvarDados();
                        iniciarTreinoAtual();
                    } catch (JSONException e) {}
                });
                return;
            }

            if (configData.getJSONObject("academia").has("treino_" + hojeKey) &&
                !configData.getJSONObject("academia").isNull("treino_" + hojeKey)) {
                treinoAtual = configData.getJSONObject("academia").getJSONObject("treino_" + hojeKey);
                exercicioAtualIndex = 0;
                serieAtualIndex = 0;
                if (treinoAtual.has("exercicios")) {
                    JSONArray exs = treinoAtual.getJSONArray("exercicios");
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (e.has("series")) {
                            JSONArray series = e.getJSONArray("series");
                            for (int j = 0; j < series.length(); j++) {
                                JSONObject s = series.getJSONObject(j);
                                if (!s.has("_feita")) s.put("_feita", false);
                                if (!s.has("loadHistory")) s.put("loadHistory", new JSONArray());
                            }
                        }
                    }
                    boolean algumNaoConcluido = false;
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (e.has("series")) {
                            JSONArray series = e.getJSONArray("series");
                            boolean allDone = true;
                            for (int j = 0; j < series.length(); j++) {
                                if (!series.getJSONObject(j).getBoolean("_feita")) {
                                    allDone = false;
                                    break;
                                }
                            }
                            if (!allDone) {
                                exercicioAtualIndex = i;
                                algumNaoConcluido = true;
                                break;
                            }
                        }
                    }
                    if (!algumNaoConcluido) {
                        treinoConcluido.put(hojeKey, true);
                        configData.getJSONObject("academia").put("treino_" + hojeKey, JSONObject.NULL);
                        configData.getJSONObject("academia").put("botaoAtivo", false);
                        salvarDados();
                        isActive = false;
                        treinoAtual = null;
                        atualizarTreinoHoje();
                        return;
                    }
                }
                isActive = true;
                configData.getJSONObject("academia").put("botaoAtivo", true);
                salvarDados();
                treinoHojePanel.setVisibility(View.GONE);
                dadosContainer.setVisibility(View.GONE);
                cardTreinoPanel.setVisibility(View.VISIBLE);
                renderTreinoCard();
                if (verificarPeso()) mostrarAvisoPeso();
                return;
            }

            iniciarTreinoAtual();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void iniciarTreinoAtual() {
        JSONArray treinos = getTodayTreinos();
        if (treinos.length() == 0) return;
        try {
            JSONObject treinoBase = treinos.getJSONObject(0);
            treinoAtual = new JSONObject(treinoBase.toString());
            if (treinoAtual.has("exercicios")) {
                JSONArray exs = treinoAtual.getJSONArray("exercicios");
                for (int i = 0; i < exs.length(); i++) {
                    JSONObject e = exs.getJSONObject(i);
                    if (e.has("series")) {
                        JSONArray series = e.getJSONArray("series");
                        for (int j = 0; j < series.length(); j++) {
                            JSONObject s = series.getJSONObject(j);
                            s.put("_feita", false);
                            if (!s.has("loadHistory")) s.put("loadHistory", new JSONArray());
                        }
                    }
                }
            }
            exercicioAtualIndex = 0;
            serieAtualIndex = 0;
            isActive = true;
            configData.getJSONObject("academia").put("botaoAtivo", true);
            treinoHojePanel.setVisibility(View.GONE);
            dadosContainer.setVisibility(View.GONE);
            cardTreinoPanel.setVisibility(View.VISIBLE);
            renderTreinoCard();

            String hojeKey = getTodayKey();
            configData.getJSONObject("academia").put("treino_" + hojeKey, treinoAtual);
            salvarDados();

            if (verificarPeso()) mostrarAvisoPeso();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void toggleTreino() {
        if (isActive) {
            mostrarConfirmacao("Parar Treino", "Deseja realmente parar o treino?", () -> {
                isActive = false;
                cardTreinoPanel.setVisibility(View.GONE);
                treinoAtual = null;
                limparTimer();
                String hojeKey = getTodayKey();
                try {
                    configData.getJSONObject("academia").put("treino_" + hojeKey, JSONObject.NULL);
                    configData.getJSONObject("academia").put("botaoAtivo", false);
                } catch (JSONException e) {}
                salvarDados();
                atualizarTreinoHoje();
                treinoHojePanel.setVisibility(View.VISIBLE);
                dadosContainer.setVisibility(View.VISIBLE);
                renderDados();
            });
        } else {
            JSONArray treinos = getTodayTreinos();
            if (treinos.length() == 0) {
                mostrarConfirmacaoUnico("Aviso", "Nenhum treino programado para hoje.");
                return;
            }
            iniciarTreino();
        }
    }

    private void carregarEstadoBotao() {
        try {
            String hojeKey = getTodayKey();
            boolean temTreinoSalvo = configData.getJSONObject("academia").has("treino_" + hojeKey) &&
                !configData.getJSONObject("academia").isNull("treino_" + hojeKey);
            JSONObject treinoConcluido = configData.getJSONObject("academia").getJSONObject("treinoConcluido");
            boolean treinoConcluidoHoje = treinoConcluido.has(hojeKey) && treinoConcluido.getBoolean(hojeKey);
            boolean botaoAtivo = configData.getJSONObject("academia").getBoolean("botaoAtivo");

            if (temTreinoSalvo && !treinoConcluidoHoje && botaoAtivo) {
                treinoAtual = configData.getJSONObject("academia").getJSONObject("treino_" + hojeKey);
                if (treinoAtual.has("exercicios")) {
                    JSONArray exs = treinoAtual.getJSONArray("exercicios");
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (e.has("series")) {
                            JSONArray series = e.getJSONArray("series");
                            for (int j = 0; j < series.length(); j++) {
                                JSONObject s = series.getJSONObject(j);
                                if (!s.has("_feita")) s.put("_feita", false);
                                if (!s.has("loadHistory")) s.put("loadHistory", new JSONArray());
                            }
                        }
                    }
                    boolean todosConcluidos = true;
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (e.has("series")) {
                            JSONArray series = e.getJSONArray("series");
                            for (int j = 0; j < series.length(); j++) {
                                if (!series.getJSONObject(j).getBoolean("_feita")) {
                                    todosConcluidos = false;
                                    break;
                                }
                            }
                        }
                        if (!todosConcluidos) break;
                    }
                    if (todosConcluidos) {
                        isActive = false;
                        cardTreinoPanel.setVisibility(View.GONE);
                        treinoAtual = null;
                        limparTimer();
                        treinoConcluido.put(hojeKey, true);
                        configData.getJSONObject("academia").put("botaoAtivo", false);
                        salvarDados();
                        treinoHojePanel.setVisibility(View.VISIBLE);
                        dadosContainer.setVisibility(View.VISIBLE);
                    } else {
                        isActive = true;
                        cardTreinoPanel.setVisibility(View.VISIBLE);
                        treinoHojePanel.setVisibility(View.GONE);
                        dadosContainer.setVisibility(View.GONE);
                        exercicioAtualIndex = 0;
                        serieAtualIndex = 0;
                        renderTreinoCard();
                    }
                    return;
                }
            }

            isActive = false;
            cardTreinoPanel.setVisibility(View.GONE);
            treinoAtual = null;
            limparTimer();
            treinoHojePanel.setVisibility(View.VISIBLE);
            dadosContainer.setVisibility(View.VISIBLE);
            if (configData.getJSONObject("academia").getBoolean("botaoAtivo")) {
                configData.getJSONObject("academia").put("botaoAtivo", false);
                salvarDados();
            }
        } catch (JSONException e) {
            isActive = false;
            cardTreinoPanel.setVisibility(View.GONE);
            treinoAtual = null;
            limparTimer();
            treinoHojePanel.setVisibility(View.VISIBLE);
            dadosContainer.setVisibility(View.VISIBLE);
        }
        atualizarTreinoHoje();
    }

    private void renderDados() {
        dadosContainer.removeAllViews();

        if (isActive) {
            dadosContainer.setVisibility(View.GONE);
            return;
        }
        dadosContainer.setVisibility(View.VISIBLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.parseColor("#1a1a1a"));
        box.setPadding(dpToPx(14), dpToPx(16), dpToPx(14), dpToPx(16));
        GradientDrawable border = new GradientDrawable();
        border.setStroke(1, Color.parseColor("#2a2a2a"));
        border.setColor(Color.parseColor("#1a1a1a"));
        box.setBackground(border);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setWeightSum(2);

        TextView title = new TextView(this);
        title.setText("Dados da Academia");
        title.setTextColor(Color.parseColor("#aaaaaa"));
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        title.setLayoutParams(titleParams);
        header.addView(title);

        Button configBtn2 = new Button(this);
        configBtn2.setText(modoConfig ? "Pronto" : "Configurar");
        configBtn2.setBackgroundColor(modoConfig ? Color.parseColor("#1a3a1a") : Color.parseColor("#2a2a2a"));
        configBtn2.setTextColor(modoConfig ? Color.parseColor("#8bc34a") : Color.parseColor("#cccccc"));
        configBtn2.setPadding(dpToPx(12), dpToPx(5), dpToPx(12), dpToPx(5));
        configBtn2.setOnClickListener(v -> {
            modoConfig = !modoConfig;
            renderDados();
        });
        header.addView(configBtn2);

        box.addView(header);

        View separator = new View(this);
        separator.setBackgroundColor(Color.parseColor("#2a2a2a"));
        separator.setMinimumHeight(1);
        separator.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        box.addView(separator);

        if (modoConfig) {
            renderModoConfig(box);
        } else {
            renderModoVisualizacao(box);
        }

        dadosContainer.addView(box);
    }

    private void renderModoVisualizacao(LinearLayout parent) {
        try {
            JSONObject academia = configData.getJSONObject("academia");
            JSONObject peso = academia.getJSONObject("peso");
            int diasFreq = getDiasFrequentados();
            String inicioDisplay = academia.isNull("inicio") ? "Nao definida" : formatDataBR(academia.getString("inicio"));

            LinearLayout infoGrid = new LinearLayout(this);
            infoGrid.setOrientation(LinearLayout.VERTICAL);
            infoGrid.setPadding(0, dpToPx(8), 0, dpToPx(8));

            addInfoRow(infoGrid, "Data de Inicio", inicioDisplay + (academia.has("inicio") && !academia.isNull("inicio") && diasFreq > 0 ? " (" + diasFreq + " dias)" : ""));
            
            String pesoAtual = peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg";
            String evolucaoTexto = "";
            String evolucaoCor = "#666666";
            if (peso.has("historico") && peso.getJSONArray("historico").length() >= 2) {
                JSONArray hist = peso.getJSONArray("historico");
                double primeiro = hist.getJSONObject(0).getDouble("peso");
                double ultimo = hist.getJSONObject(hist.length() - 1).getDouble("peso");
                double diff = ultimo - primeiro;
                double pct = primeiro != 0 ? (diff / primeiro) * 100 : 0;
                evolucaoTexto = " (" + (diff > 0 ? "+" : "") + String.format("%.1f", diff) + "kg | " + (pct > 0 ? "+" : "") + String.format("%.1f", pct) + "%)";
                evolucaoCor = diff > 0 ? "#8bc34a" : (diff < 0 ? "#ff6b6b" : "#aaaaaa");
            }
            addInfoRow(infoGrid, "Peso Atual", pesoAtual + evolucaoTexto, evolucaoCor);
            
            if (!peso.isNull("meta")) {
                addInfoRow(infoGrid, "Meta de Peso", peso.getDouble("meta") + " kg");
            }
            addInfoRow(infoGrid, "Ultima pesagem", getUltimaPesagemData());
            addInfoRow(infoGrid, "Intervalo entre pesagem", peso.getInt("intervalo") + " dias");

            parent.addView(infoGrid);

            JSONArray treinos = academia.getJSONArray("treinos");
            JSONArray todosExercicios = new JSONArray();
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject t = treinos.getJSONObject(i);
                if (t.has("exercicios")) {
                    JSONArray exs = t.getJSONArray("exercicios");
                    for (int j = 0; j < exs.length(); j++) {
                        JSONObject e = exs.getJSONObject(j);
                        if (e.has("series")) {
                            JSONArray series = e.getJSONArray("series");
                            for (int k = 0; k < series.length(); k++) {
                                JSONObject s = series.getJSONObject(k);
                                if (!(s.has("warmup") && s.getBoolean("warmup"))) {
                                    JSONObject exCopy = new JSONObject();
                                    exCopy.put("exercise", e.getString("exercise") + " (Serie " + (k+1) + ")");
                                    if (!s.has("loadHistory")) s.put("loadHistory", new JSONArray());
                                    exCopy.put("loadHistory", s.getJSONArray("loadHistory"));
                                    todosExercicios.put(exCopy);
                                }
                            }
                        }
                    }
                }
            }

            if (todosExercicios.length() > 0) {
                LinearLayout subSection = new LinearLayout(this);
                subSection.setOrientation(LinearLayout.VERTICAL);
                subSection.setPadding(0, dpToPx(12), 0, dpToPx(8));
                View line2 = new View(this);
                line2.setBackgroundColor(Color.parseColor("#2a2a2a"));
                line2.setMinimumHeight(1);
                subSection.addView(line2);

                TextView subTitle = new TextView(this);
                subTitle.setText("Evolucao de Carga");
                subTitle.setTextColor(Color.parseColor("#999999"));
                subTitle.setTextSize(13);
                subTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                subTitle.setPadding(0, dpToPx(8), 0, dpToPx(8));
                subSection.addView(subTitle);

                for (int i = 0; i < todosExercicios.length(); i++) {
                    JSONObject ex = todosExercicios.getJSONObject(i);
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setBackgroundColor(Color.parseColor("#0d0d0d"));
                    row.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
                    GradientDrawable border2 = new GradientDrawable();
                    border2.setStroke(1, Color.parseColor("#1a1a1a"));
                    border2.setColor(Color.parseColor("#0d0d0d"));
                    row.setBackground(border2);

                    TextView exName = new TextView(this);
                    exName.setText(ex.getString("exercise"));
                    exName.setTextColor(Color.parseColor("#cccccc"));
                    exName.setTextSize(12);
                    exName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    row.addView(exName);

                    String evolucao = "Sem dados";
                    String progresso = "0%";
                    String cor = "#666666";
                    if (ex.has("loadHistory") && ex.getJSONArray("loadHistory").length() >= 2) {
                        JSONArray hist = ex.getJSONArray("loadHistory");
                        double primeiro = hist.getJSONObject(0).getDouble("load");
                        double ultimo = hist.getJSONObject(hist.length() - 1).getDouble("load");
                        double diff = ultimo - primeiro;
                        double pct = primeiro != 0 ? (diff / primeiro) * 100 : 0;
                        evolucao = (diff > 0 ? "+" : "") + String.format("%.1f", diff) + "kg";
                        progresso = (pct > 0 ? "+" : "") + String.format("%.1f", pct) + "%";
                        cor = diff > 0 ? "#8bc34a" : (diff < 0 ? "#ff6b6b" : "#aaaaaa");
                    }

                    TextView exEvo = new TextView(this);
                    exEvo.setText(evolucao);
                    exEvo.setTextColor(Color.parseColor(cor));
                    exEvo.setTextSize(12);
                    exEvo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    row.addView(exEvo);

                    TextView exProg = new TextView(this);
                    exProg.setText(progresso);
                    exProg.setTextColor(Color.parseColor(cor));
                    exProg.setTextSize(12);
                    exProg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    row.addView(exProg);

                    subSection.addView(row);
                }
                parent.addView(subSection);
            }

            LinearLayout subSection = new LinearLayout(this);
            subSection.setOrientation(LinearLayout.VERTICAL);
            subSection.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line3 = new View(this);
            line3.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line3.setMinimumHeight(1);
            subSection.addView(line3);

            TextView subTitle2 = new TextView(this);
            subTitle2.setText("Objetivos");
            subTitle2.setTextColor(Color.parseColor("#999999"));
            subTitle2.setTextSize(13);
            subTitle2.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle2.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSection.addView(subTitle2);

            JSONArray objetivos = academia.getJSONArray("objetivos");
            if (objetivos.length() > 0) {
                for (int i = 0; i < objetivos.length(); i++) {
                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.HORIZONTAL);
                    item.setBackgroundColor(Color.parseColor("#0d0d0d"));
                    item.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
                    GradientDrawable border3 = new GradientDrawable();
                    border3.setStroke(1, Color.parseColor("#1a1a1a"));
                    border3.setColor(Color.parseColor("#0d0d0d"));
                    item.setBackground(border3);
                    TextView lbl = new TextView(this);
                    lbl.setText("• " + objetivos.getString(i));
                    lbl.setTextColor(Color.parseColor("#eeeeee"));
                    lbl.setTextSize(13);
                    item.addView(lbl);
                    subSection.addView(item);
                }
            } else {
                TextView empty = new TextView(this);
                empty.setText("Nenhum objetivo definido.");
                empty.setTextColor(Color.parseColor("#666666"));
                empty.setTextSize(11);
                empty.setPadding(dpToPx(10), dpToPx(6), 0, dpToPx(6));
                subSection.addView(empty);
            }

            parent.addView(subSection);

            JSONObject roupas = academia.getJSONObject("roupas");
            String[] cats = {"camisas", "calcas", "tenis"};
            String[] catLabels = {"Camisas", "Calcas", "Tenis"};
            LinearLayout subSectionRoupas = new LinearLayout(this);
            subSectionRoupas.setOrientation(LinearLayout.VERTICAL);
            subSectionRoupas.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line4 = new View(this);
            line4.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line4.setMinimumHeight(1);
            subSectionRoupas.addView(line4);

            TextView subTitle3 = new TextView(this);
            subTitle3.setText("Roupas");
            subTitle3.setTextColor(Color.parseColor("#999999"));
            subTitle3.setTextSize(13);
            subTitle3.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle3.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSectionRoupas.addView(subTitle3);

            for (int c = 0; c < cats.length; c++) {
                LinearLayout catLayout = new LinearLayout(this);
                catLayout.setOrientation(LinearLayout.VERTICAL);
                catLayout.setPadding(0, dpToPx(4), 0, dpToPx(4));
                
                TextView catLabel = new TextView(this);
                catLabel.setText(catLabels[c]);
                catLabel.setTextColor(Color.parseColor("#888888"));
                catLabel.setTextSize(11);
                catLayout.addView(catLabel);
                
                JSONArray items = roupas.getJSONArray(cats[c]);
                if (items.length() > 0) {
                    for (int i = 0; i < items.length(); i++) {
                        LinearLayout item = new LinearLayout(this);
                        item.setOrientation(LinearLayout.HORIZONTAL);
                        item.setBackgroundColor(Color.parseColor("#0d0d0d"));
                        item.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
                        TextView lbl = new TextView(this);
                        lbl.setText("• " + items.getString(i));
                        lbl.setTextColor(Color.parseColor("#cccccc"));
                        lbl.setTextSize(12);
                        item.addView(lbl);
                        catLayout.addView(item);
                    }
                } else {
                    TextView empty = new TextView(this);
                    empty.setText("Nenhuma " + catLabels[c].toLowerCase());
                    empty.setTextColor(Color.parseColor("#666666"));
                    empty.setTextSize(11);
                    empty.setPadding(dpToPx(8), dpToPx(3), 0, dpToPx(3));
                    catLayout.addView(empty);
                }
                subSectionRoupas.addView(catLayout);
            }
            parent.addView(subSectionRoupas);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addInfoRow(LinearLayout parent, String label, String value) {
        addInfoRow(parent, label, value, null);
    }

    private void addInfoRow(LinearLayout parent, String label, String value, String color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#0d0d0d"));
        row.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        GradientDrawable border = new GradientDrawable();
        border.setStroke(1, Color.parseColor("#1a1a1a"));
        border.setColor(Color.parseColor("#0d0d0d"));
        row.setBackground(border);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.parseColor("#888888"));
        lbl.setTextSize(12);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(color != null ? Color.parseColor(color) : Color.parseColor("#eeeeee"));
        val.setTextSize(13);
        val.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(val);

        parent.addView(row);
    }

    private void renderModoConfig(LinearLayout parent) {
        try {
            JSONObject academia = configData.getJSONObject("academia");
            JSONObject peso = academia.getJSONObject("peso");
            int diasFreq = getDiasFrequentados();
            String inicioDisplay = academia.isNull("inicio") ? "Nao definida" : formatDataBR(academia.getString("inicio"));

            LinearLayout subSection = new LinearLayout(this);
            subSection.setOrientation(LinearLayout.VERTICAL);
            subSection.setPadding(0, dpToPx(8), 0, dpToPx(8));

            addConfigRow(subSection, "Data de Inicio", inicioDisplay + (academia.has("inicio") && !academia.isNull("inicio") && diasFreq > 0 ? " (" + diasFreq + " dias)" : ""), "Editar", v -> mostrarEditarInicio());
            addConfigRow(subSection, "Peso Atual", peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg", "Registrar", v -> mostrarRegistrarPeso());
            addConfigRow(subSection, "Historico de Peso", "", "Ver", v -> mostrarHistoricoPeso());
            addConfigRow(subSection, "Intervalo Pesagem", peso.getInt("intervalo") + " dias", "Editar", v -> mostrarEditarIntervalo());
            if (!peso.isNull("meta")) {
                addConfigRow(subSection, "Meta", peso.getDouble("meta") + " kg", "", null);
            }

            parent.addView(subSection);

            LinearLayout subSection2 = new LinearLayout(this);
            subSection2.setOrientation(LinearLayout.VERTICAL);
            subSection2.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line = new View(this);
            line.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line.setMinimumHeight(1);
            subSection2.addView(line);

            TextView subTitle = new TextView(this);
            subTitle.setText("Dias de Descanso");
            subTitle.setTextColor(Color.parseColor("#999999"));
            subTitle.setTextSize(13);
            subTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSection2.addView(subTitle);

            LinearLayout gridPanel = new LinearLayout(this);
            gridPanel.setOrientation(LinearLayout.VERTICAL);
            JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
            for (String d : DIAS_SEMANA) {
                boolean checked = false;
                for (int i = 0; i < diasDescanso.length(); i++) {
                    if (diasDescanso.getString(i).equals(d)) {
                        checked = true;
                        break;
                    }
                }
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dpToPx(2), 0, dpToPx(2));
                CheckBox cb = new CheckBox(this);
                cb.setText(d);
                cb.setChecked(checked);
                cb.setTextColor(Color.parseColor("#aaaaaa"));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    try {
                        JSONArray novos = new JSONArray();
                        LinearLayout parentLayout = (LinearLayout) buttonView.getParent().getParent();
                        for (int j = 0; j < parentLayout.getChildCount(); j++) {
                            View child = parentLayout.getChildAt(j);
                            if (child instanceof LinearLayout) {
                                LinearLayout rowLayout = (LinearLayout) child;
                                for (int k = 0; k < rowLayout.getChildCount(); k++) {
                                    View v = rowLayout.getChildAt(k);
                                    if (v instanceof CheckBox) {
                                        CheckBox c = (CheckBox) v;
                                        if (c.isChecked()) novos.put(c.getText().toString());
                                    }
                                }
                            }
                        }
                        configData.getJSONObject("academia").put("diasDescanso", novos);
                        salvarDados();
                        renderDados();
                    } catch (JSONException ex) {}
                });
                row.addView(cb);
                gridPanel.addView(row);
            }
            subSection2.addView(gridPanel);
            parent.addView(subSection2);

            LinearLayout subSection3 = new LinearLayout(this);
            subSection3.setOrientation(LinearLayout.VERTICAL);
            subSection3.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line2 = new View(this);
            line2.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line2.setMinimumHeight(1);
            subSection3.addView(line2);

            TextView subTitle2 = new TextView(this);
            subTitle2.setText("Objetivos");
            subTitle2.setTextColor(Color.parseColor("#999999"));
            subTitle2.setTextSize(13);
            subTitle2.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle2.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSection3.addView(subTitle2);

            JSONArray objetivos = academia.getJSONArray("objetivos");
            for (int i = 0; i < objetivos.length(); i++) {
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setBackgroundColor(Color.parseColor("#0d0d0d"));
                item.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
                GradientDrawable border = new GradientDrawable();
                border.setStroke(1, Color.parseColor("#1a1a1a"));
                border.setColor(Color.parseColor("#0d0d0d"));
                item.setBackground(border);

                TextView lbl = new TextView(this);
                lbl.setText("• " + objetivos.getString(i));
                lbl.setTextColor(Color.parseColor("#eeeeee"));
                lbl.setTextSize(13);
                lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                item.addView(lbl);

                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                int idx = i;
                Button editObj = new Button(this);
                editObj.setText("✎");
                editObj.setTextColor(Color.parseColor("#88aaff"));
                editObj.setBackground(null);
                editObj.setOnClickListener(v -> mostrarEditarObjetivo(idx));
                actions.addView(editObj);

                Button delObj = new Button(this);
                delObj.setText("✕");
                delObj.setTextColor(Color.parseColor("#ff6666"));
                delObj.setBackground(null);
                delObj.setOnClickListener(v -> {
                    mostrarConfirmacao("Excluir Objetivo", "Tem certeza que deseja excluir este objetivo?", () -> {
                        try {
                            JSONArray objs = configData.getJSONObject("academia").getJSONArray("objetivos");
                            objs.remove(idx);
                            salvarDados();
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                });
                actions.addView(delObj);

                item.addView(actions);
                subSection3.addView(item);
            }

            Button addObjBtn = new Button(this);
            addObjBtn.setText("+ Adicionar Objetivo");
            addObjBtn.setBackgroundColor(Color.parseColor("#1a3a1a"));
            addObjBtn.setTextColor(Color.parseColor("#8bc34a"));
            addObjBtn.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            addObjBtn.setOnClickListener(v -> mostrarAdicionarObjetivo());
            subSection3.addView(addObjBtn);

            parent.addView(subSection3);

            LinearLayout subSection4 = new LinearLayout(this);
            subSection4.setOrientation(LinearLayout.VERTICAL);
            subSection4.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line3 = new View(this);
            line3.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line3.setMinimumHeight(1);
            subSection4.addView(line3);

            TextView subTitle3 = new TextView(this);
            subTitle3.setText("Treinos");
            subTitle3.setTextColor(Color.parseColor("#999999"));
            subTitle3.setTextSize(13);
            subTitle3.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle3.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSection4.addView(subTitle3);

            JSONArray treinos = academia.getJSONArray("treinos");
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject treino = treinos.getJSONObject(i);
                LinearLayout treinoPanel = new LinearLayout(this);
                treinoPanel.setOrientation(LinearLayout.VERTICAL);
                treinoPanel.setBackgroundColor(Color.parseColor("#0d0d0d"));
                treinoPanel.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
                GradientDrawable border2 = new GradientDrawable();
                border2.setStroke(1, Color.parseColor("#1a1a1a"));
                border2.setColor(Color.parseColor("#0d0d0d"));
                treinoPanel.setBackground(border2);

                LinearLayout headerPanel = new LinearLayout(this);
                headerPanel.setOrientation(LinearLayout.HORIZONTAL);

                TextView nomeLabel = new TextView(this);
                nomeLabel.setText(treino.getString("nome"));
                nomeLabel.setTextColor(Color.parseColor("#eeeeee"));
                nomeLabel.setTextSize(13);
                nomeLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                nomeLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                headerPanel.addView(nomeLabel);

                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                int treinoIdx = i;
                Button editTreino = new Button(this);
                editTreino.setText("✎");
                editTreino.setTextColor(Color.parseColor("#88aaff"));
                editTreino.setBackground(null);
                editTreino.setOnClickListener(v -> mostrarEditarTreino(treinoIdx));
                actions.addView(editTreino);

                Button delTreino = new Button(this);
                delTreino.setText("✕");
                delTreino.setTextColor(Color.parseColor("#ff6666"));
                delTreino.setBackground(null);
                delTreino.setOnClickListener(v -> {
                    mostrarConfirmacao("Excluir Treino", "Tem certeza que deseja excluir este treino?", () -> {
                        try {
                            JSONArray ts = configData.getJSONObject("academia").getJSONArray("treinos");
                            ts.remove(treinoIdx);
                            salvarDados();
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                });
                actions.addView(delTreino);

                headerPanel.addView(actions);
                treinoPanel.addView(headerPanel);

                TextView diaLabel = new TextView(this);
                diaLabel.setText("Dia: " + treino.getString("dia"));
                diaLabel.setTextColor(Color.parseColor("#888888"));
                diaLabel.setTextSize(12);
                treinoPanel.addView(diaLabel);

                if (treino.has("objetivo") && !treino.isNull("objetivo") && !treino.getString("objetivo").isEmpty()) {
                    TextView objLabel = new TextView(this);
                    objLabel.setText("Objetivo: " + treino.getString("objetivo"));
                    objLabel.setTextColor(Color.parseColor("#888888"));
                    objLabel.setTextSize(12);
                    treinoPanel.addView(objLabel);
                }

                LinearLayout exContainer = new LinearLayout(this);
                exContainer.setOrientation(LinearLayout.VERTICAL);
                treinoPanel.addView(exContainer);

                renderExerciciosLista(treinoIdx, exContainer);

                Button addExBtn = new Button(this);
                addExBtn.setText("+ Exercicio");
                addExBtn.setBackgroundColor(Color.parseColor("#1a3a1a"));
                addExBtn.setTextColor(Color.parseColor("#8bc34a"));
                addExBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
                addExBtn.setOnClickListener(v -> mostrarAdicionarExercicio(treinoIdx));
                treinoPanel.addView(addExBtn);

                subSection4.addView(treinoPanel);
            }

            Button addTreinoBtn = new Button(this);
            addTreinoBtn.setText("+ Adicionar Treino");
            addTreinoBtn.setBackgroundColor(Color.parseColor("#1a3a1a"));
            addTreinoBtn.setTextColor(Color.parseColor("#8bc34a"));
            addTreinoBtn.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            addTreinoBtn.setOnClickListener(v -> mostrarAdicionarTreino());
            subSection4.addView(addTreinoBtn);

            parent.addView(subSection4);

            LinearLayout subSection5 = new LinearLayout(this);
            subSection5.setOrientation(LinearLayout.VERTICAL);
            subSection5.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line4 = new View(this);
            line4.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line4.setMinimumHeight(1);
            subSection5.addView(line4);

            TextView subTitle4 = new TextView(this);
            subTitle4.setText("Roupas");
            subTitle4.setTextColor(Color.parseColor("#999999"));
            subTitle4.setTextSize(13);
            subTitle4.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle4.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSection5.addView(subTitle4);

            JSONObject roupas = academia.getJSONObject("roupas");
            String[] cats = {"camisas", "calcas", "tenis"};
            String[] catLabels = {"Camisas", "Calcas", "Tenis"};
            for (int c = 0; c < cats.length; c++) {
                LinearLayout catLayout = new LinearLayout(this);
                catLayout.setOrientation(LinearLayout.VERTICAL);
                catLayout.setPadding(0, dpToPx(4), 0, dpToPx(4));
                
                TextView catLabel = new TextView(this);
                catLabel.setText(catLabels[c]);
                catLabel.setTextColor(Color.parseColor("#888888"));
                catLabel.setTextSize(11);
                catLayout.addView(catLabel);

                JSONArray items = roupas.getJSONArray(cats[c]);
                for (int i = 0; i < items.length(); i++) {
                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.HORIZONTAL);
                    item.setBackgroundColor(Color.parseColor("#0d0d0d"));
                    item.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));

                    TextView lbl = new TextView(this);
                    lbl.setText("• " + items.getString(i));
                    lbl.setTextColor(Color.parseColor("#cccccc"));
                    lbl.setTextSize(12);
                    lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    item.addView(lbl);

                    LinearLayout actions2 = new LinearLayout(this);
                    actions2.setOrientation(LinearLayout.HORIZONTAL);
                    int roupaIdx = i;
                    String catFinal = cats[c];
                    Button editRoupa = new Button(this);
                    editRoupa.setText("✎");
                    editRoupa.setTextColor(Color.parseColor("#88aaff"));
                    editRoupa.setBackground(null);
                    editRoupa.setOnClickListener(v -> mostrarEditarRoupa(catFinal, roupaIdx));
                    actions2.addView(editRoupa);

                    Button delRoupa = new Button(this);
                    delRoupa.setText("✕");
                    delRoupa.setTextColor(Color.parseColor("#ff6666"));
                    delRoupa.setBackground(null);
                    delRoupa.setOnClickListener(v -> {
                        mostrarConfirmacao("Excluir Roupa", "Tem certeza que deseja excluir esta roupa?", () -> {
                            try {
                                JSONObject r = configData.getJSONObject("academia").getJSONObject("roupas");
                                r.getJSONArray(catFinal).remove(roupaIdx);
                                salvarDados();
                                renderDados();
                            } catch (JSONException ex) {}
                        });
                    });
                    actions2.addView(delRoupa);

                    item.addView(actions2);
                    catLayout.addView(item);
                }

                Button addRoupaBtn = new Button(this);
                addRoupaBtn.setText("+ Adicionar " + catLabels[c].toLowerCase());
                addRoupaBtn.setBackgroundColor(Color.parseColor("#1a3a1a"));
                addRoupaBtn.setTextColor(Color.parseColor("#8bc34a"));
                addRoupaBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
                String catFinal2 = cats[c];
                addRoupaBtn.setOnClickListener(v -> mostrarAdicionarRoupa(catFinal2));
                catLayout.addView(addRoupaBtn);
                subSection5.addView(catLayout);
            }
            parent.addView(subSection5);

            LinearLayout subSection6 = new LinearLayout(this);
            subSection6.setOrientation(LinearLayout.VERTICAL);
            subSection6.setPadding(0, dpToPx(12), 0, dpToPx(8));
            View line5 = new View(this);
            line5.setBackgroundColor(Color.parseColor("#2a2a2a"));
            line5.setMinimumHeight(1);
            subSection6.addView(line5);

            TextView subTitle5 = new TextView(this);
            subTitle5.setText("Combinacoes por Dia");
            subTitle5.setTextColor(Color.parseColor("#999999"));
            subTitle5.setTextSize(13);
            subTitle5.setTypeface(null, android.graphics.Typeface.BOLD);
            subTitle5.setPadding(0, dpToPx(8), 0, dpToPx(8));
            subSection6.addView(subTitle5);

            JSONObject combinacoes2 = academia.getJSONObject("combinacoes");
            JSONArray diasDescanso2 = academia.getJSONArray("diasDescanso");
            JSONArray workingDays2 = new JSONArray();
            for (String d : DIAS_SEMANA) {
                boolean isDescanso = false;
                for (int i = 0; i < diasDescanso2.length(); i++) {
                    if (diasDescanso2.getString(i).equals(d)) {
                        isDescanso = true;
                        break;
                    }
                }
                if (!isDescanso) workingDays2.put(d);
            }

            if (workingDays2.length() > 0) {
                for (int i = 0; i < workingDays2.length(); i++) {
                    String day = workingDays2.getString(i);
                    LinearLayout dayPanel = new LinearLayout(this);
                    dayPanel.setOrientation(LinearLayout.VERTICAL);
                    dayPanel.setBackgroundColor(Color.parseColor("#0d0d0d"));
                    dayPanel.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
                    GradientDrawable border3 = new GradientDrawable();
                    border3.setStroke(1, Color.parseColor("#1a1a1a"));
                    border3.setColor(Color.parseColor("#0d0d0d"));
                    dayPanel.setBackground(border3);

                    TextView dayTitle = new TextView(this);
                    dayTitle.setText(day);
                    dayTitle.setTextColor(Color.parseColor("#aaaaaa"));
                    dayTitle.setTextSize(12);
                    dayTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    dayPanel.addView(dayTitle);

                    JSONArray combos2 = combinacoes2.has(day) ? combinacoes2.getJSONArray(day) : new JSONArray();
                    for (int j = 0; j < combos2.length(); j++) {
                        LinearLayout comboItem = new LinearLayout(this);
                        comboItem.setOrientation(LinearLayout.HORIZONTAL);
                        comboItem.setPadding(0, dpToPx(2), 0, dpToPx(2));

                        TextView comboText = new TextView(this);
                        comboText.setText("• " + combos2.getString(j));
                        comboText.setTextColor(Color.parseColor("#cccccc"));
                        comboText.setTextSize(12);
                        comboText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                        comboItem.addView(comboText);

                        int comboIdx = j;
                        String dayFinal = day;
                        Button delCombo = new Button(this);
                        delCombo.setText("✕");
                        delCombo.setTextColor(Color.parseColor("#ff6666"));
                        delCombo.setBackground(null);
                        delCombo.setOnClickListener(v -> {
                            mostrarConfirmacao("Excluir Combinacao", "Tem certeza que deseja excluir esta combinacao?", () -> {
                                try {
                                    JSONObject comb = configData.getJSONObject("academia").getJSONObject("combinacoes");
                                    if (comb.has(dayFinal)) {
                                        JSONArray cArray = comb.getJSONArray(dayFinal);
                                        cArray.remove(comboIdx);
                                        if (cArray.length() == 0) comb.remove(dayFinal);
                                        salvarDados();
                                        renderDados();
                                    }
                                } catch (JSONException ex) {}
                            });
                        });
                        comboItem.addView(delCombo);
                        dayPanel.addView(comboItem);
                    }

                    JSONObject roupas2 = academia.getJSONObject("roupas");
                    LinearLayout comboSelects = new LinearLayout(this);
                    comboSelects.setOrientation(LinearLayout.HORIZONTAL);
                    comboSelects.setPadding(0, dpToPx(4), 0, 0);

                    Spinner camisaSpinner = new Spinner(this);
                    ArrayAdapter<String> camisaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
                    camisaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    camisaAdapter.add("");
                    JSONArray camisas = roupas2.getJSONArray("camisas");
                    for (int j = 0; j < camisas.length(); j++) camisaAdapter.add(camisas.getString(j));
                    camisaSpinner.setAdapter(camisaAdapter);
                    camisaSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    Spinner calcaSpinner = new Spinner(this);
                    ArrayAdapter<String> calcaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
                    calcaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    calcaAdapter.add("");
                    JSONArray calcas = roupas2.getJSONArray("calcas");
                    for (int j = 0; j < calcas.length(); j++) calcaAdapter.add(calcas.getString(j));
                    calcaSpinner.setAdapter(calcaAdapter);
                    calcaSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    Spinner tenisSpinner = new Spinner(this);
                    ArrayAdapter<String> tenisAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
                    tenisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    tenisAdapter.add("");
                    JSONArray tenis = roupas2.getJSONArray("tenis");
                    for (int j = 0; j < tenis.length(); j++) tenisAdapter.add(tenis.getString(j));
                    tenisSpinner.setAdapter(tenisAdapter);
                    tenisSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                    comboSelects.addView(camisaSpinner);
                    comboSelects.addView(calcaSpinner);
                    comboSelects.addView(tenisSpinner);

                    Button addComboBtn = new Button(this);
                    addComboBtn.setText("+");
                    addComboBtn.setBackgroundColor(Color.parseColor("#1a3a1a"));
                    addComboBtn.setTextColor(Color.parseColor("#8bc34a"));
                    addComboBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
                    String dayFinal2 = day;
                    addComboBtn.setOnClickListener(v -> {
                        try {
                            String cam = camisaSpinner.getSelectedItem().toString();
                            String cal = calcaSpinner.getSelectedItem().toString();
                            String ten = tenisSpinner.getSelectedItem().toString();
                            String comboText = "";
                            if (!cam.isEmpty()) comboText += cam;
                            if (!cal.isEmpty()) {
                                if (!comboText.isEmpty()) comboText += ", ";
                                comboText += cal;
                            }
                            if (!ten.isEmpty()) {
                                if (!comboText.isEmpty()) comboText += ", ";
                                comboText += ten;
                            }
                            if (comboText.isEmpty()) {
                                Toast.makeText(this, "Selecione pelo menos uma peca.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            JSONObject comb = configData.getJSONObject("academia").getJSONObject("combinacoes");
                            if (!comb.has(dayFinal2)) comb.put(dayFinal2, new JSONArray());
                            comb.getJSONArray(dayFinal2).put(comboText);
                            salvarDados();
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                    comboSelects.addView(addComboBtn);

                    dayPanel.addView(comboSelects);
                    subSection6.addView(dayPanel);
                }
            } else {
                TextView empty = new TextView(this);
                empty.setText("Nenhum dia disponivel. Defina os dias de descanso primeiro.");
                empty.setTextColor(Color.parseColor("#666666"));
                empty.setTextSize(11);
                empty.setPadding(dpToPx(10), dpToPx(6), 0, dpToPx(6));
                subSection6.addView(empty);
            }

            parent.addView(subSection6);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addConfigRow(LinearLayout parent, String label, String value, String actionLabel, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#0d0d0d"));
        row.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        GradientDrawable border = new GradientDrawable();
        border.setStroke(1, Color.parseColor("#1a1a1a"));
        border.setColor(Color.parseColor("#0d0d0d"));
        row.setBackground(border);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.parseColor("#888888"));
        lbl.setTextSize(11);
        textLayout.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(Color.parseColor("#eeeeee"));
        val.setTextSize(13);
        val.setTypeface(null, android.graphics.Typeface.BOLD);
        textLayout.addView(val);

        row.addView(textLayout);

        if (actionLabel != null && listener != null) {
            Button btn = new Button(this);
            btn.setText(actionLabel);
            btn.setBackgroundColor(Color.parseColor("#1a3a1a"));
            btn.setTextColor(Color.parseColor("#8bc34a"));
            btn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
            btn.setOnClickListener(listener);
            row.addView(btn);
        }

        parent.addView(row);
    }

    private void renderExerciciosLista(int treinoIdx, LinearLayout container) {
        container.removeAllViews();
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            if (!treino.has("exercicios")) return;

            JSONArray exercicios = treino.getJSONArray("exercicios");
            for (int i = 0; i < exercicios.length(); i++) {
                JSONObject ex = exercicios.getJSONObject(i);
                LinearLayout exItem = new LinearLayout(this);
                exItem.setOrientation(LinearLayout.VERTICAL);
                exItem.setBackgroundColor(Color.parseColor("#0d0d0d"));
                exItem.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
                GradientDrawable border = new GradientDrawable();
                border.setStroke(1, Color.parseColor("#1a1a1a"));
                border.setColor(Color.parseColor("#0d0d0d"));
                exItem.setBackground(border);

                LinearLayout headerLayout = new LinearLayout(this);
                headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                headerLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView nomeEx = new TextView(this);
                nomeEx.setText(ex.getString("exercise"));
                nomeEx.setTextColor(Color.parseColor("#ffffff"));
                nomeEx.setTextSize(14);
                nomeEx.setTypeface(null, android.graphics.Typeface.BOLD);
                nomeEx.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                headerLayout.addView(nomeEx);

                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                int exIdx = i;

                Button addSerieBtn = new Button(this);
                addSerieBtn.setText("+ Serie");
                addSerieBtn.setTextColor(Color.parseColor("#8bc34a"));
                addSerieBtn.setBackground(null);
                addSerieBtn.setOnClickListener(v -> mostrarAdicionarSerie(treinoIdx, exIdx));
                actions.addView(addSerieBtn);

                Button editExBtn = new Button(this);
                editExBtn.setText("✎");
                editExBtn.setTextColor(Color.parseColor("#88aaff"));
                editExBtn.setBackground(null);
                editExBtn.setOnClickListener(v -> mostrarEditarExercicio(treinoIdx, exIdx));
                actions.addView(editExBtn);

                Button delExBtn = new Button(this);
                delExBtn.setText("✕");
                delExBtn.setTextColor(Color.parseColor("#ff6666"));
                delExBtn.setBackground(null);
                delExBtn.setOnClickListener(v -> {
                    mostrarConfirmacao("Excluir Exercicio", "Tem certeza que deseja excluir este exercicio?", () -> {
                        try {
                            JSONArray ts = configData.getJSONObject("academia").getJSONArray("treinos");
                            JSONObject t = ts.getJSONObject(treinoIdx);
                            t.getJSONArray("exercicios").remove(exIdx);
                            salvarDados();
                            renderDados();
                        } catch (JSONException exc) {}
                    });
                });
                actions.addView(delExBtn);

                headerLayout.addView(actions);
                exItem.addView(headerLayout);

                LinearLayout seriesContainer = new LinearLayout(this);
                seriesContainer.setOrientation(LinearLayout.VERTICAL);
                seriesContainer.setPadding(dpToPx(8), dpToPx(4), 0, dpToPx(4));

                if (ex.has("series")) {
                    JSONArray series = ex.getJSONArray("series");
                    for (int j = 0; j < series.length(); j++) {
                        JSONObject s = series.getJSONObject(j);
                        LinearLayout serieItem = new LinearLayout(this);
                        serieItem.setOrientation(LinearLayout.HORIZONTAL);
                        serieItem.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
                        serieItem.setBackgroundColor(Color.parseColor("#0a0a0a"));

                        TextView serieText = new TextView(this);
                        String warmup = s.has("warmup") && s.getBoolean("warmup") ? " [AQUECIMENTO]" : "";
                        serieText.setText("Serie " + (j+1) + ": " + s.getInt("reps") + "x " + s.getDouble("load") + "kg" + warmup);
                        serieText.setTextColor(Color.parseColor("#aaaaaa"));
                        serieText.setTextSize(11);
                        serieText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                        serieItem.addView(serieText);

final int serieIdx = j;
Button delSerieBtn = new Button(this);
delSerieBtn.setText("✕");
delSerieBtn.setTextColor(Color.parseColor("#ff6666"));
delSerieBtn.setBackground(null);
delSerieBtn.setOnClickListener(v -> {
    mostrarConfirmacao("Excluir Serie", "Tem certeza que deseja excluir esta serie?", () -> {
        try {
            JSONArray sArray = ex.getJSONArray("series");
            sArray.remove(serieIdx);
            salvarDados();
            renderDados();
        } catch (JSONException exc) {}
    });
});
serieItem.addView(delSerieBtn);

                        seriesContainer.addView(serieItem);
                    }
                }

                exItem.addView(seriesContainer);
                container.addView(exItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void mostrarEditarInicio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Definir Data de Inicio");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        final TextView dataDisplay = new TextView(this);
        dataDisplay.setText("Selecione a data");
        dataDisplay.setTextColor(Color.parseColor("#eeeeee"));
        dataDisplay.setTextSize(16);
        dataDisplay.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        dataDisplay.setBackgroundColor(Color.parseColor("#0d0d0d"));
        dataDisplay.setGravity(android.view.Gravity.CENTER);
        dataDisplay.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            String current = "";
            try {
                current = configData.getJSONObject("academia").isNull("inicio") ? "" : configData.getJSONObject("academia").getString("inicio");
                if (!current.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = sdf.parse(current);
                    calendar.setTime(date);
                }
            } catch (Exception e) {}

            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String dataSelecionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                dataDisplay.setText(dataSelecionada);
                dataDisplay.setTag(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        String currentData = "";
        try {
            currentData = configData.getJSONObject("academia").isNull("inicio") ? "" : formatDataBR(configData.getJSONObject("academia").getString("inicio"));
            if (!currentData.isEmpty()) {
                dataDisplay.setText(currentData);
                dataDisplay.setTag(configData.getJSONObject("academia").getString("inicio"));
            }
        } catch (JSONException e) {}

        layout.addView(dataDisplay);

        builder.setView(layout);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String val = (String) dataDisplay.getTag();
            if (val == null || val.isEmpty()) {
                Toast.makeText(this, "Selecione uma data valida.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                configData.getJSONObject("academia").put("inicio", val);
                salvarDados();
                renderDados();
            } catch (JSONException ex) {}
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarEditarIntervalo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Intervalo para Pesagem");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        int current = 7;
        try {
            current = configData.getJSONObject("academia").getJSONObject("peso").getInt("intervalo");
        } catch (JSONException e) {}

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setBackgroundColor(Color.parseColor("#0d0d0d"));
        input.setTextColor(Color.parseColor("#ffffff"));
        layout.addView(input);

        TextView note = new TextView(this);
        note.setText("A cada quantos dias voce deve pesar?");
        note.setTextColor(Color.parseColor("#666666"));
        note.setTextSize(11);
        layout.addView(note);

        builder.setView(layout);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            try {
                int val = Integer.parseInt(input.getText().toString());
                if (val < 1) throw new NumberFormatException();
                configData.getJSONObject("academia").getJSONObject("peso").put("intervalo", val);
                salvarDados();
                renderDados();
            } catch (Exception ex) {
                Toast.makeText(this, "Valor invalido.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarRegistrarPeso() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registrar Peso");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        TextView pesoLabel = new TextView(this);
        pesoLabel.setText("Peso (kg)");
        pesoLabel.setTextColor(Color.parseColor("#888888"));
        pesoLabel.setTextSize(12);
        layout.addView(pesoLabel);

        final EditText pesoInput = new EditText(this);
        pesoInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pesoInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
        pesoInput.setTextColor(Color.parseColor("#ffffff"));
        layout.addView(pesoInput);

        TextView metaLabel = new TextView(this);
        metaLabel.setText("Meta (opcional)");
        metaLabel.setTextColor(Color.parseColor("#888888"));
        metaLabel.setTextSize(12);
        layout.addView(metaLabel);

        double currentMeta = 0;
        try {
            currentMeta = configData.getJSONObject("academia").getJSONObject("peso").isNull("meta") ? 0 : configData.getJSONObject("academia").getJSONObject("peso").getDouble("meta");
        } catch (JSONException e) {}

        final EditText metaInput = new EditText(this);
        metaInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        metaInput.setText(currentMeta > 0 ? String.valueOf(currentMeta) : "");
        metaInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
        metaInput.setTextColor(Color.parseColor("#ffffff"));
        layout.addView(metaInput);

        builder.setView(layout);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            try {
                double peso = Double.parseDouble(pesoInput.getText().toString());
                if (peso <= 0) throw new NumberFormatException();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                String hoje = sdf.format(new Date());
                JSONObject pesoObj = configData.getJSONObject("academia").getJSONObject("peso");
                JSONArray historico = pesoObj.getJSONArray("historico");
                JSONObject novo = new JSONObject();
                novo.put("peso", peso);
                novo.put("data", hoje);
                historico.put(novo);
                pesoObj.put("atual", peso);
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                pesoObj.put("ultimoRegistro", sdf2.format(new Date()));

                String metaStr = metaInput.getText().toString().trim();
                if (!metaStr.isEmpty()) {
                    double meta = Double.parseDouble(metaStr);
                    if (meta > 0) pesoObj.put("meta", meta);
                }

                salvarDados();
                renderDados();
                if (treinoAtual != null) renderTreinoCard();
            } catch (Exception ex) {
                Toast.makeText(this, "Peso invalido.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarHistoricoPeso() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Historico de Peso");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        try {
            JSONArray historico = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
            if (historico.length() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Nenhum registro.");
                empty.setTextColor(Color.parseColor("#666666"));
                empty.setTextSize(11);
                layout.addView(empty);
            } else {
                for (int i = 0; i < historico.length(); i++) {
                    JSONObject item = historico.getJSONObject(i);
                    LinearLayout entry = new LinearLayout(this);
                    entry.setOrientation(LinearLayout.HORIZONTAL);
                    entry.setPadding(0, dpToPx(4), 0, dpToPx(4));

                    TextView info = new TextView(this);
                    info.setText(item.getDouble("peso") + " kg (" + item.getString("data") + ")");
                    info.setTextColor(Color.parseColor("#bbbbbb"));
                    info.setTextSize(12);
                    info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    entry.addView(info);

                    final int idx = i;
                    Button delBtn = new Button(this);
                    delBtn.setText("✕");
                    delBtn.setTextColor(Color.parseColor("#ff6666"));
                    delBtn.setBackground(null);
                    delBtn.setOnClickListener(v -> {
                        mostrarConfirmacao("Excluir Registro", "Tem certeza que deseja excluir este registro?", () -> {
                            try {
                                JSONArray hist = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
                                hist.remove(idx);
                                if (hist.length() > 0) {
                                    JSONObject last = hist.getJSONObject(hist.length() - 1);
                                    configData.getJSONObject("academia").getJSONObject("peso").put("atual", last.getDouble("peso"));
                                } else {
                                    configData.getJSONObject("academia").getJSONObject("peso").put("atual", JSONObject.NULL);
                                }
                                salvarDados();
                                mostrarHistoricoPeso();
                            } catch (JSONException ex) {}
                        });
                    });
                    entry.addView(delBtn);

                    layout.addView(entry);
                }
            }
        } catch (JSONException e) {}

        builder.setView(layout);
        builder.setPositiveButton("Fechar", null);
        builder.show();
    }

    private void mostrarAdicionarObjetivo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo Objetivo");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Digite o objetivo");
        input.setBackgroundColor(Color.parseColor("#0d0d0d"));
        input.setTextColor(Color.parseColor("#ffffff"));
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(this, "Digite um objetivo.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                configData.getJSONObject("academia").getJSONArray("objetivos").put(val);
                salvarDados();
                renderDados();
            } catch (JSONException ex) {}
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarEditarObjetivo(int idx) {
        try {
            JSONArray objetivos = configData.getJSONObject("academia").getJSONArray("objetivos");
            String current = objetivos.getString(idx);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Editar Objetivo");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(current);
            input.setBackgroundColor(Color.parseColor("#0d0d0d"));
            input.setTextColor(Color.parseColor("#ffffff"));
            builder.setView(input);

            builder.setPositiveButton("Salvar", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "Digite um objetivo.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    JSONArray objs = configData.getJSONObject("academia").getJSONArray("objetivos");
                    objs.put(idx, val);
                    salvarDados();
                    renderDados();
                } catch (JSONException ex) {}
            });
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarTreino() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo Treino");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        TextView nomeLabel = new TextView(this);
        nomeLabel.setText("Nome");
        nomeLabel.setTextColor(Color.parseColor("#888888"));
        nomeLabel.setTextSize(12);
        layout.addView(nomeLabel);

        final EditText nomeInput = new EditText(this);
        nomeInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nomeInput.setHint("Nome do treino");
        nomeInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
        nomeInput.setTextColor(Color.parseColor("#ffffff"));
        layout.addView(nomeInput);

        TextView diaLabel = new TextView(this);
        diaLabel.setText("Dia *");
        diaLabel.setTextColor(Color.parseColor("#888888"));
        diaLabel.setTextSize(12);
        layout.addView(diaLabel);

        final Spinner diaSpinner = new Spinner(this);
        ArrayAdapter<String> diaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        diaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        diaAdapter.add("Selecione um dia");
        for (String d : DIAS_SEMANA) diaAdapter.add(d);
        diaSpinner.setAdapter(diaAdapter);
        layout.addView(diaSpinner);

        TextView objLabel = new TextView(this);
        objLabel.setText("Objetivo (opcional)");
        objLabel.setTextColor(Color.parseColor("#888888"));
        objLabel.setTextSize(12);
        layout.addView(objLabel);

        final Spinner objSpinner = new Spinner(this);
        ArrayAdapter<String> objAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        objAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        objAdapter.add("Nenhum");
        try {
            JSONArray objetivos = configData.getJSONObject("academia").getJSONArray("objetivos");
            for (int i = 0; i < objetivos.length(); i++) objAdapter.add(objetivos.getString(i));
        } catch (JSONException e) {}
        objSpinner.setAdapter(objAdapter);
        layout.addView(objSpinner);

        builder.setView(layout);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String nome = nomeInput.getText().toString().trim();
            String dia = diaSpinner.getSelectedItem().toString();
            String obj = objSpinner.getSelectedItem().toString();
            if (nome.isEmpty() || dia.equals("Selecione um dia")) {
                Toast.makeText(this, "Nome e dia sao obrigatorios.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject treino = new JSONObject();
                treino.put("nome", nome);
                treino.put("dia", dia);
                treino.put("objetivo", obj.equals("Nenhum") ? JSONObject.NULL : obj);
                treino.put("exercicios", new JSONArray());
                configData.getJSONObject("academia").getJSONArray("treinos").put(treino);
                salvarDados();
                renderDados();
                atualizarTreinoHoje();
            } catch (JSONException ex) {}
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarEditarTreino(int idx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(idx);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Editar Treino");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

            TextView nomeLabel = new TextView(this);
            nomeLabel.setText("Nome");
            nomeLabel.setTextColor(Color.parseColor("#888888"));
            nomeLabel.setTextSize(12);
            layout.addView(nomeLabel);

            final EditText nomeInput = new EditText(this);
            nomeInput.setInputType(InputType.TYPE_CLASS_TEXT);
            nomeInput.setText(treino.getString("nome"));
            nomeInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
            nomeInput.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(nomeInput);

            TextView diaLabel = new TextView(this);
            diaLabel.setText("Dia *");
            diaLabel.setTextColor(Color.parseColor("#888888"));
            diaLabel.setTextSize(12);
            layout.addView(diaLabel);

            final Spinner diaSpinner = new Spinner(this);
            ArrayAdapter<String> diaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
            diaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            diaAdapter.add("Selecione um dia");
            for (String d : DIAS_SEMANA) diaAdapter.add(d);
            diaSpinner.setAdapter(diaAdapter);
            diaSpinner.setSelection(diaAdapter.getPosition(treino.getString("dia")));
            layout.addView(diaSpinner);

            TextView objLabel = new TextView(this);
            objLabel.setText("Objetivo (opcional)");
            objLabel.setTextColor(Color.parseColor("#888888"));
            objLabel.setTextSize(12);
            layout.addView(objLabel);

            final Spinner objSpinner = new Spinner(this);
            ArrayAdapter<String> objAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
            objAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            objAdapter.add("Nenhum");
            try {
                JSONArray objetivos = configData.getJSONObject("academia").getJSONArray("objetivos");
                for (int i = 0; i < objetivos.length(); i++) objAdapter.add(objetivos.getString(i));
            } catch (JSONException e) {}
            objSpinner.setAdapter(objAdapter);
            if (treino.has("objetivo") && !treino.isNull("objetivo")) {
                objSpinner.setSelection(objAdapter.getPosition(treino.getString("objetivo")));
            }
            layout.addView(objSpinner);

            builder.setView(layout);
            builder.setPositiveButton("Salvar", (dialog, which) -> {
                String nome = nomeInput.getText().toString().trim();
                String dia = diaSpinner.getSelectedItem().toString();
                String obj = objSpinner.getSelectedItem().toString();
                if (nome.isEmpty() || dia.equals("Selecione um dia")) {
                    Toast.makeText(this, "Nome e dia sao obrigatorios.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    treino.put("nome", nome);
                    treino.put("dia", dia);
                    treino.put("objetivo", obj.equals("Nenhum") ? JSONObject.NULL : obj);
                    salvarDados();
                    renderDados();
                    atualizarTreinoHoje();
                } catch (JSONException ex) {}
            });
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarExercicio(int treinoIdx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adicionar Exercicio");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        TextView exLabel = new TextView(this);
        exLabel.setText("Nome do Exercicio *");
        exLabel.setTextColor(Color.parseColor("#888888"));
        exLabel.setTextSize(12);
        layout.addView(exLabel);

        final EditText exInput = new EditText(this);
        exInput.setInputType(InputType.TYPE_CLASS_TEXT);
        exInput.setHint("Ex: Supino");
        exInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
        exInput.setTextColor(Color.parseColor("#ffffff"));
        layout.addView(exInput);

        builder.setView(layout);
        builder.setPositiveButton("Criar Exercicio", (dialog, which) -> {
            try {
                String exercise = exInput.getText().toString().trim();
                if (exercise.isEmpty()) {
                    Toast.makeText(this, "Nome do exercicio e obrigatorio.", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject exercicio = new JSONObject();
                exercicio.put("exercise", exercise);
                exercicio.put("series", new JSONArray());

                JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
                JSONObject treino = treinos.getJSONObject(treinoIdx);
                if (!treino.has("exercicios")) treino.put("exercicios", new JSONArray());
                treino.getJSONArray("exercicios").put(exercicio);
                salvarDados();
                renderDados();
            } catch (Exception ex) {
                Toast.makeText(this, "Erro ao criar exercicio.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarAdicionarSerie(int treinoIdx, int exIdx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            JSONObject ex = treino.getJSONArray("exercicios").getJSONObject(exIdx);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Adicionar Serie - " + ex.getString("exercise"));

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

            TextView repsLabel = new TextView(this);
            repsLabel.setText("Repeticoes *");
            repsLabel.setTextColor(Color.parseColor("#888888"));
            repsLabel.setTextSize(12);
            layout.addView(repsLabel);

            final EditText repsInput = new EditText(this);
            repsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            repsInput.setText("10");
            repsInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
            repsInput.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(repsInput);

            TextView loadLabel = new TextView(this);
            loadLabel.setText("Carga (kg) *");
            loadLabel.setTextColor(Color.parseColor("#888888"));
            loadLabel.setTextSize(12);
            layout.addView(loadLabel);

            final EditText loadInput = new EditText(this);
            loadInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            loadInput.setText("20");
            loadInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
            loadInput.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(loadInput);

            TextView metaLabel = new TextView(this);
            metaLabel.setText("Meta de Carga (kg, opcional)");
            metaLabel.setTextColor(Color.parseColor("#888888"));
            metaLabel.setTextSize(12);
            layout.addView(metaLabel);

            final EditText metaInput = new EditText(this);
            metaInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            metaInput.setHint("Ex: 30");
            metaInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
            metaInput.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(metaInput);

            TextView descLabel = new TextView(this);
            descLabel.setText("Descanso (segundos)");
            descLabel.setTextColor(Color.parseColor("#888888"));
            descLabel.setTextSize(12);
            layout.addView(descLabel);

            final EditText descInput = new EditText(this);
            descInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            descInput.setText("60");
            descInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
            descInput.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(descInput);

            final CheckBox warmupCheck = new CheckBox(this);
            warmupCheck.setText("Serie de aquecimento");
            warmupCheck.setTextColor(Color.parseColor("#aaaaaa"));
            layout.addView(warmupCheck);

            builder.setView(layout);
            builder.setPositiveButton("Adicionar Serie", (dialog, which) -> {
                try {
                    int reps = Integer.parseInt(repsInput.getText().toString().trim());
                    double load = Double.parseDouble(loadInput.getText().toString().trim());
                    if (reps < 1 || load <= 0) {
                        throw new NumberFormatException();
                    }
                    JSONObject serie = new JSONObject();
                    serie.put("reps", reps);
                    serie.put("load", load);
                    serie.put("warmup", warmupCheck.isChecked());
                    serie.put("_feita", false);
                    serie.put("loadHistory", new JSONArray());

                    int descanso = 0;
                    if (!descInput.getText().toString().trim().isEmpty()) {
                        descanso = Integer.parseInt(descInput.getText().toString().trim());
                    }
                    if (descanso > 0) serie.put("descanso", descanso);

                    String metaStr = metaInput.getText().toString().trim();
                    if (!metaStr.isEmpty()) {
                        double meta = Double.parseDouble(metaStr);
                        if (meta > 0) serie.put("metaCarga", meta);
                    }

                    if (!ex.has("series")) ex.put("series", new JSONArray());
                    ex.getJSONArray("series").put(serie);
                    salvarDados();
                    renderDados();
                } catch (Exception ex2) {
                    Toast.makeText(this, "Valores invalidos. Verifique os campos.", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        } catch (JSONException e) {}
    }

    private void mostrarEditarExercicio(int treinoIdx, int exIdx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            JSONObject ex = treino.getJSONArray("exercicios").getJSONObject(exIdx);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Editar Exercicio - " + ex.getString("exercise"));

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

            TextView nomeLabel = new TextView(this);
            nomeLabel.setText("Nome do Exercicio");
            nomeLabel.setTextColor(Color.parseColor("#888888"));
            nomeLabel.setTextSize(12);
            layout.addView(nomeLabel);

            final EditText nomeInput = new EditText(this);
            nomeInput.setInputType(InputType.TYPE_CLASS_TEXT);
            nomeInput.setText(ex.getString("exercise"));
            nomeInput.setBackgroundColor(Color.parseColor("#0d0d0d"));
            nomeInput.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(nomeInput);

            builder.setView(layout);
            builder.setPositiveButton("Salvar", (dialog, which) -> {
                try {
                    String novoNome = nomeInput.getText().toString().trim();
                    if (novoNome.isEmpty()) {
                        Toast.makeText(this, "Nome nao pode estar vazio.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ex.put("exercise", novoNome);
                    salvarDados();
                    renderDados();
                } catch (Exception ex2) {
                    Toast.makeText(this, "Erro ao salvar.", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        } catch (JSONException e) {}
    }

    private void mostrarHistoricoCarga() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Historico de Carga");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject treino = treinos.getJSONObject(i);
                if (treino.has("exercicios")) {
                    JSONArray exs = treino.getJSONArray("exercicios");
                    for (int j = 0; j < exs.length(); j++) {
                        JSONObject ex = exs.getJSONObject(j);
                        if (ex.has("series")) {
                            JSONArray series = ex.getJSONArray("series");
                            for (int k = 0; k < series.length(); k++) {
                                JSONObject s = series.getJSONObject(k);
                                if (s.has("loadHistory") && s.getJSONArray("loadHistory").length() > 0) {
                                    JSONArray history = s.getJSONArray("loadHistory");
                                    for (int h = 0; h < history.length(); h++) {
                                        JSONObject item = history.getJSONObject(h);
                                        LinearLayout entry = new LinearLayout(this);
                                        entry.setOrientation(LinearLayout.HORIZONTAL);
                                        entry.setPadding(0, dpToPx(4), 0, dpToPx(4));

                                        TextView info = new TextView(this);
                                        info.setText(ex.getString("exercise") + " (S" + (k+1) + "): " + item.getDouble("load") + "kg × " + item.getInt("reps") + " reps (" + item.getString("date") + ")");
                                        info.setTextColor(Color.parseColor("#bbbbbb"));
                                        info.setTextSize(12);
                                        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                                        entry.addView(info);

                                        layout.addView(entry);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            builder.setView(layout);
            builder.setPositiveButton("Fechar", null);
            builder.show();
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarRoupa(String categoria) {
        String label = categoria.equals("camisas") ? "Camisa" : categoria.equals("calcas") ? "Calca" : "Tenis";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova " + label);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nome da " + label.toLowerCase());
        input.setBackgroundColor(Color.parseColor("#0d0d0d"));
        input.setTextColor(Color.parseColor("#ffffff"));
        layout.addView(input);

        final CheckBox sweatCheck = new CheckBox(this);
        sweatCheck.setText("Marca suor");
        sweatCheck.setTextColor(Color.parseColor("#aaaaaa"));
        layout.addView(sweatCheck);

        builder.setView(layout);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(this, "Nome e obrigatorio.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sweatCheck.isChecked()) val += " (suor)";
            try {
                JSONObject roupas = configData.getJSONObject("academia").getJSONObject("roupas");
                roupas.getJSONArray(categoria).put(val);
                salvarDados();
                renderDados();
            } catch (JSONException ex) {}
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarEditarRoupa(String categoria, int idx) {
        try {
            JSONObject roupas = configData.getJSONObject("academia").getJSONObject("roupas");
            String item = roupas.getJSONArray(categoria).getString(idx);
            boolean isSweat = item.contains("(suor)");
            String cleanName = item.replace(" (suor)", "");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Editar Roupa");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(cleanName);
            input.setBackgroundColor(Color.parseColor("#0d0d0d"));
            input.setTextColor(Color.parseColor("#ffffff"));
            layout.addView(input);

            final CheckBox sweatCheck = new CheckBox(this);
            sweatCheck.setText("Marca suor");
            sweatCheck.setChecked(isSweat);
            sweatCheck.setTextColor(Color.parseColor("#aaaaaa"));
            layout.addView(sweatCheck);

            builder.setView(layout);
            builder.setPositiveButton("Salvar", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "Nome e obrigatorio.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (sweatCheck.isChecked()) val += " (suor)";
                try {
                    JSONObject r = configData.getJSONObject("academia").getJSONObject("roupas");
                    r.getJSONArray(categoria).put(idx, val);
                    salvarDados();
                    renderDados();
                } catch (JSONException ex) {}
            });
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        } catch (JSONException e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        salvarDados();
    }

    @Override
    protected void onPause() {
        super.onPause();
        salvarDados();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarDados();
        atualizarTreinoHoje();
        renderDados();
    }
}
EOF
