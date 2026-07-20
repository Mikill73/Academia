mkdir -p src/main/java/com/academia/app
cat > src/main/java/com/academia/app/MainActivity.java << 'EOF'
package com.academia.app;

import android.app.Activity;
import android.content.SharedPreferences;
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

    private LinearLayout mainLayout;
    private LinearLayout menuPanel;
    private FrameLayout menuOverlay;
    private FrameLayout configModal;
    private LinearLayout configContent;
    private FrameLayout pesoAvisoModal;
    private FrameLayout confirmModal;
    private LinearLayout todayCard;
    private LinearLayout exercisesContainer;
    private LinearLayout timerContainer;
    private View progressFill;
    private TextView progressText;
    private Button mainBtn;

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
    private static final int COR_FUNDO_OVERLAY = Color.argb(136, 0, 0, 0);
    private static final int COR_FUNDO_TRANSPARENTE = Color.argb(0, 0, 0, 0);
    private static final int COR_CINZA_MAIS_CLARO = Color.rgb(238, 238, 238);
    private static final int COR_FUNDO_INPUT = Color.rgb(10, 10, 10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            prefs = getSharedPreferences("AcademiaPrefs", MODE_PRIVATE);
            timerHandler = new Handler(Looper.getMainLooper());
            loadConfig();
            setupUI();
            carregarEstadoBotao();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupUI() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(COR_FUNDO);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Button hamburger = new Button(this);
        hamburger.setText("☰");
        hamburger.setTextColor(COR_BRANCO);
        hamburger.setTextSize(28);
        hamburger.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
        LinearLayout.LayoutParams hamburgerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hamburgerParams.setMargins(dpToPx(16), dpToPx(16), 0, 0);
        hamburger.setLayoutParams(hamburgerParams);
        hamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu(true);
            }
        });
        mainLayout.addView(hamburger);

        mainBtn = new Button(this);
        LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(dpToPx(130), dpToPx(130));
        mainParams.gravity = Gravity.CENTER_HORIZONTAL;
        mainParams.topMargin = dpToPx(40);
        mainBtn.setLayoutParams(mainParams);
        mainBtn.setBackgroundDrawable(new ShapeDrawable(new OvalShape()));
        mainBtn.setBackgroundColor(COR_VERMELHO);
        mainBtn.setText("INICIAR");
        mainBtn.setTextColor(COR_BRANCO);
        mainBtn.setTextSize(14);
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
        cardParams.setMargins(dpToPx(10), dpToPx(20), dpToPx(10), 0);
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

        menuOverlay = new FrameLayout(this);
        menuOverlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        menuOverlay.setBackgroundColor(COR_FUNDO_OVERLAY);
        menuOverlay.setVisibility(View.GONE);
        menuOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu(false);
            }
        });

        menuPanel = new LinearLayout(this);
        menuPanel.setOrientation(LinearLayout.VERTICAL);
        menuPanel.setBackgroundColor(COR_CARD);
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dpToPx(280), ViewGroup.LayoutParams.MATCH_PARENT);
        menuParams.gravity = Gravity.START;
        menuPanel.setLayoutParams(menuParams);
        menuPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });

        Button closeMenu = new Button(this);
        closeMenu.setText("✕");
        closeMenu.setTextColor(COR_CINZA_ESCURO);
        closeMenu.setTextSize(28);
        closeMenu.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
        closeMenu.setGravity(Gravity.END);
        closeMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu(false);
            }
        });
        menuPanel.addView(closeMenu);

        Button menuAcademia = new Button(this);
        menuAcademia.setText("Academia");
        menuAcademia.setTextColor(COR_CINZA_CLARO);
        menuAcademia.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
        menuAcademia.setGravity(Gravity.START);
        menuAcademia.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        menuAcademia.setTextSize(18);
        menuAcademia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu(false);
                openConfig(true);
            }
        });
        menuPanel.addView(menuAcademia);

        Button menuConfig = new Button(this);
        menuConfig.setText("Configurações");
        menuConfig.setTextColor(COR_CINZA_CLARO);
        menuConfig.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
        menuConfig.setGravity(Gravity.START);
        menuConfig.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        menuConfig.setTextSize(18);
        menuConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu(false);
                openConfig(false);
            }
        });
        menuPanel.addView(menuConfig);

        menuOverlay.addView(menuPanel);
        root.addView(mainLayout);
        root.addView(menuOverlay);

        setupConfigModal(root);
        setupPesoAvisoModal(root);
        setupConfirmModal(root);

        setContentView(root);
    }

    private void setupConfigModal(FrameLayout root) {
        configModal = new FrameLayout(this);
        configModal.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        configModal.setBackgroundColor(COR_FUNDO_OVERLAY);
        configModal.setVisibility(View.GONE);

        LinearLayout configBox = new LinearLayout(this);
        configBox.setOrientation(LinearLayout.VERTICAL);
        configBox.setBackgroundColor(COR_CARD);
        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.gravity = Gravity.CENTER;
        boxParams.setMargins(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        configBox.setLayoutParams(boxParams);

        LinearLayout configHeader = new LinearLayout(this);
        configHeader.setOrientation(LinearLayout.HORIZONTAL);
        configHeader.setGravity(Gravity.CENTER_VERTICAL);
        configHeader.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        Button backBtn = new Button(this);
        backBtn.setText("←");
        backBtn.setTextColor(COR_CINZA_ESCURO);
        backBtn.setTextSize(28);
        backBtn.setBackgroundColor(COR_FUNDO_TRANSPARENTE);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConfig();
            }
        });
        configHeader.addView(backBtn);

        TextView configTitleView = new TextView(this);
        configTitleView.setText("Academia");
        configTitleView.setTextColor(COR_CINZA_MEDIO);
        configTitleView.setTextSize(18);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.gravity = Gravity.CENTER;
        configTitleView.setLayoutParams(titleParams);
        configHeader.addView(configTitleView);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(30), ViewGroup.LayoutParams.WRAP_CONTENT));
        configHeader.addView(spacer);
        configBox.addView(configHeader);

        configContent = new LinearLayout(this);
        configContent.setOrientation(LinearLayout.VERTICAL);
        configContent.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        configBox.addView(configContent);

        configModal.addView(configBox);
        configModal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConfig();
            }
        });
        configBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {}
        });
        root.addView(configModal);
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
        avisoParams.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
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

    private void toggleMenu(boolean open) {
        if (menuOverlay != null) {
            menuOverlay.setVisibility(open ? View.VISIBLE : View.GONE);
        }
    }

    private void openConfig(boolean viewOnly) {
        if (configModal != null) {
            configModal.setVisibility(View.VISIBLE);
            renderConfig(viewOnly);
        }
    }

    private void closeConfig() {
        if (configModal != null) {
            configModal.setVisibility(View.GONE);
        }
    }

    private void loadConfig() {
        try {
            String json = prefs.getString("academia_data", null);
            if (json != null && !json.isEmpty()) {
                configData = new JSONObject(json);
            } else {
                createDefaultConfig();
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
            academia.put("inicio", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            JSONObject peso = new JSONObject();
            peso.put("atual", 70.0);
            peso.put("historico", new JSONArray());
            JSONObject histEntry = new JSONObject();
            histEntry.put("peso", 70.0);
            histEntry.put("data", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            peso.getJSONArray("historico").put(histEntry);
            peso.put("meta", 65.0);
            peso.put("intervalo", 7);
            peso.put("ultimoRegistro", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));
            academia.put("peso", peso);
            JSONArray diasDescanso = new JSONArray();
            diasDescanso.put("Domingo");
            academia.put("diasDescanso", diasDescanso);
            academia.put("objetivos", new JSONArray());
            JSONObject roupas = new JSONObject();
            roupas.put("camisas", new JSONArray());
            roupas.put("calcas", new JSONArray());
            roupas.put("tenis", new JSONArray());
            academia.put("roupas", roupas);
            academia.put("combinacoes", new JSONObject());
            JSONArray treinos = new JSONArray();
            JSONObject treinoSegunda = new JSONObject();
            treinoSegunda.put("dia", "Segunda");
            JSONArray exerciciosSegunda = new JSONArray();
            JSONObject ex1 = new JSONObject();
            ex1.put("exercise", "Supino Reto");
            ex1.put("sets", 4);
            ex1.put("reps", 10);
            ex1.put("load", 50.0);
            ex1.put("warmup", false);
            ex1.put("descanso", 60);
            ex1.put("loadHistory", new JSONArray());
            JSONObject histLoad = new JSONObject();
            histLoad.put("load", 50.0);
            histLoad.put("reps", 10);
            histLoad.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            ex1.getJSONArray("loadHistory").put(histLoad);
            exerciciosSegunda.put(ex1);
            JSONObject ex2 = new JSONObject();
            ex2.put("exercise", "Crucifixo");
            ex2.put("sets", 3);
            ex2.put("reps", 12);
            ex2.put("load", 15.0);
            ex2.put("warmup", false);
            ex2.put("descanso", 45);
            ex2.put("loadHistory", new JSONArray());
            JSONObject histLoad2 = new JSONObject();
            histLoad2.put("load", 15.0);
            histLoad2.put("reps", 12);
            histLoad2.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            ex2.getJSONArray("loadHistory").put(histLoad2);
            exerciciosSegunda.put(ex2);
            treinoSegunda.put("exercicios", exerciciosSegunda);
            treinos.put(treinoSegunda);

            JSONObject treinoQuarta = new JSONObject();
            treinoQuarta.put("dia", "Quarta");
            JSONArray exerciciosQuarta = new JSONArray();
            JSONObject ex3 = new JSONObject();
            ex3.put("exercise", "Puxada Frontal");
            ex3.put("sets", 4);
            ex3.put("reps", 10);
            ex3.put("load", 40.0);
            ex3.put("warmup", false);
            ex3.put("descanso", 60);
            ex3.put("loadHistory", new JSONArray());
            JSONObject histLoad3 = new JSONObject();
            histLoad3.put("load", 40.0);
            histLoad3.put("reps", 10);
            histLoad3.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            ex3.getJSONArray("loadHistory").put(histLoad3);
            exerciciosQuarta.put(ex3);
            treinoQuarta.put("exercicios", exerciciosQuarta);
            treinos.put(treinoQuarta);

            academia.put("treinos", treinos);
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
                prefs.edit().putString("academia_data", configData.toString()).apply();
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

    private JSONArray getTodayTreinos() {
        try {
            if (configData == null) return new JSONArray();
            String hoje = getTodayName();
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONArray result = new JSONArray();
            for (int i = 0; i < treinos.length(); i++) {
                if (treinos.getJSONObject(i).getString("dia").equals(hoje)) {
                    result.put(treinos.getJSONObject(i));
                }
            }
            return result;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void handleMainBtnClick() {
        if (isActive) {
            mostrarConfirmacao("Parar Treino", "Deseja realmente parar?", new ConfirmCallback() {
                @Override
                public void onConfirm(boolean ok) {
                    if (ok) {
                        pararTreino();
                    }
                }
            });
        } else {
            JSONArray treinos = getTodayTreinos();
            if (treinos.length() == 0) {
                mostrarConfirmacao("Aviso", "Nenhum treino programado para hoje.", new ConfirmCallback() {
                    @Override
                    public void onConfirm(boolean ok) {}
                }, true);
                return;
            }
            iniciarTreino();
        }
    }

    private void iniciarTreino() {
        try {
            if (configData == null) return;
            final String hojeKey = getTodayKey();
            final JSONObject treinoConcluido = configData.getJSONObject("academia").getJSONObject("treinoConcluido");
            if (treinoConcluido.optBoolean(hojeKey, false)) {
                mostrarConfirmacao("Treino Concluído", "Deseja refazê-lo?", new ConfirmCallback() {
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
            if (configData.getJSONObject("academia").has("treino_" + hojeKey)) {
                treinoAtual = configData.getJSONObject("academia").getJSONObject("treino_" + hojeKey);
                exercicioAtualIndex = 0;
                todayCard.setVisibility(View.VISIBLE);
                mainBtn.setBackgroundColor(COR_VERDE);
                mainBtn.setText("PARAR");
                isActive = true;
                configData.getJSONObject("academia").put("botaoAtivo", true);
                saveConfig();
                renderTreinoCard();
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
            JSONArray treinos = getTodayTreinos();
            if (treinos.length() == 0) return;
            treinoAtual = new JSONObject(treinos.getJSONObject(0).toString());
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
            exercicioAtualIndex = 0;
            todayCard.setVisibility(View.VISIBLE);
            mainBtn.setBackgroundColor(COR_VERDE);
            mainBtn.setText("PARAR");
            isActive = true;
            configData.getJSONObject("academia").put("botaoAtivo", true);
            configData.getJSONObject("academia").put("treino_" + getTodayKey(), treinoAtual);
            saveConfig();
            renderTreinoCard();
            if (verificarPeso()) mostrarAvisoPeso();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pararTreino() {
        try {
            mainBtn.setBackgroundColor(COR_VERMELHO);
            mainBtn.setText("INICIAR");
            isActive = false;
            todayCard.setVisibility(View.GONE);
            treinoAtual = null;
            limparTimer();
            if (configData != null) {
                configData.getJSONObject("academia").put("treino_" + getTodayKey(), JSONObject.NULL);
                configData.getJSONObject("academia").put("botaoAtivo", false);
                saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    mainBtn.setText("INICIAR");
                    isActive = false;
                    todayCard.setVisibility(View.GONE);
                    treinoAtual = null;
                } else {
                    mainBtn.setBackgroundColor(COR_VERDE);
                    mainBtn.setText("PARAR");
                    isActive = true;
                    todayCard.setVisibility(View.VISIBLE);
                    renderTreinoCard();
                }
            } else {
                mainBtn.setBackgroundColor(COR_VERMELHO);
                mainBtn.setText("INICIAR");
                isActive = false;
                todayCard.setVisibility(View.GONE);
                treinoAtual = null;
                if (botaoAtivo) {
                    academia.put("botaoAtivo", false);
                    saveConfig();
                }
            }
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
            name.setText(ex.optString("exercise", "Exercício") + (ex.optBoolean("warmup", false) ? " (Aquecimento)" : ""));
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
                    mainBtn.setText("INICIAR");
                    isActive = false;
                    todayCard.setVisibility(View.GONE);
                    treinoAtual = null;
                    limparTimer();
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

    private void registrarPeso(double val) {
        try {
            if (configData == null) return;
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
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

    private void renderConfig(final boolean viewOnly) {
        try {
            if (configContent == null || configData == null) return;
            configContent.removeAllViews();

            final JSONObject academia = configData.getJSONObject("academia");
            TextView sectionTitle = new TextView(this);
            sectionTitle.setText("Resumo");
            sectionTitle.setTextColor(Color.rgb(153, 153, 153));
            sectionTitle.setTextSize(14);
            sectionTitle.setPadding(0, dpToPx(10), 0, dpToPx(5));
            configContent.addView(sectionTitle);

            addConfigRow("Data de Início", academia.optString("inicio", "Não definida"));
            
            JSONObject peso = academia.getJSONObject("peso");
            addConfigRow("Peso Atual", peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg");
            addConfigRow("Meta", peso.isNull("meta") ? "Não definida" : peso.getDouble("meta") + " kg");
            addConfigRow("Intervalo Pesagem", peso.optInt("intervalo", 7) + " dias");

            if (!viewOnly) {
                Button btnPeso = new Button(this);
                btnPeso.setText("Registrar Peso");
                btnPeso.setTextColor(COR_VERDE_CLARO);
                btnPeso.setBackgroundColor(COR_VERDE_ESCURO);
                btnPeso.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnPeso.setPadding(0, dpToPx(8), 0, dpToPx(8));
                btnPeso.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final EditText input = new EditText(MainActivity.this);
                        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        input.setHint("Peso (kg)");
                        input.setTextColor(COR_CINZA_CLARO);
                        input.setBackgroundColor(COR_FUNDO_INPUT);
                        
                        final FrameLayout overlay = new FrameLayout(MainActivity.this);
                        overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        overlay.setBackgroundColor(COR_FUNDO_OVERLAY);
                        overlay.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View ev) {}
                        });
                        
                        LinearLayout box = new LinearLayout(MainActivity.this);
                        box.setOrientation(LinearLayout.VERTICAL);
                        box.setBackgroundColor(COR_CARD);
                        box.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
                        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                        box.setLayoutParams(boxParams);
                        
                        TextView title = new TextView(MainActivity.this);
                        title.setText("Registrar Peso");
                        title.setTextColor(COR_CINZA_MAIS_CLARO);
                        title.setTextSize(18);
                        title.setGravity(Gravity.CENTER);
                        box.addView(title);
                        box.addView(input);
                        
                        Button saveBtn = new Button(MainActivity.this);
                        saveBtn.setText("Salvar");
                        saveBtn.setTextColor(COR_VERDE_CLARO);
                        saveBtn.setBackgroundColor(COR_VERDE_ESCURO);
                        saveBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View ev) {
                                try {
                                    double val = Double.parseDouble(input.getText().toString());
                                    registrarPeso(val);
                                    ((FrameLayout) getWindow().getDecorView()).removeView(overlay);
                                    renderConfig(false);
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Inválido", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        box.addView(saveBtn);
                        overlay.addView(box);
                        ((FrameLayout) getWindow().getDecorView()).addView(overlay);
                    }
                });
                configContent.addView(btnPeso);
            }

            sectionTitle = new TextView(this);
            sectionTitle.setText("Dias de Descanso");
            sectionTitle.setTextColor(Color.rgb(153, 153, 153));
            sectionTitle.setTextSize(14);
            sectionTitle.setPadding(0, dpToPx(15), 0, dpToPx(5));
            configContent.addView(sectionTitle);

            JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
            final LinearLayout diasContainer = new LinearLayout(this);
            diasContainer.setOrientation(LinearLayout.VERTICAL);
            for (String dia : DIAS_SEMANA) {
                CheckBox cb = new CheckBox(this);
                cb.setText(dia);
                cb.setTextColor(COR_CINZA_MEDIO);
                cb.setChecked(diasDescanso.toString().contains("\"" + dia + "\""));
                if (viewOnly) cb.setEnabled(false);
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
            configContent.addView(diasContainer);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao carregar configurações", Toast.LENGTH_SHORT).show();
        }
    }

    private void addConfigRow(String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + ": " + value);
        tv.setTextColor(COR_CINZA_CLARO);
        tv.setTextSize(13);
        tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
        configContent.addView(tv);
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    interface ConfirmCallback {
        void onConfirm(boolean ok);
    }
}
EOF
