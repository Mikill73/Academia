mkdir -p src/main/java/com/academia/app
cat > src/main/java/com/academia/app/MainActivity.java << 'EOF'
package com.academia.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private JSONObject configData;
    private JSONObject treinoAtual;
    private int exercicioAtualIndex = 0;
    private boolean isActive = false;
    private boolean aguardandoTimer = false;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private int timerRestante = 0;
    private boolean modoConfig = false;

    private LinearLayout mainLayout;
    private FrameLayout overlayModal;
    private LinearLayout overlayContent;
    private FrameLayout confirmModal;
    private LinearLayout todayCard;
    private LinearLayout exercisesContainer;
    private LinearLayout timerContainer;
    private View progressFill;
    private TextView progressText;
    private Button mainBtn;
    private Button configBtn;
    private LinearLayout dadosContainer;
    private FrameLayout pesoAvisoModal;
    private FrameLayout subModal;

    private static final String[] DIAS_SEMANA = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};

    private static final int COR_FUNDO = Color.rgb(13, 13, 13);
    private static final int COR_CARD = Color.rgb(26, 26, 26);
    private static final int COR_VERMELHO = Color.rgb(255, 0, 0);
    private static final int COR_VERDE = Color.rgb(0, 204, 0);
    private static final int COR_VERDE_CLARO = Color.rgb(139, 195, 74);
    private static final int COR_CINZA_ESCURO = Color.rgb(136, 136, 136);
    private static final int COR_CINZA_MEDIO = Color.rgb(170, 170, 170);
    private static final int COR_CINZA_CLARO = Color.rgb(204, 204, 204);
    private static final int COR_BRANCO = Color.WHITE;
    private static final int COR_AMARELO = Color.rgb(255, 170, 0);
    private static final int COR_VERMELHO_CLARO = Color.rgb(255, 138, 138);
    private static final int COR_VERDE_ESCURO = Color.rgb(26, 58, 26);
    private static final int COR_FUNDO_OVERLAY = Color.argb(180, 0, 0, 0);
    private static final int COR_FUNDO_TRANSPARENTE = Color.argb(0, 0, 0, 0);
    private static final int COR_CINZA_MAIS_CLARO = Color.rgb(238, 238, 238);
    private static final int COR_FUNDO_INPUT = Color.rgb(10, 10, 10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            timerHandler = new Handler(Looper.getMainLooper());
            loadConfig();
            setupUI();
            carregarEstadoBotao();
            renderDados();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        salvarNoArquivo();
    }

    private void salvarNoArquivo() {
        try {
            if (configData == null) return;
            File file = new File(getFilesDir(), "academia_dados.json");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            osw.write(configData.toString());
            osw.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarDoArquivo() {
        try {
            File file = new File(getFilesDir(), "academia_dados.json");
            if (!file.exists()) return;
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = isr.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            isr.close();
            fis.close();
            String json = sb.toString();
            if (json != null && !json.isEmpty()) {
                configData = new JSONObject(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        try {
            carregarDoArquivo();
            if (configData == null) {
                createDefaultConfig();
            }
            if (!configData.has("academia")) {
                createDefaultConfig();
            }
            JSONObject academia = configData.getJSONObject("academia");
            if (!academia.has("peso")) {
                JSONObject peso = new JSONObject();
                peso.put("atual", JSONObject.NULL);
                peso.put("historico", new JSONArray());
                peso.put("meta", JSONObject.NULL);
                peso.put("intervalo", 7);
                peso.put("ultimoRegistro", JSONObject.NULL);
                academia.put("peso", peso);
            }
            if (!academia.has("roupas")) {
                JSONObject roupas = new JSONObject();
                roupas.put("camisas", new JSONArray());
                roupas.put("calcas", new JSONArray());
                roupas.put("tenis", new JSONArray());
                academia.put("roupas", roupas);
            }
            if (!academia.has("combinacoes")) {
                academia.put("combinacoes", new JSONObject());
            }
            if (!academia.has("treinos")) {
                academia.put("treinos", new JSONArray());
            }
            if (!academia.has("objetivos")) {
                academia.put("objetivos", new JSONArray());
            }
            if (!academia.has("diasDescanso")) {
                academia.put("diasDescanso", new JSONArray());
            }
            if (!academia.has("treinoConcluido")) {
                academia.put("treinoConcluido", new JSONObject());
            }
            if (!academia.has("botaoAtivo")) {
                academia.put("botaoAtivo", false);
            }
            if (!academia.has("inicio")) {
                academia.put("inicio", JSONObject.NULL);
            }
            JSONArray treinos = academia.getJSONArray("treinos");
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject treino = treinos.getJSONObject(i);
                if (treino.has("exercicios")) {
                    JSONArray exs = treino.getJSONArray("exercicios");
                    for (int j = 0; j < exs.length(); j++) {
                        JSONObject ex = exs.getJSONObject(j);
                        if (!ex.has("loadHistory")) {
                            ex.put("loadHistory", new JSONArray());
                        }
                    }
                }
            }
            if (academia.getJSONObject("peso").has("historico")) {
                JSONArray hist = academia.getJSONObject("peso").getJSONArray("historico");
                if (hist.length() > 0) {
                    JSONObject ultimo = hist.getJSONObject(hist.length() - 1);
                    academia.getJSONObject("peso").put("atual", ultimo.getDouble("peso"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
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
            saveConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try {
            if (configData != null) {
                salvarNoArquivo();
                if (dadosContainer != null) renderDados();
                if (treinoAtual != null) renderTreinoCard();
                carregarEstadoBotao();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTodayName() {
        String[] names = {"Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
        try {
            return names[new Date().getDay()];
        } catch (Exception e) {
            return "Segunda";
        }
    }

    private String getTodayKey() {
        try {
            Date d = new Date();
            return String.format(Locale.getDefault(), "%02d/%02d/%d", d.getDate(), d.getMonth() + 1, d.getYear() + 1900);
        } catch (Exception e) {
            return "01/01/2024";
        }
    }

    private void setupUI() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(COR_FUNDO);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mainBtn = new Button(this);
        LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(dpToPx(130), dpToPx(130));
        mainParams.gravity = Gravity.CENTER_HORIZONTAL;
        mainParams.topMargin = dpToPx(10);
        mainBtn.setLayoutParams(mainParams);
        mainBtn.setBackgroundDrawable(new ShapeDrawable(new OvalShape()));
        mainBtn.setBackgroundColor(COR_VERMELHO);
        mainBtn.setText("");
        mainBtn.setTextColor(COR_BRANCO);
        mainBtn.setTextSize(0);
        mainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMainBtnClick();
            }
        });
        mainLayout.addView(mainBtn);

        todayCard = new LinearLayout(this);
        todayCard.setOrientation(LinearLayout.VERTICAL);
        todayCard.setBackgroundColor(COR_CARD);
        todayCard.setPadding(dpToPx(14), dpToPx(16), dpToPx(14), dpToPx(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dpToPx(20);
        todayCard.setLayoutParams(cardParams);
        todayCard.setVisibility(View.GONE);

        TextView cardTitle = new TextView(this);
        cardTitle.setText("Treino de Hoje");
        cardTitle.setTextColor(COR_CINZA_MEDIO);
        cardTitle.setTextSize(14);
        cardTitle.setGravity(Gravity.CENTER);
        cardTitle.setPadding(0, 0, 0, dpToPx(8));
        todayCard.addView(cardTitle);

        exercisesContainer = new LinearLayout(this);
        exercisesContainer.setOrientation(LinearLayout.VERTICAL);
        todayCard.addView(exercisesContainer);

        timerContainer = new LinearLayout(this);
        timerContainer.setOrientation(LinearLayout.VERTICAL);
        todayCard.addView(timerContainer);

        LinearLayout progressLayout = new LinearLayout(this);
        progressLayout.setBackgroundColor(COR_FUNDO);
        progressLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(5)));
        progressFill = new View(this);
        progressFill.setBackgroundColor(COR_VERDE_CLARO);
        progressFill.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
        progressLayout.addView(progressFill);
        todayCard.addView(progressLayout);

        progressText = new TextView(this);
        progressText.setText("0/0 concluídos");
        progressText.setTextColor(COR_CINZA_ESCURO);
        progressText.setTextSize(11);
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(0, dpToPx(6), 0, 0);
        todayCard.addView(progressText);

        mainLayout.addView(todayCard);

        dadosContainer = new LinearLayout(this);
        dadosContainer.setOrientation(LinearLayout.VERTICAL);
        dadosContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dadosContainer.setPadding(0, dpToPx(10), 0, 0);
        mainLayout.addView(dadosContainer);

        scrollView.addView(mainLayout);
        root.addView(scrollView);

        setupOverlayModal(root);
        setupPesoAvisoModal(root);
        setupConfirmModal(root);
        setupSubModal(root);

        setContentView(root);
    }

    private void setupOverlayModal(FrameLayout root) {
        overlayModal = new FrameLayout(this);
        overlayModal.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlayModal.setBackgroundColor(COR_FUNDO_OVERLAY);
        overlayModal.setVisibility(View.GONE);
        overlayModal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeOverlay();
            }
        });

        overlayContent = new LinearLayout(this);
        overlayContent.setOrientation(LinearLayout.VERTICAL);
        overlayContent.setBackgroundColor(COR_CARD);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        overlayParams.gravity = Gravity.CENTER;
        overlayParams.setMargins(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        overlayContent.setLayoutParams(overlayParams);
        overlayContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        Button closeBtn = new Button(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(COR_CINZA_ESCURO);
        closeBtn.setTextSize(28);
        closeBtn.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeOverlay();
            }
        });
        header.addView(closeBtn);

        TextView overlayTitle = new TextView(this);
        overlayTitle.setText("Academia");
        overlayTitle.setTextColor(COR_CINZA_MEDIO);
        overlayTitle.setTextSize(18);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.gravity = Gravity.CENTER;
        overlayTitle.setLayoutParams(titleParams);
        header.addView(overlayTitle);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(30), ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(spacer);

        overlayContent.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        final LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dpToPx(14), 0, dpToPx(14), dpToPx(14));
        scroll.addView(contentContainer);
        overlayContent.addView(scroll);
        overlayModal.addView(overlayContent);
        root.addView(overlayModal);
    }

    private void setupPesoAvisoModal(FrameLayout root) {
        pesoAvisoModal = new FrameLayout(this);
        pesoAvisoModal.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pesoAvisoModal.setBackgroundColor(COR_FUNDO_OVERLAY);
        pesoAvisoModal.setVisibility(View.GONE);
        LinearLayout avisoBox = new LinearLayout(this);
        avisoBox.setOrientation(LinearLayout.VERTICAL);
        avisoBox.setBackgroundColor(COR_CARD);
        avisoBox.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        FrameLayout.LayoutParams avisoParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        avisoParams.setMargins(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        avisoBox.setLayoutParams(avisoParams);

        TextView avisoTitle = new TextView(this);
        avisoTitle.setText("Hora de Pesar!");
        avisoTitle.setTextColor(COR_VERMELHO_CLARO);
        avisoTitle.setTextSize(18);
        avisoTitle.setGravity(Gravity.CENTER);
        avisoBox.addView(avisoTitle);

        final TextView avisoText = new TextView(this);
        avisoText.setTextColor(COR_CINZA_CLARO);
        avisoText.setTextSize(14);
        avisoText.setGravity(Gravity.CENTER);
        avisoText.setPadding(0, dpToPx(10), 0, dpToPx(10));
        avisoBox.addView(avisoText);

        final EditText pesoInput = new EditText(this);
        pesoInput.setHint("Peso atual (kg)");
        pesoInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pesoInput.setTextColor(COR_CINZA_CLARO);
        pesoInput.setHintTextColor(COR_CINZA_ESCURO);
        pesoInput.setBackgroundColor(COR_FUNDO_INPUT);
        pesoInput.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        avisoBox.addView(pesoInput);

        Button pesoBtn = new Button(this);
        pesoBtn.setText("Registrar");
        pesoBtn.setTextColor(COR_VERDE_CLARO);
        pesoBtn.setBackgroundColor(COR_VERDE_ESCURO);
        pesoBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pesoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String valStr = pesoInput.getText().toString();
                if (!valStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(valStr);
                        registrarPeso(val);
                        pesoAvisoModal.setVisibility(View.GONE);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Peso inválido", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        avisoBox.addView(pesoBtn);
        pesoAvisoModal.addView(avisoBox);
        pesoAvisoModal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });
        root.addView(pesoAvisoModal);
    }

    private void setupConfirmModal(FrameLayout root) {
        confirmModal = new FrameLayout(this);
        confirmModal.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        confirmModal.setBackgroundColor(COR_FUNDO_OVERLAY);
        confirmModal.setVisibility(View.GONE);
        LinearLayout confirmBox = new LinearLayout(this);
        confirmBox.setOrientation(LinearLayout.VERTICAL);
        confirmBox.setBackgroundColor(COR_CARD);
        confirmBox.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        FrameLayout.LayoutParams confirmParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        confirmParams.setMargins(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        confirmBox.setLayoutParams(confirmParams);

        final TextView confirmTitle = new TextView(this);
        confirmTitle.setTextColor(COR_CINZA_MAIS_CLARO);
        confirmTitle.setTextSize(18);
        confirmTitle.setGravity(Gravity.CENTER);
        confirmBox.addView(confirmTitle);

        final TextView confirmMsg = new TextView(this);
        confirmMsg.setTextColor(COR_CINZA_MEDIO);
        confirmMsg.setTextSize(14);
        confirmMsg.setGravity(Gravity.CENTER);
        confirmMsg.setPadding(0, dpToPx(10), 0, dpToPx(14));
        confirmBox.addView(confirmMsg);

        final LinearLayout confirmBtnRow = new LinearLayout(this);
        confirmBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        confirmBtnRow.setGravity(Gravity.CENTER);
        confirmBtnRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        confirmBox.addView(confirmBtnRow);
        confirmModal.addView(confirmBox);
        confirmModal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });
        root.addView(confirmModal);
    }

    private void setupSubModal(FrameLayout root) {
        subModal = new FrameLayout(this);
        subModal.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        subModal.setBackgroundColor(COR_FUNDO_OVERLAY);
        subModal.setVisibility(View.GONE);
        subModal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSubModal();
            }
        });

        LinearLayout subBox = new LinearLayout(this);
        subBox.setOrientation(LinearLayout.VERTICAL);
        subBox.setBackgroundColor(COR_CARD);
        FrameLayout.LayoutParams subParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        subParams.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        subBox.setLayoutParams(subParams);
        subBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });

        final LinearLayout subContent = new LinearLayout(this);
        subContent.setOrientation(LinearLayout.VERTICAL);
        subContent.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        subBox.addView(subContent);
        subModal.addView(subBox);
        root.addView(subModal);
    }

    private void openOverlay() {
        if (overlayModal != null) {
            overlayModal.setVisibility(View.VISIBLE);
            renderOverlayContent();
        }
    }

    private void closeOverlay() {
        if (overlayModal != null) {
            overlayModal.setVisibility(View.GONE);
        }
    }

    private void openSubModal(String html) {
        if (subModal != null) {
            LinearLayout subContent = (LinearLayout) ((LinearLayout) subModal.getChildAt(0)).getChildAt(0);
            subContent.removeAllViews();
            String[] lines = html.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("<h3>")) {
                    TextView tv = new TextView(this);
                    tv.setText(line.replace("<h3>", "").replace("</h3>", "").trim());
                    tv.setTextColor(COR_CINZA_MEDIO);
                    tv.setTextSize(18);
                    tv.setGravity(Gravity.CENTER);
                    tv.setPadding(0, 0, 0, dpToPx(8));
                    subContent.addView(tv);
                } else if (line.trim().startsWith("<label>")) {
                    TextView tv = new TextView(this);
                    tv.setText(line.replace("<label>", "").replace("</label>", "").trim());
                    tv.setTextColor(COR_CINZA_ESCURO);
                    tv.setTextSize(12);
                    tv.setPadding(0, dpToPx(6), 0, dpToPx(2));
                    subContent.addView(tv);
                } else if (line.trim().startsWith("<input")) {
                    EditText et = new EditText(this);
                    et.setBackgroundColor(COR_FUNDO_INPUT);
                    et.setTextColor(COR_CINZA_CLARO);
                    et.setHintTextColor(COR_CINZA_ESCURO);
                    et.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
                    if (line.contains("type=\"number\"")) {
                        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    }
                    if (line.contains("id=\"exNome\"")) et.setHint("Ex: Supino");
                    if (line.contains("id=\"exSets\"")) { et.setText("3"); }
                    if (line.contains("id=\"exReps\"")) { et.setText("10"); }
                    if (line.contains("id=\"exLoad\"")) et.setHint("20");
                    if (line.contains("id=\"exMetaCarga\"")) et.setHint("Ex: 30");
                    if (line.contains("id=\"exDescansoMin\"")) { et.setHint("Min"); et.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), ViewGroup.LayoutParams.WRAP_CONTENT)); }
                    if (line.contains("id=\"exDescansoSec\"")) { et.setHint("Seg"); et.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), ViewGroup.LayoutParams.WRAP_CONTENT)); }
                    if (line.contains("id=\"exWarmup\"")) {
                        CheckBox cb = new CheckBox(this);
                        cb.setText("Série de aquecimento");
                        cb.setTextColor(COR_CINZA_MEDIO);
                        subContent.addView(cb);
                    }
                    if (!line.contains("type=\"checkbox\"")) {
                        subContent.addView(et);
                    }
                } else if (line.trim().startsWith("<button")) {
                    Button btn = new Button(this);
                    btn.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
                    btn.setTextSize(14);
                    if (line.contains("btn-primary")) {
                        btn.setBackgroundColor(COR_VERDE_ESCURO);
                        btn.setTextColor(COR_VERDE_CLARO);
                    } else {
                        btn.setBackgroundColor(Color.rgb(42, 42, 42));
                        btn.setTextColor(COR_CINZA_CLARO);
                    }
                    String text = line.replaceAll("<[^>]*>", "").trim();
                    btn.setText(text);
                    if (line.contains("id=\"addTreinoBtn\"") || line.contains("id=\"addObjetivoBtn\"") || 
                        line.contains("id=\"addPesoBtn\"") || line.contains("id=\"showPesoHistoryBtn\"") ||
                        line.contains("id=\"editInicioBtn\"") || line.contains("id=\"editIntervaloBtn\"")) {
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                closeSubModal();
                                renderDados();
                            }
                        });
                    } else {
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                closeSubModal();
                            }
                        });
                    }
                    subContent.addView(btn);
                } else if (line.trim().startsWith("<div") && line.contains("btn-row")) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER);
                    row.setPadding(0, dpToPx(10), 0, 0);
                    subContent.addView(row);
                } else if (line.trim().startsWith("<div") && line.contains("small-note")) {
                    TextView tv = new TextView(this);
                    tv.setText(line.replaceAll("<[^>]*>", "").trim());
                    tv.setTextColor(COR_CINZA_ESCURO);
                    tv.setTextSize(11);
                    tv.setPadding(0, dpToPx(4), 0, 0);
                    subContent.addView(tv);
                } else if (line.trim().startsWith("<p")) {
                    TextView tv = new TextView(this);
                    tv.setText(line.replaceAll("<[^>]*>", "").trim());
                    tv.setTextColor(COR_CINZA_MEDIO);
                    tv.setTextSize(14);
                    tv.setPadding(0, dpToPx(6), 0, dpToPx(6));
                    subContent.addView(tv);
                }
            }
            subModal.setVisibility(View.VISIBLE);
        }
    }

    private void closeSubModal() {
        if (subModal != null) {
            subModal.setVisibility(View.GONE);
            LinearLayout subContent = (LinearLayout) ((LinearLayout) subModal.getChildAt(0)).getChildAt(0);
            subContent.removeAllViews();
        }
    }

    private void renderOverlayContent() {
        try {
            ScrollView scroll = (ScrollView) overlayContent.getChildAt(1);
            LinearLayout container = (LinearLayout) scroll.getChildAt(0);
            container.removeAllViews();

            if (configData == null) {
                TextView empty = new TextView(this);
                empty.setText("Nenhum dado disponível.");
                empty.setTextColor(COR_CINZA_CLARO);
                empty.setGravity(Gravity.CENTER);
                container.addView(empty);
                return;
            }

            final JSONObject academia = configData.getJSONObject("academia");
            TextView sectionTitle = new TextView(this);
            sectionTitle.setText("Resumo");
            sectionTitle.setTextColor(Color.rgb(153, 153, 153));
            sectionTitle.setTextSize(14);
            sectionTitle.setPadding(0, dpToPx(10), 0, dpToPx(5));
            container.addView(sectionTitle);

            JSONObject peso = academia.getJSONObject("peso");
            addInfoRow(container, "Data de Início", academia.optString("inicio", "Não definida"));
            addInfoRow(container, "Peso Atual", peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg");
            addInfoRow(container, "Meta", peso.isNull("meta") ? "Não definida" : peso.getDouble("meta") + " kg");
            addInfoRow(container, "Intervalo Pesagem", peso.optInt("intervalo", 7) + " dias");

            Button btnPeso = new Button(this);
            btnPeso.setText("Registrar Peso");
            btnPeso.setTextColor(COR_VERDE_CLARO);
            btnPeso.setBackgroundColor(COR_VERDE_ESCURO);
            btnPeso.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btnPeso.setPadding(0, dpToPx(8), 0, dpToPx(8));
            btnPeso.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPesoInput();
                }
            });
            container.addView(btnPeso);

            sectionTitle = new TextView(this);
            sectionTitle.setText("Dias de Descanso");
            sectionTitle.setTextColor(Color.rgb(153, 153, 153));
            sectionTitle.setTextSize(14);
            sectionTitle.setPadding(0, dpToPx(15), 0, dpToPx(5));
            container.addView(sectionTitle);

            JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
            final LinearLayout diasContainer = new LinearLayout(this);
            diasContainer.setOrientation(LinearLayout.VERTICAL);
            for (String dia : DIAS_SEMANA) {
                CheckBox cb = new CheckBox(this);
                cb.setText(dia);
                cb.setTextColor(COR_CINZA_MEDIO);
                cb.setChecked(diasDescanso.toString().contains("\"" + dia + "\""));
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        try {
                            JSONArray novos = new JSONArray();
                            for (int i = 0; i < DIAS_SEMANA.length; i++) {
                                CheckBox c = (CheckBox) diasContainer.getChildAt(i);
                                if (c.isChecked()) novos.put(c.getText().toString());
                            }
                            academia.put("diasDescanso", novos);
                            saveConfig();
                        } catch (Exception e) {}
                    }
                });
                diasContainer.addView(cb);
            }
            container.addView(diasContainer);

            TextView treinosTitle = new TextView(this);
            treinosTitle.setText("Treinos Disponíveis");
            treinosTitle.setTextColor(Color.rgb(153, 153, 153));
            treinosTitle.setTextSize(14);
            treinosTitle.setPadding(0, dpToPx(15), 0, dpToPx(5));
            container.addView(treinosTitle);

            JSONArray treinos = academia.getJSONArray("treinos");
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject t = treinos.getJSONObject(i);
                String dia = t.getString("dia");
                JSONArray exs = t.getJSONArray("exercicios");
                int qtd = exs.length();
                TextView tv = new TextView(this);
                tv.setText(dia + " - " + qtd + " exercícios");
                tv.setTextColor(COR_CINZA_CLARO);
                tv.setTextSize(13);
                tv.setPadding(0, dpToPx(3), 0, dpToPx(3));
                container.addView(tv);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addInfoRow(LinearLayout container, String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + ": " + value);
        tv.setTextColor(COR_CINZA_CLARO);
        tv.setTextSize(13);
        tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
        container.addView(tv);
    }

    private void showPesoInput() {
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(COR_FUNDO_OVERLAY);
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(COR_CARD);
        box.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        boxParams.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        box.setLayoutParams(boxParams);

        TextView title = new TextView(this);
        title.setText("Registrar Peso");
        title.setTextColor(COR_CINZA_MAIS_CLARO);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Peso (kg)");
        input.setTextColor(COR_CINZA_CLARO);
        input.setBackgroundColor(COR_FUNDO_INPUT);
        input.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        box.addView(input);

        Button saveBtn = new Button(this);
        saveBtn.setText("Salvar");
        saveBtn.setTextColor(COR_VERDE_CLARO);
        saveBtn.setBackgroundColor(COR_VERDE_ESCURO);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double val = Double.parseDouble(input.getText().toString());
                    registrarPeso(val);
                    ((FrameLayout) getWindow().getDecorView()).removeView(overlay);
                    renderOverlayContent();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Inválido", Toast.LENGTH_SHORT).show();
                }
            }
        });
        box.addView(saveBtn);
        overlay.addView(box);
        ((FrameLayout) getWindow().getDecorView()).addView(overlay);
    }

    private void registrarPeso(double val) {
        try {
            if (configData == null) return;
            JSONObject academia = configData.getJSONObject("academia");
            JSONObject peso = academia.getJSONObject("peso");
            JSONArray hist = peso.getJSONArray("historico");
            String hoje = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            JSONObject reg = new JSONObject();
            reg.put("peso", val);
            reg.put("data", hoje);
            hist.put(reg);
            peso.put("atual", val);
            peso.put("ultimoRegistro", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));
            saveConfig();
            if (treinoAtual != null) renderTreinoCard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTodayTreinos() {
        try {
            if (configData == null) return "[]";
            String hoje = getTodayName();
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONArray result = new JSONArray();
            for (int i = 0; i < treinos.length(); i++) {
                if (treinos.getJSONObject(i).getString("dia").equals(hoje)) {
                    result.put(treinos.getJSONObject(i));
                }
            }
            return result.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    private void handleMainBtnClick() {
        if (isActive) {
            if (treinoAtual != null && treinoAtual.has("exercicios")) {
                JSONArray exs = treinoAtual.getJSONArray("exercicios");
                boolean algumFeito = false;
                for (int i = 0; i < exs.length(); i++) {
                    if (exs.getJSONObject(i).optInt("_seriesFeitas", 0) > 0) {
                        algumFeito = true;
                        break;
                    }
                }
                if (algumFeito) {
                    mostrarConfirmacao("Parar Treino", "Você já fez alguns exercícios. Deseja realmente parar?", new ConfirmCallback() {
                        @Override
                        public void onConfirm(boolean ok) {
                            if (ok) {
                                pararTreino();
                            }
                        }
                    });
                    return;
                }
            }
            pararTreino();
        } else {
            try {
                JSONArray treinos = new JSONArray(getTodayTreinos());
                if (treinos.length() == 0) {
                    mostrarConfirmacao("Aviso", "Nenhum treino programado para hoje.", new ConfirmCallback() {
                        @Override
                        public void onConfirm(boolean ok) {}
                    }, true);
                    return;
                }
                iniciarTreino();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void pararTreino() {
        try {
            mainBtn.setBackgroundColor(COR_VERMELHO);
            isActive = false;
            todayCard.setVisibility(View.GONE);
            treinoAtual = null;
            limparTimer();
            if (configData != null) {
                configData.getJSONObject("academia").put("treino_" + getTodayKey(), JSONObject.NULL);
                configData.getJSONObject("academia").put("botaoAtivo", false);
                saveConfig();
            }
            renderDados();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarTreino() {
        try {
            if (configData == null) return;
            final String hojeKey = getTodayKey();
            final JSONObject academia = configData.getJSONObject("academia");
            final JSONObject treinoConcluido = academia.getJSONObject("treinoConcluido");
            if (treinoConcluido.optBoolean(hojeKey, false)) {
                mostrarConfirmacao("Treino Concluído", "Você já concluiu o treino de hoje. Deseja refazê-lo?", new ConfirmCallback() {
                    @Override
                    public void onConfirm(boolean ok) {
                        if (ok) {
                            try {
                                treinoConcluido.put(hojeKey, false);
                                saveConfig();
                                iniciarTreinoAtual();
                            } catch (Exception e) {}
                        }
                    }
                });
                return;
            }
            if (academia.has("treino_" + hojeKey)) {
                treinoAtual = academia.getJSONObject("treino_" + hojeKey);
                exercicioAtualIndex = 0;
                if (treinoAtual.has("exercicios")) {
                    JSONArray exs = treinoAtual.getJSONArray("exercicios");
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject ex = exs.getJSONObject(i);
                        if (!ex.has("_seriesFeitas")) ex.put("_seriesFeitas", 0);
                        if (!ex.has("_done")) ex.put("_done", false);
                        if (!ex.has("loadHistory")) ex.put("loadHistory", new JSONArray());
                        if (ex.has("loadHistory") && ex.getJSONArray("loadHistory").length() > 0) {
                            JSONArray hist = ex.getJSONArray("loadHistory");
                            JSONObject ultimo = hist.getJSONObject(hist.length() - 1);
                            ex.put("load", ultimo.getDouble("load"));
                            if (ultimo.has("reps")) ex.put("reps", ultimo.getInt("reps"));
                        }
                    }
                    boolean algumNaoConcluido = false;
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject ex = exs.getJSONObject(i);
                        if (ex.optInt("_seriesFeitas", 0) < ex.optInt("sets", 1)) {
                            exercicioAtualIndex = i;
                            algumNaoConcluido = true;
                            break;
                        }
                    }
                    if (!algumNaoConcluido) {
                        treinoConcluido.put(hojeKey, true);
                        academia.put("treino_" + hojeKey, JSONObject.NULL);
                        academia.put("botaoAtivo", false);
                        saveConfig();
                        mainBtn.setBackgroundColor(COR_VERMELHO);
                        isActive = false;
                        todayCard.setVisibility(View.GONE);
                        treinoAtual = null;
                        renderDados();
                        return;
                    }
                }
                todayCard.setVisibility(View.VISIBLE);
                mainBtn.setBackgroundColor(COR_VERDE);
                isActive = true;
                academia.put("botaoAtivo", true);
                saveConfig();
                renderTreinoCard();
                renderDados();
                if (verificarPeso()) mostrarAvisoPeso();
                return;
            }
            iniciarTreinoAtual();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarTreinoAtual() {
        try {
            JSONArray treinos = new JSONArray(getTodayTreinos());
            if (treinos.length() == 0) return;
            JSONObject treinoBase = treinos.getJSONObject(0);
            treinoAtual = new JSONObject(treinoBase.toString());
            if (treinoAtual.has("exercicios")) {
                JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
                for (int i = 0; i < exercicios.length(); i++) {
                    JSONObject ex = exercicios.getJSONObject(i);
                    ex.put("_seriesFeitas", 0);
                    ex.put("_done", false);
                    if (!ex.has("loadHistory")) ex.put("loadHistory", new JSONArray());
                    if (ex.has("loadHistory") && ex.getJSONArray("loadHistory").length() > 0) {
                        JSONArray hist = ex.getJSONArray("loadHistory");
                        JSONObject ultimo = hist.getJSONObject(hist.length() - 1);
                        ex.put("load", ultimo.getDouble("load"));
                        if (ultimo.has("reps")) ex.put("reps", ultimo.getInt("reps"));
                    }
                }
            }
            exercicioAtualIndex = 0;
            todayCard.setVisibility(View.VISIBLE);
            mainBtn.setBackgroundColor(COR_VERDE);
            isActive = true;
            configData.getJSONObject("academia").put("botaoAtivo", true);
            configData.getJSONObject("academia").put("treino_" + getTodayKey(), treinoAtual);
            saveConfig();
            renderTreinoCard();
            renderDados();
            if (verificarPeso()) mostrarAvisoPeso();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderTreinoCard() {
        try {
            if (exercisesContainer == null || timerContainer == null) return;
            exercisesContainer.removeAllViews();
            timerContainer.removeAllViews();
            if (treinoAtual == null || !treinoAtual.has("exercicios")) return;
            final JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
            if (exercicioAtualIndex >= exercicios.length()) {
                TextView doneText = new TextView(this);
                doneText.setText("Treino concluído!");
                doneText.setTextColor(COR_CINZA_ESCURO);
                doneText.setGravity(Gravity.CENTER);
                doneText.setPadding(0, dpToPx(20), 0, dpToPx(20));
                exercisesContainer.addView(doneText);
                concluirTreinoCompleto();
                return;
            }
            final JSONObject ex = exercicios.getJSONObject(exercicioAtualIndex);
            final int totalSeries = ex.optInt("sets", 1);
            final int seriesFeitas = ex.optInt("_seriesFeitas", 0);
            boolean isDone = seriesFeitas >= totalSeries;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(COR_FUNDO);
            card.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
            card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView name = new TextView(this);
            String warmupText = ex.optBoolean("warmup", false) ? " (Aquecimento)" : "";
            name.setText(ex.optString("exercise", "Exercício") + warmupText);
            name.setTextColor(COR_CINZA_MAIS_CLARO);
            name.setTextSize(18);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, 0, 0, dpToPx(4));
            card.addView(name);

            TextView details = new TextView(this);
            details.setText(totalSeries + " séries x " + ex.optInt("reps", 0) + " reps");
            details.setTextColor(COR_CINZA_MEDIO);
            details.setTextSize(14);
            details.setGravity(Gravity.CENTER);
            card.addView(details);

            TextView load = new TextView(this);
            load.setText("Carga: " + ex.optDouble("load", 0) + "kg");
            load.setTextColor(COR_VERDE_CLARO);
            load.setTextSize(15);
            load.setGravity(Gravity.CENTER);
            card.addView(load);

            if (ex.has("metaCarga") && !ex.isNull("metaCarga")) {
                TextView meta = new TextView(this);
                meta.setText("Meta: " + ex.getDouble("metaCarga") + "kg");
                meta.setTextColor(COR_AMARELO);
                meta.setTextSize(14);
                meta.setGravity(Gravity.CENTER);
                card.addView(meta);
            }

            TextView status = new TextView(this);
            status.setText(isDone ? "Concluído" : (seriesFeitas + "/" + totalSeries + " séries"));
            status.setTextColor(isDone ? COR_VERDE_CLARO : COR_CINZA_ESCURO);
            status.setTextSize(12);
            status.setGravity(Gravity.CENTER);
            status.setPadding(0, dpToPx(3), 0, 0);
            card.addView(status);

            if (!isDone && !aguardandoTimer) {
                Button btnDone = new Button(this);
                btnDone.setText("PRONTO");
                btnDone.setTextColor(COR_VERDE_CLARO);
                btnDone.setBackgroundColor(COR_VERDE_ESCURO);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                btnParams.setMargins(0, dpToPx(8), 0, 0);
                btnParams.gravity = Gravity.CENTER;
                btnDone.setLayoutParams(btnParams);
                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (aguardandoTimer) return;
                        try {
                            ex.put("_seriesFeitas", seriesFeitas + 1);
                            if (ex.optInt("_seriesFeitas", 0) >= totalSeries) {
                                ex.put("_done", true);
                                if (!ex.optBoolean("warmup", false)) {
                                    pedirEvolucaoCarga(ex, exercicioAtualIndex);
                                    return;
                                }
                            }
                            salvarProgressoEAtualizar(exercicioAtualIndex);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                card.addView(btnDone);
            } else if (aguardandoTimer) {
                TextView waitText = new TextView(this);
                waitText.setText("Aguardando descanso...");
                waitText.setTextColor(COR_AMARELO);
                waitText.setTextSize(12);
                waitText.setGravity(Gravity.CENTER);
                waitText.setPadding(0, dpToPx(8), 0, 0);
                card.addView(waitText);
            } else if (isDone) {
                TextView doneText = new TextView(this);
                doneText.setText("✓ Concluído");
                doneText.setTextColor(COR_VERDE_CLARO);
                doneText.setTextSize(14);
                doneText.setGravity(Gravity.CENTER);
                doneText.setPadding(0, dpToPx(8), 0, 0);
                card.addView(doneText);
            }
            exercisesContainer.addView(card);

            int total = exercicios.length();
            int done = 0;
            for (int i = 0; i < total; i++) {
                if (exercicios.getJSONObject(i).optInt("_seriesFeitas", 0) >= exercicios.getJSONObject(i).optInt("sets", 1)) {
                    done++;
                }
            }
            int pct = total > 0 ? (done * 100 / total) : 0;
            progressFill.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct));
            progressText.setText(done + "/" + total + " exercícios concluídos");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pedirEvolucaoCarga(final JSONObject ex, final int idx) {
        try {
            final FrameLayout overlay = new FrameLayout(this);
            overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            overlay.setBackgroundColor(COR_FUNDO_OVERLAY);
            overlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {}
            });

            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(COR_CARD);
            box.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
            FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            boxParams.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            box.setLayoutParams(boxParams);

            TextView title = new TextView(this);
            title.setText("Evolução de Carga");
            title.setTextColor(COR_CINZA_MAIS_CLARO);
            title.setTextSize(18);
            title.setGravity(Gravity.CENTER);
            box.addView(title);

            TextView desc = new TextView(this);
            desc.setText("Registre a evolução para " + ex.optString("exercise"));
            desc.setTextColor(COR_CINZA_MEDIO);
            desc.setTextSize(14);
            desc.setPadding(0, dpToPx(6), 0, dpToPx(6));
            box.addView(desc);

            final EditText loadInput = new EditText(this);
            loadInput.setHint("Carga atual (kg)");
            loadInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            loadInput.setText(String.valueOf(ex.optDouble("load", 0)));
            loadInput.setTextColor(COR_CINZA_CLARO);
            loadInput.setBackgroundColor(COR_FUNDO_INPUT);
            box.addView(loadInput);

            final EditText repsInput = new EditText(this);
            repsInput.setHint("Repetições atuais");
            repsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            repsInput.setText(String.valueOf(ex.optInt("reps", 0)));
            repsInput.setTextColor(COR_CINZA_CLARO);
            repsInput.setBackgroundColor(COR_FUNDO_INPUT);
            box.addView(repsInput);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER);
            btnRow.setPadding(0, dpToPx(10), 0, 0);

            Button btnCancel = new Button(this);
            btnCancel.setText("Pular");
            btnCancel.setTextColor(COR_CINZA_CLARO);
            btnCancel.setBackgroundColor(Color.rgb(42, 42, 42));
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((FrameLayout) getWindow().getDecorView()).removeView(overlay);
                    salvarProgressoEAtualizar(idx);
                }
            });
            btnRow.addView(btnCancel);

            Button btnSave = new Button(this);
            btnSave.setText("Registrar");
            btnSave.setTextColor(COR_VERDE_CLARO);
            btnSave.setBackgroundColor(COR_VERDE_ESCURO);
            btnSave.setPadding(dpToPx(14), 0, dpToPx(14), 0);
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        double load = Double.parseDouble(loadInput.getText().toString());
                        int reps = Integer.parseInt(repsInput.getText().toString());
                        String hoje = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                        JSONArray hist = ex.optJSONArray("loadHistory");
                        if (hist == null) hist = new JSONArray();
                        JSONObject reg = new JSONObject();
                        reg.put("load", load);
                        reg.put("reps", reps);
                        reg.put("date", hoje);
                        hist.put(reg);
                        ex.put("loadHistory", hist);
                        ex.put("load", load);
                        ex.put("reps", reps);
                        ((FrameLayout) getWindow().getDecorView()).removeView(overlay);
                        salvarProgressoEAtualizar(idx);
                    } catch (Exception e) {
                        ((FrameLayout) getWindow().getDecorView()).removeView(overlay);
                        salvarProgressoEAtualizar(idx);
                    }
                }
            });
            btnRow.addView(btnSave);
            box.addView(btnRow);
            overlay.addView(box);
            ((FrameLayout) getWindow().getDecorView()).addView(overlay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void salvarProgressoEAtualizar(int idx) {
        try {
            if (configData == null) return;
            String hojeKey = getTodayKey();
            configData.getJSONObject("academia").put("treino_" + hojeKey, treinoAtual);
            saveConfig();
            proximoExercicio(idx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void proximoExercicio(int idxAtual) {
        try {
            JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
            int proximoIdx = -1;
            for (int i = idxAtual + 1; i < exercicios.length(); i++) {
                JSONObject ex = exercicios.getJSONObject(i);
                if (ex.optInt("_seriesFeitas", 0) < ex.optInt("sets", 1)) {
                    proximoIdx = i;
                    break;
                }
            }
            if (proximoIdx == -1) {
                exercicioAtualIndex = exercicios.length();
                renderTreinoCard();
                return;
            }
            exercicioAtualIndex = proximoIdx;
            JSONObject proxEx = exercicios.getJSONObject(proximoIdx);
            int descanso = proxEx.optInt("descanso", 0);
            if (descanso > 0) {
                aguardandoTimer = true;
                renderTreinoCard();
                iniciarTimer(descanso, new Runnable() {
                    @Override
                    public void run() {
                        aguardandoTimer = false;
                        renderTreinoCard();
                    }
                });
            } else {
                renderTreinoCard();
            }
            configData.getJSONObject("academia").put("treino_" + getTodayKey(), treinoAtual);
            configData.getJSONObject("academia").put("botaoAtivo", true);
            saveConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void concluirTreinoCompleto() {
        try {
            if (configData == null) return;
            String hojeKey = getTodayKey();
            configData.getJSONObject("academia").getJSONObject("treinoConcluido").put(hojeKey, true);
            configData.getJSONObject("academia").put("treino_" + hojeKey, JSONObject.NULL);
            configData.getJSONObject("academia").put("botaoAtivo", false);
            saveConfig();
            mostrarConfirmacao("Treino Concluído", "Parabéns! Você concluiu o treino de hoje.", new ConfirmCallback() {
                @Override
                public void onConfirm(boolean ok) {
                    mainBtn.setBackgroundColor(COR_VERMELHO);
                    isActive = false;
                    todayCard.setVisibility(View.GONE);
                    treinoAtual = null;
                    limparTimer();
                    renderDados();
                }
            }, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarTimer(int segundos, final Runnable callback) {
        limparTimer();
        timerRestante = segundos;
        aguardandoTimer = true;
        timerContainer.removeAllViews();

        LinearLayout timerDisplay = new LinearLayout(this);
        timerDisplay.setOrientation(LinearLayout.VERTICAL);
        timerDisplay.setBackgroundColor(COR_FUNDO);
        timerDisplay.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(6), 0, dpToPx(6));
        timerDisplay.setLayoutParams(params);

        TextView label = new TextView(this);
        label.setText("Descanso");
        label.setTextColor(COR_CINZA_ESCURO);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        timerDisplay.addView(label);

        final TextView text = new TextView(this);
        text.setId(View.generateViewId());
        text.setTextColor(COR_VERDE_CLARO);
        text.setTextSize(22);
        text.setGravity(Gravity.CENTER);
        timerDisplay.addView(text);
        timerContainer.addView(timerDisplay);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timerRestante--;
                if (timerRestante <= 0) {
                    limparTimer();
                    if (callback != null) callback.run();
                } else {
                    int m = timerRestante / 60;
                    int s = timerRestante % 60;
                    TextView tv = findViewById(text.getId());
                    if (tv != null) tv.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void limparTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        timerRestante = 0;
        aguardandoTimer = false;
        timerContainer.removeAllViews();
    }

    private void carregarEstadoBotao() {
        try {
            if (configData == null) return;
            String hojeKey = getTodayKey();
            JSONObject academia = configData.getJSONObject("academia");
            boolean temTreino = !academia.isNull("treino_" + hojeKey);
            boolean concluido = academia.getJSONObject("treinoConcluido").optBoolean(hojeKey, false);
            boolean botaoAtivo = academia.optBoolean("botaoAtivo", false);

            if (temTreino && !concluido && botaoAtivo) {
                treinoAtual = academia.getJSONObject("treino_" + hojeKey);
                if (treinoAtual.has("exercicios")) {
                    JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
                    boolean todosConcluidos = true;
                    for (int i = 0; i < exercicios.length(); i++) {
                        JSONObject ex = exercicios.getJSONObject(i);
                        int feitas = ex.optInt("_seriesFeitas", 0);
                        int total = ex.optInt("sets", 1);
                        if (feitas < total) {
                            todosConcluidos = false;
                            exercicioAtualIndex = i;
                            break;
                        }
                    }
                    if (todosConcluidos) {
                        academia.getJSONObject("treinoConcluido").put(hojeKey, true);
                        academia.put("treino_" + hojeKey, JSONObject.NULL);
                        academia.put("botaoAtivo", false);
                        saveConfig();
                        mainBtn.setBackgroundColor(COR_VERMELHO);
                        isActive = false;
                        todayCard.setVisibility(View.GONE);
                        treinoAtual = null;
                        renderDados();
                    } else {
                        mainBtn.setBackgroundColor(COR_VERDE);
                        isActive = true;
                        todayCard.setVisibility(View.VISIBLE);
                        renderTreinoCard();
                        renderDados();
                    }
                }
            } else {
                mainBtn.setBackgroundColor(COR_VERMELHO);
                isActive = false;
                todayCard.setVisibility(View.GONE);
                treinoAtual = null;
                limparTimer();
                if (botaoAtivo) {
                    academia.put("botaoAtivo", false);
                    saveConfig();
                }
                renderDados();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean verificarPeso() {
        try {
            if (configData == null) return false;
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return false;
            String ultimoStr = peso.getString("ultimoRegistro");
            Date ultimo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(ultimoStr);
            if (ultimo == null) return false;
            Date hoje = new Date();
            long diff = (hoje.getTime() - ultimo.getTime()) / (1000 * 60 * 60 * 24);
            return diff >= peso.optInt("intervalo", 7);
        } catch (Exception e) {
            return false;
        }
    }

    private void mostrarAvisoPeso() {
        try {
            if (configData == null || pesoAvisoModal == null) return;
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            String ultimoStr = peso.optString("ultimoRegistro", "");
            String dataFormatada = "Nunca";
            if (!ultimoStr.isEmpty()) {
                Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(ultimoStr);
                if (d != null) dataFormatada = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d);
            }
            long diff = 999;
            if (!ultimoStr.isEmpty()) {
                Date ultimo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(ultimoStr);
                if (ultimo != null) diff = (new Date().getTime() - ultimo.getTime()) / (1000 * 60 * 60 * 24);
            }
            LinearLayout box = (LinearLayout) pesoAvisoModal.getChildAt(0);
            TextView avisoText = (TextView) box.getChildAt(1);
            avisoText.setText("Já faz " + diff + " dias desde a última pesagem (" + dataFormatada + "). Registre seu novo peso.");
            pesoAvisoModal.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarConfirmacao(String titulo, String msg, final ConfirmCallback callback, boolean unicoBotao) {
        try {
            if (confirmModal == null) return;
            LinearLayout box = (LinearLayout) confirmModal.getChildAt(0);
            TextView titleView = (TextView) box.getChildAt(0);
            TextView msgView = (TextView) box.getChildAt(1);
            LinearLayout btnRow = (LinearLayout) box.getChildAt(2);

            titleView.setText(titulo);
            msgView.setText(msg);
            btnRow.removeAllViews();

            if (unicoBotao) {
                Button btnOk = new Button(this);
                btnOk.setText("OK");
                btnOk.setTextColor(COR_VERDE_CLARO);
                btnOk.setBackgroundColor(COR_VERDE_ESCURO);
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmModal.setVisibility(View.GONE);
                        if (callback != null) callback.onConfirm(true);
                    }
                });
                btnRow.addView(btnOk);
            } else {
                Button btnNo = new Button(this);
                btnNo.setText("Não");
                btnNo.setTextColor(COR_CINZA_CLARO);
                btnNo.setBackgroundColor(Color.rgb(42, 42, 42));
                btnNo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmModal.setVisibility(View.GONE);
                        if (callback != null) callback.onConfirm(false);
                    }
                });
                btnRow.addView(btnNo);

                Button btnYes = new Button(this);
                btnYes.setText("Sim");
                btnYes.setTextColor(COR_VERMELHO_CLARO);
                btnYes.setBackgroundColor(Color.rgb(58, 26, 26));
                btnYes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmModal.setVisibility(View.GONE);
                        if (callback != null) callback.onConfirm(true);
                    }
                });
                btnRow.addView(btnYes);
            }
            confirmModal.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarConfirmacao(String titulo, String msg, ConfirmCallback callback) {
        mostrarConfirmacao(titulo, msg, callback, false);
    }

    private void renderDados() {
        try {
            if (dadosContainer == null || configData == null) return;
            dadosContainer.removeAllViews();
            if (isActive) return;

            JSONObject academia = configData.getJSONObject("academia");
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(COR_CARD);
            box.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView headerTitle = new TextView(this);
            headerTitle.setText("Academia");
            headerTitle.setTextColor(COR_CINZA_MEDIO);
            headerTitle.setTextSize(18);
            headerTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            header.addView(headerTitle);

            configBtn = new Button(this);
            configBtn.setText(modoConfig ? "✅ Pronto" : "⚙️ Configurar");
            configBtn.setTextColor(COR_CINZA_CLARO);
            configBtn.setBackgroundColor(Color.rgb(42, 42, 42));
            configBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
            configBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    modoConfig = !modoConfig;
                    renderDados();
                }
            });
            header.addView(configBtn);

            box.addView(header);

            if (modoConfig) {
                renderModoConfig(box, academia);
            } else {
                renderModoVisualizacao(box, academia);
            }

            dadosContainer.addView(box);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderModoVisualizacao(LinearLayout box, JSONObject academia) throws Exception {
        JSONObject peso = academia.getJSONObject("peso");
        String inicioDisplay = academia.optString("inicio", "Não definida");
        if (academia.has("inicio") && !academia.isNull("inicio") && !inicioDisplay.isEmpty()) {
            String[] parts = inicioDisplay.split("-");
            if (parts.length == 3) inicioDisplay = parts[2] + "/" + parts[1] + "/" + parts[0];
        }

        addLabel(box, "Data de Início");
        addValueDisplay(box, inicioDisplay);

        addSubSection(box, "Evolução do Peso");
        String pesoAtual = peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg";
        addValueDisplay(box, "Peso Atual: " + pesoAtual);

        String ultimaPesagem = "Nunca";
        if (peso.has("ultimoRegistro") && !peso.isNull("ultimoRegistro")) {
            String d = peso.getString("ultimoRegistro");
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(d);
                if (date != null) ultimaPesagem = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
            } catch (Exception e) {}
        }
        addValueDisplay(box, "Última pesagem: " + ultimaPesagem);

        if (!peso.isNull("meta")) {
            addValueDisplay(box, "Meta: " + peso.getDouble("meta") + " kg");
        }

        addSubSection(box, "Resumo");
        addValueDisplay(box, "Peso Atual: " + pesoAtual);
        addValueDisplay(box, "Meta: " + (peso.isNull("meta") ? "Não definida" : peso.getDouble("meta") + " kg"));
        addValueDisplay(box, "Intervalo Pesagem: " + peso.optInt("intervalo", 7) + " dias");
        addValueDisplay(box, "Data de início: " + inicioDisplay);

        JSONArray objetivos = academia.getJSONArray("objetivos");
        addSubSection(box, "Objetivos");
        if (objetivos.length() > 0) {
            for (int i = 0; i < objetivos.length(); i++) {
                TextView tv = new TextView(this);
                tv.setText("• " + objetivos.getString(i));
                tv.setTextColor(COR_CINZA_CLARO);
                tv.setTextSize(14);
                tv.setPadding(0, dpToPx(2), 0, dpToPx(2));
                box.addView(tv);
            }
        } else {
            addSmallNote(box, "Nenhum objetivo definido.");
        }

        JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
        addSubSection(box, "Dias de Descanso");
        LinearLayout diasLayout = new LinearLayout(this);
        diasLayout.setOrientation(LinearLayout.HORIZONTAL);
        diasLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        diasLayout.setPadding(0, dpToPx(4), 0, dpToPx(4));
        if (diasDescanso.length() > 0) {
            for (int i = 0; i < diasDescanso.length(); i++) {
                TextView tv = new TextView(this);
                tv.setText(diasDescanso.getString(i));
                tv.setTextColor(COR_CINZA_CLARO);
                tv.setTextSize(12);
                tv.setBackgroundColor(Color.rgb(26, 26, 26));
                tv.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = dpToPx(4);
                tv.setLayoutParams(lp);
                diasLayout.addView(tv);
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("Nenhum dia de descanso");
            tv.setTextColor(COR_CINZA_ESCURO);
            tv.setTextSize(11);
            diasLayout.addView(tv);
        }
        box.addView(diasLayout);

        addSubSection(box, "Roupas");
        JSONObject roupas = academia.getJSONObject("roupas");
        String[] cats = {"camisas", "calcas", "tenis"};
        String[] labels = {"Camisas", "Calças", "Tênis"};
        for (int c = 0; c < cats.length; c++) {
            TextView catLabel = new TextView(this);
            catLabel.setText(labels[c]);
            catLabel.setTextColor(COR_CINZA_ESCURO);
            catLabel.setTextSize(11);
            box.addView(catLabel);
            JSONArray items = roupas.getJSONArray(cats[c]);
            if (items.length() > 0) {
                for (int i = 0; i < items.length(); i++) {
                    LinearLayout itemLayout = new LinearLayout(this);
                    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                    itemLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    itemLayout.setBackgroundColor(COR_FUNDO);
                    itemLayout.setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3));
                    TextView itemText = new TextView(this);
                    itemText.setText(items.getString(i));
                    itemText.setTextColor(COR_CINZA_CLARO);
                    itemText.setTextSize(12);
                    itemText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    itemLayout.addView(itemText);
                    box.addView(itemLayout);
                }
            } else {
                addSmallNote(box, "Nenhuma " + labels[c].toLowerCase() + ".");
            }
        }

        addSubSection(box, "Combinações");
        JSONObject combinacoes = academia.getJSONObject("combinacoes");
        for (String dia : DIAS_SEMANA) {
            if (combinacoes.has(dia)) {
                JSONArray combos = combinacoes.getJSONArray(dia);
                TextView diaLabel = new TextView(this);
                diaLabel.setText(dia + ":");
                diaLabel.setTextColor(COR_CINZA_MEDIO);
                diaLabel.setTextSize(13);
                diaLabel.setPadding(0, dpToPx(4), 0, dpToPx(2));
                box.addView(diaLabel);
                for (int i = 0; i < combos.length(); i++) {
                    TextView tv = new TextView(this);
                    tv.setText("  " + combos.getString(i));
                    tv.setTextColor(COR_CINZA_CLARO);
                    tv.setTextSize(12);
                    box.addView(tv);
                }
            }
        }
    }

    private void renderModoConfig(LinearLayout box, JSONObject academia) throws Exception {
        JSONObject peso = academia.getJSONObject("peso");
        String inicioDisplay = academia.optString("inicio", "");
        if (academia.has("inicio") && !academia.isNull("inicio") && !inicioDisplay.isEmpty()) {
            String[] parts = inicioDisplay.split("-");
            if (parts.length == 3) inicioDisplay = parts[2] + "/" + parts[1] + "/" + parts[0];
        }

        addSubSection(box, "Data de Início");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tv = new TextView(this);
        tv.setText(inicioDisplay.isEmpty() ? "Não definida" : inicioDisplay);
        tv.setTextColor(COR_CINZA_CLARO);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tv);
        Button editBtn = new Button(this);
        editBtn.setText(academia.isNull("inicio") ? "Definir" : "Editar");
        editBtn.setTextColor(COR_VERDE_CLARO);
        editBtn.setBackgroundColor(COR_VERDE_ESCURO);
        editBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSubModal("<h3>Data de Início</h3><input type=\"date\" id=\"editInicioInput\" value=\"" + academia.optString("inicio", "") + "\" /><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelInicioBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveInicioBtn\">Salvar</button></div>");
            }
        });
        row.addView(editBtn);
        box.addView(row);

        addSubSection(box, "Peso");
        String pesoAtual = peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg";
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv = new TextView(this);
        tv.setText("Atual: " + pesoAtual);
        tv.setTextColor(COR_CINZA_CLARO);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tv);
        Button pesoBtn = new Button(this);
        pesoBtn.setText("Registrar");
        pesoBtn.setTextColor(COR_VERDE_CLARO);
        pesoBtn.setBackgroundColor(COR_VERDE_ESCURO);
        pesoBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        pesoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSubModal("<h3>Registrar Peso</h3><label>Peso (kg)</label><input type=\"number\" step=\"0.1\" id=\"pesoInput\" /><label>Meta (opcional)</label><input type=\"number\" step=\"0.1\" id=\"pesoMetaInput\" value=\"" + (peso.isNull("meta") ? "" : peso.getDouble("meta")) + "\" /><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelPesoBtn\">Cancelar</button><button class=\"btn-primary\" id=\"savePesoBtn\">Salvar</button></div>");
            }
        });
        row.addView(pesoBtn);
        box.addView(row);

        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv = new TextView(this);
        tv.setText("Intervalo: " + peso.optInt("intervalo", 7) + " dias");
        tv.setTextColor(COR_CINZA_CLARO);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tv);
        editBtn = new Button(this);
        editBtn.setText("Editar");
        editBtn.setTextColor(COR_VERDE_CLARO);
        editBtn.setBackgroundColor(COR_VERDE_ESCURO);
        editBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSubModal("<h3>Intervalo para Pesagem</h3><input type=\"number\" id=\"editIntervaloInput\" value=\"" + peso.optInt("intervalo", 7) + "\" /><div class=\"small-note\">A cada quantos dias você deve pesar?</div><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelIntervaloBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveIntervaloBtn\">Salvar</button></div>");
            }
        });
        row.addView(editBtn);
        box.addView(row);

        addSubSection(box, "Dias de Descanso");
        JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
        for (String dia : DIAS_SEMANA) {
            CheckBox cb = new CheckBox(this);
            cb.setText(dia);
            cb.setTextColor(COR_CINZA_MEDIO);
            boolean checked = false;
            for (int i = 0; i < diasDescanso.length(); i++) {
                if (diasDescanso.getString(i).equals(dia)) { checked = true; break; }
            }
            cb.setChecked(checked);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    try {
                        JSONArray novos = new JSONArray();
                        LinearLayout parent = (LinearLayout) buttonView.getParent();
                        for (int i = 0; i < parent.getChildCount(); i++) {
                            View child = parent.getChildAt(i);
                            if (child instanceof CheckBox) {
                                if (((CheckBox) child).isChecked()) novos.put(((CheckBox) child).getText().toString());
                            }
                        }
                        academia.put("diasDescanso", novos);
                        saveConfig();
                    } catch (Exception e) {}
                }
            });
            box.addView(cb);
        }

        addSubSection(box, "Objetivos");
        JSONArray objetivos = academia.getJSONArray("objetivos");
        for (int i = 0; i < objetivos.length(); i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            item.setBackgroundColor(COR_FUNDO);
            item.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));
            TextView objText = new TextView(this);
            objText.setText(objetivos.getString(i));
            objText.setTextColor(COR_CINZA_CLARO);
            objText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            item.addView(objText);
            Button delBtn = new Button(this);
            delBtn.setText("✕");
            delBtn.setTextColor(COR_VERMELHO_CLARO);
            delBtn.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
            final int idx = i;
            delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarConfirmacao("Excluir Objetivo", "Tem certeza que deseja excluir este objetivo?", new ConfirmCallback() {
                        @Override
                        public void onConfirm(boolean ok) {
                            if (ok) {
                                try {
                                    objetivos.remove(idx);
                                    saveConfig();
                                } catch (Exception e) {}
                            }
                        }
                    });
                }
            });
            item.addView(delBtn);
            box.addView(item);
        }
        Button addObj = new Button(this);
        addObj.setText("+ Adicionar Objetivo");
        addObj.setTextColor(COR_VERDE_CLARO);
        addObj.setBackgroundColor(COR_VERDE_ESCURO);
        addObj.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        addObj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSubModal("<h3>Novo Objetivo</h3><input type=\"text\" id=\"novoObjetivoInput\" placeholder=\"Digite o objetivo\" /><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelObjBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveObjBtn\">Salvar</button></div>");
            }
        });
        box.addView(addObj);

        addSubSection(box, "Treinos");
        JSONArray treinos = academia.getJSONArray("treinos");
        for (int i = 0; i < treinos.length(); i++) {
            JSONObject treino = treinos.getJSONObject(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackgroundColor(COR_FUNDO);
            item.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            item.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView treinoName = new TextView(this);
            treinoName.setText(treino.optString("nome", "Treino") + " (" + treino.optString("dia", "") + ")");
            treinoName.setTextColor(COR_CINZA_CLARO);
            treinoName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            headerRow.addView(treinoName);
            Button delTreino = new Button(this);
            delTreino.setText("✕");
            delTreino.setTextColor(COR_VERMELHO_CLARO);
            delTreino.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
            final int treinoIdx = i;
            delTreino.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarConfirmacao("Excluir Treino", "Tem certeza que deseja excluir este treino?", new ConfirmCallback() {
                        @Override
                        public void onConfirm(boolean ok) {
                            if (ok) {
                                try {
                                    treinos.remove(treinoIdx);
                                    saveConfig();
                                } catch (Exception e) {}
                            }
                        }
                    });
                }
            });
            headerRow.addView(delTreino);
            item.addView(headerRow);

            JSONArray exs = treino.getJSONArray("exercicios");
            for (int j = 0; j < exs.length(); j++) {
                JSONObject ex = exs.getJSONObject(j);
                TextView exText = new TextView(this);
                exText.setText("  " + ex.optString("exercise") + " - " + ex.optInt("sets") + "x" + ex.optInt("reps") + " " + ex.optDouble("load") + "kg");
                exText.setTextColor(COR_CINZA_MEDIO);
                exText.setTextSize(12);
                item.addView(exText);
            }

            Button addEx = new Button(this);
            addEx.setText("+ Exercício");
            addEx.setTextColor(COR_VERDE_CLARO);
            addEx.setBackgroundColor(COR_VERDE_ESCURO);
            addEx.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            addEx.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSubModal("<h3>Adicionar Exercício</h3><label>Exercício *</label><input type=\"text\" id=\"exNome\" placeholder=\"Ex: Supino\" /><div class=\"form-row\"><div class=\"form-group\"><label>Séries *</label><input type=\"number\" id=\"exSets\" value=\"3\" /></div><div class=\"form-group\"><label>Repetições *</label><input type=\"number\" id=\"exReps\" value=\"10\" /></div><div class=\"form-group\"><label>Carga (kg) *</label><input type=\"number\" step=\"0.5\" id=\"exLoad\" /></div></div><label>Meta de Carga (kg, opcional)</label><input type=\"number\" step=\"0.5\" id=\"exMetaCarga\" placeholder=\"Ex: 30\" /><label>Descanso entre séries (opcional)</label><div style=\"display:flex;gap:4px;align-items:center;\"><input type=\"number\" id=\"exDescansoMin\" placeholder=\"Min\" style=\"width:60px;\" /><span style=\"color:#888;\">:</span><input type=\"number\" id=\"exDescansoSec\" placeholder=\"Seg\" style=\"width:60px;\" /></div><div class=\"checkbox-row\"><input type=\"checkbox\" id=\"exWarmup\" /><label for=\"exWarmup\">Série de aquecimento</label></div><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelExBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveExBtn\">Salvar</button></div>");
                }
            });
            item.addView(addEx);
            box.addView(item);
        }

        Button addTreino = new Button(this);
        addTreino.setText("+ Adicionar Treino");
        addTreino.setTextColor(COR_VERDE_CLARO);
        addTreino.setBackgroundColor(COR_VERDE_ESCURO);
        addTreino.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        addTreino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String options = "";
                for (String d : DIAS_SEMANA) options += "<option value=\"" + d + "\">" + d + "</option>";
                openSubModal("<h3>Novo Treino</h3><label>Nome</label><input type=\"text\" id=\"novoTreinoNome\" placeholder=\"Nome do treino\" /><label>Dia *</label><select id=\"novoTreinoDia\" style=\"width:100%;margin:3px 0;background:#0a0a0a;border:1px solid #2a2a2a;color:#ddd;padding:6px;border-radius:4px;\"><option value=\"\">Selecione um dia</option>" + options + "</select><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelTreinoBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveTreinoBtn\">Salvar</button></div>");
            }
        });
        box.addView(addTreino);

        addSubSection(box, "Roupas");
        JSONObject roupas = academia.getJSONObject("roupas");
        String[] cats = {"camisas", "calcas", "tenis"};
        String[] labels = {"Camisas", "Calças", "Tênis"};
        for (int c = 0; c < cats.length; c++) {
            TextView catLabel = new TextView(this);
            catLabel.setText(labels[c]);
            catLabel.setTextColor(COR_CINZA_ESCURO);
            catLabel.setTextSize(11);
            catLabel.setPadding(0, dpToPx(6), 0, 0);
            box.addView(catLabel);
            JSONArray items = roupas.getJSONArray(cats[c]);
            for (int i = 0; i < items.length(); i++) {
                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                itemLayout.setBackgroundColor(COR_FUNDO);
                itemLayout.setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3));
                TextView itemText = new TextView(this);
                itemText.setText(items.getString(i));
                itemText.setTextColor(COR_CINZA_CLARO);
                itemText.setTextSize(12);
                itemText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                itemLayout.addView(itemText);
                final String cat = cats[c];
                final int idx = i;
                Button delBtn = new Button(this);
                delBtn.setText("✕");
                delBtn.setTextColor(COR_VERMELHO_CLARO);
                delBtn.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
                delBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mostrarConfirmacao("Excluir Roupa", "Tem certeza que deseja excluir esta roupa?", new ConfirmCallback() {
                            @Override
                            public void onConfirm(boolean ok) {
                                if (ok) {
                                    try {
                                        roupas.getJSONArray(cat).remove(idx);
                                        saveConfig();
                                    } catch (Exception e) {}
                                }
                            }
                        });
                    }
                });
                itemLayout.addView(delBtn);
                box.addView(itemLayout);
            }
            Button addRoupa = new Button(this);
            addRoupa.setText("+ Adicionar " + labels[c].toLowerCase());
            addRoupa.setTextColor(COR_VERDE_CLARO);
            addRoupa.setBackgroundColor(COR_VERDE_ESCURO);
            addRoupa.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            final String catFinal = cats[c];
            addRoupa.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSubModal("<h3>Nova " + labels[c] + "</h3><input type=\"text\" id=\"novaRoupaInput\" placeholder=\"Nome da " + labels[c].toLowerCase() + "\" /><div class=\"checkbox-row\"><input type=\"checkbox\" id=\"roupaSweat\" /><label for=\"roupaSweat\">Marca suor</label></div><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelRoupaBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveRoupaBtn\">Salvar</button></div>");
                }
            });
            box.addView(addRoupa);
        }

        addSubSection(box, "Combinações");
        JSONObject combinacoes = academia.getJSONObject("combinacoes");
        for (String dia : DIAS_SEMANA) {
            boolean isDescanso = false;
            for (int i = 0; i < diasDescanso.length(); i++) {
                if (diasDescanso.getString(i).equals(dia)) { isDescanso = true; break; }
            }
            if (isDescanso) continue;
            TextView diaLabel = new TextView(this);
            diaLabel.setText(dia + ":");
            diaLabel.setTextColor(COR_CINZA_MEDIO);
            diaLabel.setTextSize(13);
            diaLabel.setPadding(0, dpToPx(4), 0, dpToPx(2));
            box.addView(diaLabel);

            if (combinacoes.has(dia)) {
                JSONArray combos = combinacoes.getJSONArray(dia);
                for (int i = 0; i < combos.length(); i++) {
                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.HORIZONTAL);
                    item.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    item.setBackgroundColor(COR_FUNDO);
                    item.setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3));
                    TextView comboText = new TextView(this);
                    comboText.setText(combos.getString(i));
                    comboText.setTextColor(COR_CINZA_CLARO);
                    comboText.setTextSize(12);
                    comboText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    item.addView(comboText);
                    final String diaFinal = dia;
                    final int comboIdx = i;
                    Button delBtn = new Button(this);
                    delBtn.setText("✕");
                    delBtn.setTextColor(COR_VERMELHO_CLARO);
                    delBtn.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
                    delBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mostrarConfirmacao("Excluir Combinação", "Tem certeza que deseja excluir esta combinação?", new ConfirmCallback() {
                                @Override
                                public void onConfirm(boolean ok) {
                                    if (ok) {
                                        try {
                                            JSONArray c = combinacoes.getJSONArray(diaFinal);
                                            c.remove(comboIdx);
                                            if (c.length() == 0) combinacoes.remove(diaFinal);
                                            saveConfig();
                                        } catch (Exception e) {}
                                    }
                                }
                            });
                        }
                    });
                    item.addView(delBtn);
                    box.addView(item);
                }
            }

            LinearLayout comboRow = new LinearLayout(this);
            comboRow.setOrientation(LinearLayout.HORIZONTAL);
            comboRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            comboRow.setPadding(0, dpToPx(4), 0, 0);
            String[] catsRoupa = {"camisas", "calcas", "tenis"};
            String[] labelsRoupa = {"Camisa", "Calça", "Tênis"};
            for (int c = 0; c < catsRoupa.length; c++) {
                final LinearLayout spinLayout = new LinearLayout(this);
                spinLayout.setOrientation(LinearLayout.VERTICAL);
                spinLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                TextView spinLabel = new TextView(this);
                spinLabel.setText(labelsRoupa[c]);
                spinLabel.setTextColor(COR_CINZA_ESCURO);
                spinLabel.setTextSize(10);
                spinLayout.addView(spinLabel);
                final Button spinBtn = new Button(this);
                spinBtn.setText("Selecionar");
                spinBtn.setTextColor(COR_CINZA_CLARO);
                spinBtn.setBackgroundColor(COR_FUNDO_INPUT);
                spinBtn.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
                final String catR = catsRoupa[c];
                final String diaF = dia;
                spinBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            JSONArray items = roupas.getJSONArray(catR);
                            if (items.length() == 0) {
                                Toast.makeText(MainActivity.this, "Nenhuma " + labelsRoupa[c] + " disponível", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String options = "";
                            for (int i = 0; i < items.length(); i++) {
                                options += "<option value=\"" + items.getString(i) + "\">" + items.getString(i) + "</option>";
                            }
                            openSubModal("<h3>Selecionar " + labelsRoupa[c] + "</h3><select id=\"selectRoupa_" + catR + "\" style=\"width:100%;margin:3px 0;background:#0a0a0a;border:1px solid #2a2a2a;color:#ddd;padding:6px;border-radius:4px;\">" + options + "</select><div class=\"btn-row\"><button class=\"btn-secondary\" id=\"cancelSelectBtn\">Cancelar</button><button class=\"btn-primary\" id=\"saveSelectBtn\">Selecionar</button></div>");
                        } catch (Exception e) {}
                    }
                });
                spinLayout.addView(spinBtn);
                comboRow.addView(spinLayout);
            }
            Button addComboBtn = new Button(this);
            addComboBtn.setText("Adicionar");
            addComboBtn.setTextColor(COR_VERDE_CLARO);
            addComboBtn.setBackgroundColor(COR_VERDE_ESCURO);
            addComboBtn.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));
            addComboBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addComboBtn.setGravity(Gravity.CENTER);
            comboRow.addView(addComboBtn);
            box.addView(comboRow);
        }
    }

    private void addLabel(LinearLayout box, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(COR_CINZA_ESCURO);
        tv.setTextSize(12);
        tv.setPadding(0, dpToPx(8), 0, dpToPx(2));
        box.addView(tv);
    }

    private void addValueDisplay(LinearLayout box, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(COR_CINZA_CLARO);
        tv.setTextSize(14);
        tv.setBackgroundColor(COR_FUNDO);
        tv.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
        box.addView(tv);
    }

    private void addSubSection(LinearLayout box, String title) throws Exception {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.rgb(153, 153, 153));
        tv.setTextSize(14);
        tv.setPadding(0, dpToPx(12), 0, dpToPx(4));
        box.addView(tv);
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        line.setBackgroundColor(Color.rgb(42, 42, 42));
        box.addView(line);
    }

    private void addSmallNote(LinearLayout box, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(COR_CINZA_ESCURO);
        tv.setTextSize(11);
        tv.setPadding(0, dpToPx(3), 0, dpToPx(3));
        box.addView(tv);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    interface ConfirmCallback {
        void onConfirm(boolean ok);
    }
}
EOF
