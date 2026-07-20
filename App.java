cat > src/main/java/com/academia/app/MainActivity.java << 'EOF'
package com.academia.app;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class MainActivity extends JFrame {
    private JPanel mainPanel;
    private JButton btnPrincipal;
    private JPanel cardTreinoPanel;
    private JPanel exerciciosContainer;
    private JPanel dadosContainer;
    private JPanel timerPanel;
    private JProgressBar progressBar;
    private JLabel progressText;
    private JLabel timerLabel;
    private JLabel cardTitle;
    private JButton configBtn;
    private boolean modoConfig = false;
    private boolean isActive = false;
    private boolean aguardandoTimer = false;
    private javax.swing.Timer timer;
    private int timerRestante = 0;
    private JSONObject configData;
    private JSONObject treinoAtual;
    private int exercicioAtualIndex = 0;
    private static final String ARQUIVO_DADOS = "academia_dados.json";
    private String[] DIAS_SEMANA = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
    private JFrame subModal;
    private JPanel subModalBox;
    private JFrame confirmModal;
    private JFrame pesoAviso;
    private int pendingConfirmIndex = -1;
    private Runnable pendingConfirmAction;

    public MainActivity() {
        setTitle("Sistema de Academia");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(13, 13, 13));

        carregarDados();

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        btnPrincipal = new JButton();
        btnPrincipal.setPreferredSize(new Dimension(130, 130));
        btnPrincipal.setMaximumSize(new Dimension(130, 130));
        btnPrincipal.setMinimumSize(new Dimension(90, 90));
        btnPrincipal.setBackground(new Color(255, 0, 0));
        btnPrincipal.setOpaque(true);
        btnPrincipal.setBorderPainted(false);
        btnPrincipal.setFocusPainted(false);
        btnPrincipal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrincipal.addActionListener(e -> toggleTreino());
        topPanel.add(btnPrincipal);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        cardTreinoPanel = new JPanel();
        cardTreinoPanel.setLayout(new BoxLayout(cardTreinoPanel, BoxLayout.Y_AXIS));
        cardTreinoPanel.setBackground(new Color(26, 26, 26));
        cardTreinoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 42, 42)),
            BorderFactory.createEmptyBorder(16, 14, 16, 14)
        ));
        cardTreinoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardTreinoPanel.setMaximumSize(new Dimension(430, 600));
        cardTreinoPanel.setVisible(false);

        cardTitle = new JLabel("Treino de Hoje");
        cardTitle.setForeground(new Color(170, 170, 170));
        cardTitle.setFont(new Font("Arial", Font.BOLD, 14));
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardTreinoPanel.add(cardTitle);
        cardTreinoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        exerciciosContainer = new JPanel();
        exerciciosContainer.setLayout(new BoxLayout(exerciciosContainer, BoxLayout.Y_AXIS));
        exerciciosContainer.setOpaque(false);
        exerciciosContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardTreinoPanel.add(exerciciosContainer);

        timerPanel = new JPanel();
        timerPanel.setLayout(new BoxLayout(timerPanel, BoxLayout.Y_AXIS));
        timerPanel.setOpaque(false);
        timerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerPanel.setVisible(false);
        cardTreinoPanel.add(timerPanel);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.setOpaque(false);
        progressPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 5));
        progressBar.setMaximumSize(new Dimension(400, 5));
        progressBar.setBackground(new Color(13, 13, 13));
        progressBar.setForeground(new Color(139, 195, 74));
        progressBar.setValue(0);
        progressPanel.add(progressBar);

        progressText = new JLabel("0/0 concluídos");
        progressText.setForeground(new Color(136, 136, 136));
        progressText.setFont(new Font("Arial", Font.PLAIN, 11));
        progressText.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressPanel.add(progressText);

        cardTreinoPanel.add(progressPanel);

        centerPanel.add(cardTreinoPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        dadosContainer = new JPanel();
        dadosContainer.setLayout(new BoxLayout(dadosContainer, BoxLayout.Y_AXIS));
        dadosContainer.setOpaque(false);
        dadosContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        dadosContainer.setMaximumSize(new Dimension(430, Integer.MAX_VALUE));

        JScrollPane scrollDados = new JScrollPane(dadosContainer);
        scrollDados.setOpaque(false);
        scrollDados.getViewport().setOpaque(false);
        scrollDados.setBorder(null);
        scrollDados.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollDados.setMaximumSize(new Dimension(430, 500));

        centerPanel.add(scrollDados);

        add(centerPanel, BorderLayout.CENTER);

        criarModais();

        carregarEstadoBotao();
        renderDados();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salvarDados();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    private void criarModais() {
        subModal = new JFrame();
        subModal.setUndecorated(true);
        subModal.setSize(420, 500);
        subModal.setLocationRelativeTo(this);
        subModal.setBackground(new Color(0, 0, 0, 200));
        subModal.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel modalPanel = new JPanel(new BorderLayout());
        modalPanel.setBackground(new Color(26, 26, 26));
        modalPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 42, 42)),
            BorderFactory.createEmptyBorder(16, 14, 16, 14)
        ));

        subModalBox = new JPanel();
        subModalBox.setLayout(new BoxLayout(subModalBox, BoxLayout.Y_AXIS));
        subModalBox.setOpaque(false);
        JScrollPane scrollSub = new JScrollPane(subModalBox);
        scrollSub.setOpaque(false);
        scrollSub.getViewport().setOpaque(false);
        scrollSub.setBorder(null);
        modalPanel.add(scrollSub, BorderLayout.CENTER);

        subModal.add(modalPanel);

        confirmModal = new JFrame();
        confirmModal.setUndecorated(true);
        confirmModal.setSize(340, 180);
        confirmModal.setLocationRelativeTo(this);
        confirmModal.setBackground(new Color(0, 0, 0, 200));
        confirmModal.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel confirmPanel = new JPanel();
        confirmPanel.setLayout(new BoxLayout(confirmPanel, BoxLayout.Y_AXIS));
        confirmPanel.setBackground(new Color(26, 26, 26));
        confirmPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 42, 42)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        confirmPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel confirmTitle = new JLabel("Confirmar");
        confirmTitle.setForeground(new Color(238, 238, 238));
        confirmTitle.setFont(new Font("Arial", Font.PLAIN, 16));
        confirmTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPanel.add(confirmTitle);

        JLabel confirmMsg = new JLabel("Tem certeza?");
        confirmMsg.setForeground(new Color(170, 170, 170));
        confirmMsg.setFont(new Font("Arial", Font.PLAIN, 13));
        confirmMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPanel.add(confirmMsg);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 10));
        btnRow.setOpaque(false);

        JButton btnSim = new JButton("Sim");
        btnSim.setBackground(new Color(58, 26, 26));
        btnSim.setForeground(new Color(255, 138, 138));
        btnSim.setBorder(BorderFactory.createLineBorder(new Color(90, 42, 42)));
        btnSim.setFocusPainted(false);
        btnSim.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSim.addActionListener(e -> {
            confirmModal.setVisible(false);
            if (pendingConfirmAction != null) {
                pendingConfirmAction.run();
                pendingConfirmAction = null;
            }
        });

        JButton btnNao = new JButton("Não");
        btnNao.setBackground(new Color(42, 42, 42));
        btnNao.setForeground(new Color(204, 204, 204));
        btnNao.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        btnNao.setFocusPainted(false);
        btnNao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNao.addActionListener(e -> {
            confirmModal.setVisible(false);
            pendingConfirmAction = null;
        });

        btnRow.add(btnNao);
        btnRow.add(btnSim);
        confirmPanel.add(btnRow);
        confirmModal.add(confirmPanel);

        pesoAviso = new JFrame();
        pesoAviso.setUndecorated(true);
        pesoAviso.setSize(340, 220);
        pesoAviso.setLocationRelativeTo(this);
        pesoAviso.setBackground(new Color(0, 0, 0, 200));
        pesoAviso.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel pesoPanel = new JPanel();
        pesoPanel.setLayout(new BoxLayout(pesoPanel, BoxLayout.Y_AXIS));
        pesoPanel.setBackground(new Color(26, 26, 26));
        pesoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 42, 42)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        pesoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel pesoTitle = new JLabel("Hora de Pesar!");
        pesoTitle.setForeground(new Color(255, 138, 138));
        pesoTitle.setFont(new Font("Arial", Font.PLAIN, 16));
        pesoTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        pesoPanel.add(pesoTitle);

        JLabel pesoText = new JLabel("Registre seu novo peso.");
        pesoText.setForeground(new Color(204, 204, 204));
        pesoText.setFont(new Font("Arial", Font.PLAIN, 13));
        pesoText.setAlignmentX(Component.CENTER_ALIGNMENT);
        pesoPanel.add(pesoText);

        JTextField pesoInput = new JTextField(10);
        pesoInput.setMaximumSize(new Dimension(200, 30));
        pesoInput.setBackground(new Color(10, 10, 10));
        pesoInput.setForeground(new Color(221, 221, 221));
        pesoInput.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        pesoInput.setHorizontalAlignment(JTextField.CENTER);
        pesoPanel.add(pesoInput);

        JButton pesoBtn = new JButton("Registrar");
        pesoBtn.setBackground(new Color(26, 58, 26));
        pesoBtn.setForeground(new Color(139, 195, 74));
        pesoBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        pesoBtn.setFocusPainted(false);
        pesoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pesoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pesoBtn.addActionListener(e -> {
            try {
                double val = Double.parseDouble(pesoInput.getText().trim());
                if (val <= 0) throw new NumberFormatException();
                String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                JSONArray historico = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
                JSONObject novo = new JSONObject();
                novo.put("peso", val);
                novo.put("data", hoje);
                historico.put(novo);
                configData.getJSONObject("academia").getJSONObject("peso").put("atual", val);
                configData.getJSONObject("academia").getJSONObject("peso").put("ultimoRegistro", LocalDateTime.now().toString());
                salvarDados();
                pesoAviso.setVisible(false);
                pesoInput.setText("");
                renderDados();
                if (treinoAtual != null) renderTreinoCard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Peso inválido.");
            }
        });
        pesoPanel.add(pesoBtn);

        pesoAviso.add(pesoPanel);
    }

    private void carregarDados() {
        try {
            File file = new File(ARQUIVO_DADOS);
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void salvarDados() {
        try {
            File file = new File(ARQUIVO_DADOS);
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(configData.toString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTodayName() {
        return DIAS_SEMANA[LocalDate.now().getDayOfWeek().getValue() - 1];
    }

    private String getTodayKey() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private JSONArray getTodayTreinos() {
        try {
            String hoje = getTodayName();
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONArray result = new JSONArray();
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject treino = treinos.getJSONObject(i);
                if (treino.getString("dia").equals(hoje)) {
                    result.put(treino);
                }
            }
            return result;
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private boolean verificarPeso() {
        try {
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return false;
            String ultimoStr = peso.getString("ultimoRegistro");
            LocalDateTime ultimo = LocalDateTime.parse(ultimoStr);
            long diff = java.time.Duration.between(ultimo, LocalDateTime.now()).toDays();
            int intervalo = peso.getInt("intervalo");
            return diff >= intervalo;
        } catch (JSONException e) {
            return false;
        }
    }

    private int getDiasDesdePesagem() {
        try {
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return 999;
            LocalDateTime ultimo = LocalDateTime.parse(peso.getString("ultimoRegistro"));
            return (int) java.time.Duration.between(ultimo, LocalDateTime.now()).toDays();
        } catch (JSONException e) {
            return 999;
        }
    }

    private String getUltimaPesagemData() {
        try {
            JSONObject peso = configData.getJSONObject("academia").getJSONObject("peso");
            if (peso.isNull("ultimoRegistro")) return "Nunca";
            LocalDateTime ultimo = LocalDateTime.parse(peso.getString("ultimoRegistro"));
            return ultimo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (JSONException e) {
            return "Nunca";
        }
    }

    private int getDiasFrequentados() {
        try {
            JSONObject academia = configData.getJSONObject("academia");
            if (academia.isNull("inicio")) return 0;
            String inicioStr = academia.getString("inicio");
            LocalDate inicio = LocalDate.parse(inicioStr);
            return (int) java.time.Duration.between(inicio.atStartOfDay(), LocalDateTime.now()).toDays();
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDataBR(String dataStr) {
        if (dataStr == null || dataStr.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(dataStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dataStr;
        }
    }

    private void mostrarConfirmacao(String titulo, String msg, Runnable onConfirm) {
        confirmModal.setVisible(true);
        JPanel panel = (JPanel) confirmModal.getContentPane();
        JLabel title = (JLabel) ((JPanel) panel.getComponent(0)).getComponent(0);
        JLabel msgLabel = (JLabel) ((JPanel) panel.getComponent(0)).getComponent(1);
        title.setText(titulo);
        msgLabel.setText(msg);
        pendingConfirmAction = onConfirm;
        JPanel btnRow = (JPanel) ((JPanel) panel.getComponent(0)).getComponent(2);
        btnRow.removeAll();
        JButton btnSim = new JButton("Sim");
        btnSim.setBackground(new Color(58, 26, 26));
        btnSim.setForeground(new Color(255, 138, 138));
        btnSim.setBorder(BorderFactory.createLineBorder(new Color(90, 42, 42)));
        btnSim.setFocusPainted(false);
        btnSim.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSim.addActionListener(e -> {
            confirmModal.setVisible(false);
            if (pendingConfirmAction != null) {
                pendingConfirmAction.run();
                pendingConfirmAction = null;
            }
        });
        JButton btnNao = new JButton("Não");
        btnNao.setBackground(new Color(42, 42, 42));
        btnNao.setForeground(new Color(204, 204, 204));
        btnNao.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        btnNao.setFocusPainted(false);
        btnNao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNao.addActionListener(e -> {
            confirmModal.setVisible(false);
            pendingConfirmAction = null;
        });
        btnRow.add(btnNao);
        btnRow.add(btnSim);
        btnRow.revalidate();
        btnRow.repaint();
    }

    private void mostrarConfirmacaoUnico(String titulo, String msg) {
        confirmModal.setVisible(true);
        JPanel panel = (JPanel) confirmModal.getContentPane();
        JLabel title = (JLabel) ((JPanel) panel.getComponent(0)).getComponent(0);
        JLabel msgLabel = (JLabel) ((JPanel) panel.getComponent(0)).getComponent(1);
        title.setText(titulo);
        msgLabel.setText(msg);
        pendingConfirmAction = null;
        JPanel btnRow = (JPanel) ((JPanel) panel.getComponent(0)).getComponent(2);
        btnRow.removeAll();
        JButton btnOk = new JButton("OK");
        btnOk.setBackground(new Color(26, 58, 26));
        btnOk.setForeground(new Color(139, 195, 74));
        btnOk.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        btnOk.setFocusPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.addActionListener(e -> confirmModal.setVisible(false));
        btnRow.add(btnOk);
        btnRow.revalidate();
        btnRow.repaint();
    }

    private void mostrarAvisoPeso() {
        pesoAviso.setVisible(true);
        JPanel panel = (JPanel) pesoAviso.getContentPane();
        JLabel text = (JLabel) ((JPanel) panel.getComponent(0)).getComponent(1);
        text.setText("Já faz " + getDiasDesdePesagem() + " dias desde a última pesagem (" + getUltimaPesagemData() + "). Registre seu novo peso.");
        JTextField input = (JTextField) ((JPanel) panel.getComponent(0)).getComponent(2);
        input.requestFocus();
    }

    private void limparTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        timerRestante = 0;
        aguardandoTimer = false;
        timerPanel.setVisible(false);
    }

    private void iniciarTimer(int segundos, Runnable callback) {
        limparTimer();
        timerRestante = segundos;
        aguardandoTimer = true;
        timerPanel.setVisible(true);
        timerPanel.removeAll();
        JPanel display = new JPanel();
        display.setLayout(new BoxLayout(display, BoxLayout.Y_AXIS));
        display.setBackground(new Color(13, 13, 13));
        display.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 74, 42)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        display.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel("Descanso");
        label.setForeground(new Color(136, 136, 136));
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        display.add(label);

        timerLabel = new JLabel(String.format("%02d:%02d", segundos/60, segundos%60));
        timerLabel.setForeground(new Color(139, 195, 74));
        timerLabel.setFont(new Font("Arial", Font.BOLD, 22));
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        display.add(timerLabel);

        timerPanel.add(display);
        timerPanel.revalidate();
        timerPanel.repaint();

        timer = new javax.swing.Timer(1000, e -> {
            timerRestante--;
            if (timerRestante <= 0) {
                limparTimer();
                if (callback != null) callback.run();
            } else {
                timerLabel.setText(String.format("%02d:%02d", timerRestante/60, timerRestante%60));
            }
        });
        timer.start();
    }

    private void renderTreinoCard() {
        if (treinoAtual == null) {
            exerciciosContainer.removeAll();
            exerciciosContainer.revalidate();
            exerciciosContainer.repaint();
            return;
        }

        try {
            JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
            if (exercicios.length() == 0) {
                exerciciosContainer.removeAll();
                JLabel empty = new JLabel("Nenhum exercício definido.");
                empty.setForeground(new Color(102, 102, 102));
                empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                exerciciosContainer.add(empty);
                exerciciosContainer.revalidate();
                exerciciosContainer.repaint();
                return;
            }

            if (exercicioAtualIndex >= exercicios.length()) {
                exerciciosContainer.removeAll();
                JLabel done = new JLabel("Treino concluído!");
                done.setForeground(new Color(139, 195, 74));
                done.setFont(new Font("Arial", Font.BOLD, 14));
                done.setAlignmentX(Component.CENTER_ALIGNMENT);
                exerciciosContainer.add(done);
                exerciciosContainer.revalidate();
                exerciciosContainer.repaint();
                return;
            }

            JSONObject ex = exercicios.getJSONObject(exercicioAtualIndex);
            int totalSeries = ex.getInt("sets");
            int seriesFeitas = ex.has("_seriesFeitas") ? ex.getInt("_seriesFeitas") : 0;
            boolean isDone = seriesFeitas >= totalSeries;
            String warmupText = ex.has("warmup") && ex.getBoolean("warmup") ? "Aquecimento" : "";

            exerciciosContainer.removeAll();
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(new Color(13, 13, 13));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
            ));
            card.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel nameLabel = new JLabel(ex.getString("exercise") + " " + warmupText);
            nameLabel.setForeground(new Color(238, 238, 238));
            nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(nameLabel);

            JLabel details = new JLabel(ex.getInt("sets") + " séries x " + ex.getInt("reps") + " reps");
            details.setForeground(new Color(170, 170, 170));
            details.setFont(new Font("Arial", Font.PLAIN, 14));
            details.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(details);

            JLabel loadLabel = new JLabel("Carga: " + ex.getDouble("load") + "kg");
            loadLabel.setForeground(new Color(139, 195, 74));
            loadLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            loadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(loadLabel);

            if (ex.has("metaCarga") && !ex.isNull("metaCarga")) {
                JLabel metaLabel = new JLabel("Meta: " + ex.getDouble("metaCarga") + "kg");
                metaLabel.setForeground(new Color(255, 170, 0));
                metaLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                metaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(metaLabel);
            }

            JLabel statusLabel = new JLabel(isDone ? "Concluído" : seriesFeitas + "/" + totalSeries + " séries");
            statusLabel.setForeground(isDone ? new Color(139, 195, 74) : new Color(102, 102, 102));
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(statusLabel);

            if (!isDone && !aguardandoTimer) {
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                btnPanel.setOpaque(false);
                JButton btnPronto = new JButton("PRONTO");
                btnPronto.setBackground(new Color(26, 58, 26));
                btnPronto.setForeground(new Color(139, 195, 74));
                btnPronto.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
                btnPronto.setFocusPainted(false);
                btnPronto.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnPronto.addActionListener(e -> {
                    if (aguardandoTimer) return;
                    try {
                        JSONObject exAtual = treinoAtual.getJSONArray("exercicios").getJSONObject(exercicioAtualIndex);
                        int series = exAtual.getInt("sets");
                        int feitas = exAtual.has("_seriesFeitas") ? exAtual.getInt("_seriesFeitas") : 0;
                        if (feitas >= series) return;
                        exAtual.put("_seriesFeitas", feitas + 1);

                        if (exAtual.getInt("_seriesFeitas") >= series) {
                            exAtual.put("_done", true);
                            if (!(exAtual.has("warmup") && exAtual.getBoolean("warmup"))) {
                                mostrarEvolucaoDialog(exercicioAtualIndex);
                                return;
                            } else {
                                salvarProgressoEAtualizar(exercicioAtualIndex);
                            }
                        } else {
                            salvarDados();
                            renderTreinoCard();
                        }
                    } catch (JSONException ex2) {
                        ex2.printStackTrace();
                    }
                });
                btnPanel.add(btnPronto);
                card.add(btnPanel);
            } else if (isDone) {
                JLabel doneLabel = new JLabel("Concluído");
                doneLabel.setForeground(new Color(139, 195, 74));
                doneLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                doneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(doneLabel);
            } else if (aguardandoTimer) {
                JLabel waitLabel = new JLabel("Aguardando descanso...");
                waitLabel.setForeground(new Color(255, 170, 0));
                waitLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                waitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(waitLabel);
            }

            exerciciosContainer.add(card);
            exerciciosContainer.revalidate();
            exerciciosContainer.repaint();

            int total = exercicios.length();
            int done = 0;
            for (int i = 0; i < total; i++) {
                JSONObject e = exercicios.getJSONObject(i);
                int s = e.getInt("sets");
                int f = e.has("_seriesFeitas") ? e.getInt("_seriesFeitas") : 0;
                if (f >= s) done++;
            }
            int pct = total > 0 ? (done * 100) / total : 0;
            progressBar.setValue(pct);
            progressText.setText(done + "/" + total + " exercícios concluídos");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void mostrarEvolucaoDialog(int idx) {
        JDialog dialog = new JDialog(this, "Evolução de Carga", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(26, 26, 26));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Evolução de Carga");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);

        try {
            JSONObject ex = treinoAtual.getJSONArray("exercicios").getJSONObject(idx);
            JLabel exName = new JLabel("Registre a evolução para " + ex.getString("exercise"));
            exName.setForeground(new Color(170, 170, 170));
            exName.setFont(new Font("Arial", Font.PLAIN, 13));
            exName.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(exName);

            JLabel loadLabel = new JLabel("Carga atual (kg)");
            loadLabel.setForeground(new Color(136, 136, 136));
            loadLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            loadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(loadLabel);

            JTextField loadField = new JTextField(String.valueOf(ex.getDouble("load")), 10);
            loadField.setMaximumSize(new Dimension(200, 30));
            loadField.setBackground(new Color(10, 10, 10));
            loadField.setForeground(new Color(221, 221, 221));
            loadField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            loadField.setHorizontalAlignment(JTextField.CENTER);
            panel.add(loadField);

            JLabel repsLabel = new JLabel("Repetições atuais");
            repsLabel.setForeground(new Color(136, 136, 136));
            repsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            repsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(repsLabel);

            JTextField repsField = new JTextField(String.valueOf(ex.getInt("reps")), 10);
            repsField.setMaximumSize(new Dimension(200, 30));
            repsField.setBackground(new Color(10, 10, 10));
            repsField.setForeground(new Color(221, 221, 221));
            repsField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            repsField.setHorizontalAlignment(JTextField.CENTER);
            panel.add(repsField);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 10));
            btnPanel.setOpaque(false);

            JButton btnPular = new JButton("Pular");
            btnPular.setBackground(new Color(42, 42, 42));
            btnPular.setForeground(new Color(204, 204, 204));
            btnPular.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            btnPular.setFocusPainted(false);
            btnPular.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPular.addActionListener(e -> {
                dialog.dispose();
                salvarProgressoEAtualizar(idx);
            });

            JButton btnRegistrar = new JButton("Registrar");
            btnRegistrar.setBackground(new Color(26, 58, 26));
            btnRegistrar.setForeground(new Color(139, 195, 74));
            btnRegistrar.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            btnRegistrar.setFocusPainted(false);
            btnRegistrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnRegistrar.addActionListener(e -> {
                try {
                    double load = Double.parseDouble(loadField.getText().trim());
                    int reps = Integer.parseInt(repsField.getText().trim());
                    if (load <= 0 || reps < 1) throw new NumberFormatException();
                    String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    JSONArray history;
                    if (ex.has("loadHistory")) {
                        history = ex.getJSONArray("loadHistory");
                    } else {
                        history = new JSONArray();
                        ex.put("loadHistory", history);
                    }
                    JSONObject novo = new JSONObject();
                    novo.put("load", load);
                    novo.put("reps", reps);
                    novo.put("date", hoje);
                    history.put(novo);
                    ex.put("load", load);
                    ex.put("reps", reps);
                    dialog.dispose();
                    salvarProgressoEAtualizar(idx);
                } catch (Exception ex2) {
                    JOptionPane.showMessageDialog(null, "Valores inválidos.");
                }
            });

            btnPanel.add(btnPular);
            btnPanel.add(btnRegistrar);
            panel.add(btnPanel);

            dialog.add(panel);
            dialog.setVisible(true);
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
                                if (exOriginal.has("loadHistory")) {
                                    e.put("loadHistory", exOriginal.getJSONArray("loadHistory"));
                                }
                                e.put("load", exOriginal.getDouble("load"));
                                e.put("reps", exOriginal.getInt("reps"));
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            salvarDados();
            proximoExercicio(idx);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void proximoExercicio(int idxAtual) {
        try {
            JSONArray exercicios = treinoAtual.getJSONArray("exercicios");
            int proximoIdx = -1;
            for (int i = idxAtual + 1; i < exercicios.length(); i++) {
                JSONObject ex = exercicios.getJSONObject(i);
                int sets = ex.getInt("sets");
                int feitas = ex.has("_seriesFeitas") ? ex.getInt("_seriesFeitas") : 0;
                if (feitas < sets) {
                    proximoIdx = i;
                    break;
                }
            }

            if (proximoIdx == -1) {
                exercicioAtualIndex = exercicios.length();
                renderTreinoCard();
                String hojeKey = getTodayKey();
                configData.getJSONObject("academia").getJSONObject("treinoConcluido").put(hojeKey, true);
                configData.getJSONObject("academia").put("treino_" + hojeKey, JSONObject.NULL);
                configData.getJSONObject("academia").put("botaoAtivo", false);
                salvarDados();
                SwingUtilities.invokeLater(() -> {
                    mostrarConfirmacaoUnico("Treino Concluído", "Parabéns! Você concluiu o treino de hoje.");
                    btnPrincipal.setBackground(new Color(255, 0, 0));
                    isActive = false;
                    cardTreinoPanel.setVisible(false);
                    treinoAtual = null;
                    limparTimer();
                    try {
                        configData.getJSONObject("academia").put("botaoAtivo", false);
                    } catch (JSONException e) {}
                    salvarDados();
                    renderDados();
                });
                return;
            }

            exercicioAtualIndex = proximoIdx;
            JSONObject proxEx = exercicios.getJSONObject(proximoIdx);
            int descanso = proxEx.has("descanso") && !proxEx.isNull("descanso") ? proxEx.getInt("descanso") : 0;

            if (descanso > 0) {
                aguardandoTimer = true;
                renderTreinoCard();
                iniciarTimer(descanso, () -> {
                    aguardandoTimer = false;
                    renderTreinoCard();
                });
            } else {
                renderTreinoCard();
            }

            String hojeKey = getTodayKey();
            configData.getJSONObject("academia").put("treino_" + hojeKey, treinoAtual);
            configData.getJSONObject("academia").put("botaoAtivo", true);
            salvarDados();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void iniciarTreino() {
        JSONArray treinos = getTodayTreinos();
        if (treinos.length() == 0) {
            mostrarConfirmacaoUnico("Aviso", "Nenhum treino programado para hoje.");
            btnPrincipal.setBackground(new Color(255, 0, 0));
            isActive = false;
            cardTreinoPanel.setVisible(false);
            try {
                configData.getJSONObject("academia").put("botaoAtivo", false);
            } catch (JSONException e) {}
            salvarDados();
            return;
        }

        try {
            String hojeKey = getTodayKey();
            JSONObject treinoConcluido = configData.getJSONObject("academia").getJSONObject("treinoConcluido");
            if (treinoConcluido.has(hojeKey) && treinoConcluido.getBoolean(hojeKey)) {
                mostrarConfirmacao("Treino Concluído", "Você já concluiu o treino de hoje. Deseja refazê-lo?", () -> {
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
                if (treinoAtual.has("exercicios")) {
                    JSONArray exs = treinoAtual.getJSONArray("exercicios");
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (!e.has("_seriesFeitas")) e.put("_seriesFeitas", 0);
                        if (!e.has("_done")) e.put("_done", false);
                        if (!e.has("loadHistory")) e.put("loadHistory", new JSONArray());
                    }
                    boolean algumNaoConcluido = false;
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (e.getInt("_seriesFeitas") < e.getInt("sets")) {
                            exercicioAtualIndex = i;
                            algumNaoConcluido = true;
                            break;
                        }
                    }
                    if (!algumNaoConcluido) {
                        treinoConcluido.put(hojeKey, true);
                        configData.getJSONObject("academia").put("treino_" + hojeKey, JSONObject.NULL);
                        configData.getJSONObject("academia").put("botaoAtivo", false);
                        salvarDados();
                        btnPrincipal.setBackground(new Color(255, 0, 0));
                        isActive = false;
                        cardTreinoPanel.setVisible(false);
                        treinoAtual = null;
                        limparTimer();
                        return;
                    }
                }
                cardTreinoPanel.setVisible(true);
                btnPrincipal.setBackground(new Color(0, 204, 0));
                isActive = true;
                configData.getJSONObject("academia").put("botaoAtivo", true);
                salvarDados();
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
                    e.put("_seriesFeitas", 0);
                    e.put("_done", false);
                    if (!e.has("loadHistory")) e.put("loadHistory", new JSONArray());
                    if (e.has("loadHistory") && e.getJSONArray("loadHistory").length() > 0) {
                        JSONArray hist = e.getJSONArray("loadHistory");
                        JSONObject ultimo = hist.getJSONObject(hist.length() - 1);
                        e.put("load", ultimo.getDouble("load"));
                        if (ultimo.has("reps")) e.put("reps", ultimo.getInt("reps"));
                    }
                }
            }
            exercicioAtualIndex = 0;
            cardTreinoPanel.setVisible(true);
            btnPrincipal.setBackground(new Color(0, 204, 0));
            isActive = true;
            configData.getJSONObject("academia").put("botaoAtivo", true);
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
            if (treinoAtual != null && treinoAtual.has("exercicios")) {
                try {
                    JSONArray exs = treinoAtual.getJSONArray("exercicios");
                    boolean algumFeito = false;
                    for (int i = 0; i < exs.length(); i++) {
                        if (exs.getJSONObject(i).getInt("_seriesFeitas") > 0) {
                            algumFeito = true;
                            break;
                        }
                    }
                    if (algumFeito) {
                        mostrarConfirmacao("Parar Treino", "Você já fez alguns exercícios. Deseja realmente parar?", () -> {
                            btnPrincipal.setBackground(new Color(255, 0, 0));
                            isActive = false;
                            cardTreinoPanel.setVisible(false);
                            treinoAtual = null;
                            limparTimer();
                            String hojeKey = getTodayKey();
                            try {
                                configData.getJSONObject("academia").put("treino_" + hojeKey, JSONObject.NULL);
                                configData.getJSONObject("academia").put("botaoAtivo", false);
                            } catch (JSONException e) {}
                            salvarDados();
                            renderDados();
                        });
                        return;
                    }
                } catch (JSONException e) {}
            }
            btnPrincipal.setBackground(new Color(255, 0, 0));
            isActive = false;
            cardTreinoPanel.setVisible(false);
            treinoAtual = null;
            limparTimer();
            try {
                configData.getJSONObject("academia").put("botaoAtivo", false);
            } catch (JSONException e) {}
            salvarDados();
            renderDados();
        } else {
            JSONArray treinos = getTodayTreinos();
            if (treinos.length() == 0) {
                mostrarConfirmacaoUnico("Aviso", "Nenhum treino programado para hoje.");
                return;
            }
            iniciarTreino();
            renderDados();
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
                JSONObject treino = configData.getJSONObject("academia").getJSONObject("treino_" + hojeKey);
                if (treino.has("exercicios")) {
                    JSONArray exs = treino.getJSONArray("exercicios");
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (!e.has("loadHistory")) e.put("loadHistory", new JSONArray());
                    }
                    boolean todosConcluidos = true;
                    for (int i = 0; i < exs.length(); i++) {
                        JSONObject e = exs.getJSONObject(i);
                        if (e.getInt("_seriesFeitas") < e.getInt("sets")) {
                            todosConcluidos = false;
                            break;
                        }
                    }
                    if (todosConcluidos) {
                        btnPrincipal.setBackground(new Color(255, 0, 0));
                        isActive = false;
                        cardTreinoPanel.setVisible(false);
                        treinoAtual = null;
                        limparTimer();
                        treinoConcluido.put(hojeKey, true);
                        configData.getJSONObject("academia").put("botaoAtivo", false);
                        salvarDados();
                    } else {
                        btnPrincipal.setBackground(new Color(0, 204, 0));
                        isActive = true;
                        cardTreinoPanel.setVisible(true);
                        treinoAtual = treino;
                        exercicioAtualIndex = 0;
                        for (int i = 0; i < exs.length(); i++) {
                            JSONObject e = exs.getJSONObject(i);
                            if (e.getInt("_seriesFeitas") < e.getInt("sets")) {
                                exercicioAtualIndex = i;
                                break;
                            }
                        }
                        renderTreinoCard();
                    }
                    return;
                }
            }

            btnPrincipal.setBackground(new Color(255, 0, 0));
            isActive = false;
            cardTreinoPanel.setVisible(false);
            treinoAtual = null;
            limparTimer();
            if (configData.getJSONObject("academia").getBoolean("botaoAtivo")) {
                configData.getJSONObject("academia").put("botaoAtivo", false);
                salvarDados();
            }
        } catch (JSONException e) {
            btnPrincipal.setBackground(new Color(255, 0, 0));
            isActive = false;
            cardTreinoPanel.setVisible(false);
            treinoAtual = null;
            limparTimer();
        }
    }

    private void renderDados() {
        dadosContainer.removeAll();

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(26, 26, 26));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(42, 42, 42)),
            BorderFactory.createEmptyBorder(16, 14, 16, 14)
        ));
        box.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Academia");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        header.add(title, BorderLayout.WEST);

        configBtn = new JButton(modoConfig ? "✅ Pronto" : "⚙️ Configurar");
        configBtn.setBackground(new Color(42, 42, 42));
        configBtn.setForeground(new Color(204, 204, 204));
        configBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        configBtn.setFocusPainted(false);
        configBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        configBtn.addActionListener(e -> {
            modoConfig = !modoConfig;
            renderDados();
        });
        if (modoConfig) {
            configBtn.setBackground(new Color(26, 58, 26));
            configBtn.setForeground(new Color(139, 195, 74));
            configBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        }
        header.add(configBtn, BorderLayout.EAST);

        box.add(header);

        JPanel separator = new JPanel();
        separator.setBackground(new Color(42, 42, 42));
        separator.setPreferredSize(new Dimension(400, 1));
        separator.setMaximumSize(new Dimension(400, 1));
        box.add(separator);
        box.add(Box.createRigidArea(new Dimension(0, 12)));

        if (modoConfig) {
            renderModoConfig(box);
        } else {
            renderModoVisualizacao(box);
        }

        dadosContainer.add(box);
        dadosContainer.revalidate();
        dadosContainer.repaint();
    }

    private void renderModoVisualizacao(JPanel parent) {
        try {
            JSONObject academia = configData.getJSONObject("academia");
            JSONObject peso = academia.getJSONObject("peso");
            int diasFreq = getDiasFrequentados();
            String inicioDisplay = academia.isNull("inicio") ? "Não definida" : formatDataBR(academia.getString("inicio"));

            addLabel(parent, "Data de Início");
            JPanel valueDisplay = new JPanel(new BorderLayout());
            valueDisplay.setOpaque(false);
            valueDisplay.setBackground(new Color(13, 13, 13));
            valueDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            JLabel info = new JLabel(inicioDisplay + (academia.has("inicio") && !academia.isNull("inicio") && diasFreq > 0 ? " (" + diasFreq + " dias)" : ""));
            info.setForeground(new Color(238, 238, 238));
            info.setFont(new Font("Arial", Font.PLAIN, 14));
            valueDisplay.add(info, BorderLayout.WEST);
            parent.add(valueDisplay);

            parent.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            JLabel subTitle = new JLabel("Evolução do Peso");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JPanel pesoDisplay = new JPanel(new BorderLayout());
            pesoDisplay.setOpaque(false);
            pesoDisplay.setBackground(new Color(13, 13, 13));
            pesoDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            String pesoAtual = peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg";
            JLabel pesoLabel = new JLabel("Peso Atual: " + pesoAtual);
            pesoLabel.setForeground(new Color(238, 238, 238));
            pesoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            pesoDisplay.add(pesoLabel, BorderLayout.WEST);

            String evolucaoTexto = "Sem dados";
            String evolucaoCor = "#aaaaaa";
            if (peso.has("historico") && peso.getJSONArray("historico").length() >= 2) {
                JSONArray hist = peso.getJSONArray("historico");
                double primeiro = hist.getJSONObject(0).getDouble("peso");
                double ultimo = hist.getJSONObject(hist.length() - 1).getDouble("peso");
                double diff = ultimo - primeiro;
                double pct = primeiro != 0 ? (diff / primeiro) * 100 : 0;
                evolucaoTexto = (diff > 0 ? "+" : "") + String.format("%.1f", diff) + "kg (" + (pct > 0 ? "+" : "") + String.format("%.1f", pct) + "%)";
                evolucaoCor = diff > 0 ? "#8bc34a" : (diff < 0 ? "#ff6b6b" : "#aaaaaa");
            }
            JLabel evolucaoLabel = new JLabel(evolucaoTexto);
            evolucaoLabel.setForeground(Color.decode(evolucaoCor));
            evolucaoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            pesoDisplay.add(evolucaoLabel, BorderLayout.EAST);

            subSection.add(pesoDisplay);

            JLabel ultimaPesagem = new JLabel("Última pesagem: " + getUltimaPesagemData());
            ultimaPesagem.setForeground(new Color(102, 102, 102));
            ultimaPesagem.setFont(new Font("Arial", Font.PLAIN, 11));
            subSection.add(ultimaPesagem);

            if (!peso.isNull("meta")) {
                JLabel metaLabel = new JLabel("Meta: " + peso.getDouble("meta") + " kg");
                metaLabel.setForeground(new Color(102, 102, 102));
                metaLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(metaLabel);
            }

            parent.add(subSection);

            JSONArray treinos = academia.getJSONArray("treinos");
            JSONArray todosExercicios = new JSONArray();
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject t = treinos.getJSONObject(i);
                if (t.has("exercicios")) {
                    JSONArray exs = t.getJSONArray("exercicios");
                    for (int j = 0; j < exs.length(); j++) {
                        JSONObject e = exs.getJSONObject(j);
                        if (!(e.has("warmup") && e.getBoolean("warmup"))) {
                            if (!e.has("loadHistory")) e.put("loadHistory", new JSONArray());
                            todosExercicios.put(e);
                        }
                    }
                }
            }

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Evolução de Carga");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JPanel tablePanel = new JPanel(new GridLayout(0, 3, 10, 5));
            tablePanel.setOpaque(false);
            tablePanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

            JLabel h1 = new JLabel("Exercício");
            h1.setForeground(new Color(136, 136, 136));
            h1.setFont(new Font("Arial", Font.BOLD, 12));
            tablePanel.add(h1);
            JLabel h2 = new JLabel("Evolução");
            h2.setForeground(new Color(136, 136, 136));
            h2.setFont(new Font("Arial", Font.BOLD, 12));
            tablePanel.add(h2);
            JLabel h3 = new JLabel("Progresso");
            h3.setForeground(new Color(136, 136, 136));
            h3.setFont(new Font("Arial", Font.BOLD, 12));
            tablePanel.add(h3);

            if (todosExercicios.length() > 0) {
                for (int i = 0; i < todosExercicios.length(); i++) {
                    JSONObject ex = todosExercicios.getJSONObject(i);
                    String evolucao = "Sem dados";
                    String progresso = "0%";
                    String cor = "#aaaaaa";
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
                    JLabel exName = new JLabel(ex.getString("exercise"));
                    exName.setForeground(new Color(204, 204, 204));
                    exName.setFont(new Font("Arial", Font.PLAIN, 12));
                    tablePanel.add(exName);

                    JLabel exEvo = new JLabel(evolucao);
                    exEvo.setForeground(Color.decode(cor));
                    exEvo.setFont(new Font("Arial", Font.PLAIN, 12));
                    tablePanel.add(exEvo);

                    JLabel exProg = new JLabel(progresso);
                    exProg.setForeground(Color.decode(cor));
                    exProg.setFont(new Font("Arial", Font.PLAIN, 12));
                    tablePanel.add(exProg);
                }
            } else {
                JLabel empty = new JLabel("Nenhum exercício com dados");
                empty.setForeground(new Color(102, 102, 102));
                empty.setFont(new Font("Arial", Font.PLAIN, 12));
                empty.setHorizontalAlignment(SwingConstants.CENTER);
                tablePanel.add(empty);
                tablePanel.add(new JLabel());
                tablePanel.add(new JLabel());
            }

            subSection.add(tablePanel);
            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Resumo");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            String[] resumos = {
                "Peso Atual: " + (peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg"),
                "Meta: " + (peso.isNull("meta") ? "Não definida" : peso.getDouble("meta") + " kg"),
                "Intervalo Pesagem: " + peso.getInt("intervalo") + " dias",
                "Última pesagem: " + getUltimaPesagemData(),
                "Dias frequentados: " + diasFreq,
                "Data de início: " + inicioDisplay
            };
            for (String s : resumos) {
                JPanel line = new JPanel(new BorderLayout());
                line.setOpaque(false);
                line.setBackground(new Color(13, 13, 13));
                line.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(42, 42, 42)),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
                ));
                JLabel lbl = new JLabel(s);
                lbl.setForeground(new Color(238, 238, 238));
                lbl.setFont(new Font("Arial", Font.PLAIN, 13));
                line.add(lbl, BorderLayout.WEST);
                subSection.add(line);
                subSection.add(Box.createRigidArea(new Dimension(0, 3)));
            }

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Objetivos");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONArray objetivos = academia.getJSONArray("objetivos");
            if (objetivos.length() > 0) {
                for (int i = 0; i < objetivos.length(); i++) {
                    JPanel item = new JPanel(new BorderLayout());
                    item.setOpaque(false);
                    item.setBackground(new Color(13, 13, 13));
                    item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(42, 42, 42)),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                    ));
                    JLabel lbl = new JLabel(objetivos.getString(i));
                    lbl.setForeground(new Color(238, 238, 238));
                    lbl.setFont(new Font("Arial", Font.PLAIN, 13));
                    item.add(lbl, BorderLayout.WEST);
                    subSection.add(item);
                    subSection.add(Box.createRigidArea(new Dimension(0, 3)));
                }
            } else {
                JLabel empty = new JLabel("Nenhum objetivo definido.");
                empty.setForeground(new Color(102, 102, 102));
                empty.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(empty);
            }

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Dias de Descanso");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JPanel diasPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            diasPanel.setOpaque(false);
            JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
            if (diasDescanso.length() > 0) {
                for (int i = 0; i < diasDescanso.length(); i++) {
                    JLabel dia = new JLabel(diasDescanso.getString(i));
                    dia.setForeground(new Color(204, 204, 204));
                    dia.setFont(new Font("Arial", Font.PLAIN, 12));
                    dia.setBackground(new Color(26, 26, 26));
                    dia.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(42, 42, 42)),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)
                    ));
                    dia.setOpaque(true);
                    diasPanel.add(dia);
                }
            } else {
                JLabel empty = new JLabel("Nenhum dia de descanso");
                empty.setForeground(new Color(102, 102, 102));
                empty.setFont(new Font("Arial", Font.PLAIN, 11));
                diasPanel.add(empty);
            }
            subSection.add(diasPanel);

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Roupas");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONObject roupas = academia.getJSONObject("roupas");
            String[] cats = {"camisas", "calcas", "tenis"};
            String[] catLabels = {"Camisas", "Calças", "Tênis"};
            for (int c = 0; c < cats.length; c++) {
                JLabel catLabel = new JLabel(catLabels[c]);
                catLabel.setForeground(new Color(136, 136, 136));
                catLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(catLabel);
                JSONArray items = roupas.getJSONArray(cats[c]);
                if (items.length() > 0) {
                    for (int i = 0; i < items.length(); i++) {
                        JPanel item = new JPanel(new BorderLayout());
                        item.setOpaque(false);
                        item.setBackground(new Color(13, 13, 13));
                        item.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(42, 42, 42)),
                            BorderFactory.createEmptyBorder(3, 6, 3, 6)
                        ));
                        JLabel lbl = new JLabel(items.getString(i));
                        lbl.setForeground(new Color(204, 204, 204));
                        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
                        item.add(lbl, BorderLayout.WEST);
                        subSection.add(item);
                    }
                } else {
                    JLabel empty = new JLabel("Nenhuma " + catLabels[c].toLowerCase() + ".");
                    empty.setForeground(new Color(102, 102, 102));
                    empty.setFont(new Font("Arial", Font.PLAIN, 11));
                    subSection.add(empty);
                }
                subSection.add(Box.createRigidArea(new Dimension(0, 3)));
            }

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Combinações");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONObject combinacoes = academia.getJSONObject("combinacoes");
            JSONArray workingDays = new JSONArray();
            for (String d : DIAS_SEMANA) {
                boolean isDescanso = false;
                for (int i = 0; i < diasDescanso.length(); i++) {
                    if (diasDescanso.getString(i).equals(d)) {
                        isDescanso = true;
                        break;
                    }
                }
                if (!isDescanso) workingDays.put(d);
            }

            if (workingDays.length() > 0) {
                for (int i = 0; i < workingDays.length(); i++) {
                    String day = workingDays.getString(i);
                    JPanel dayPanel = new JPanel();
                    dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
                    dayPanel.setOpaque(false);
                    dayPanel.setBackground(new Color(13, 13, 13));
                    dayPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(26, 26, 26)),
                        BorderFactory.createEmptyBorder(4, 6, 4, 6)
                    ));

                    JLabel dayTitle = new JLabel(day);
                    dayTitle.setForeground(new Color(170, 170, 170));
                    dayTitle.setFont(new Font("Arial", Font.BOLD, 12));
                    dayPanel.add(dayTitle);

                    JSONArray combos = combinacoes.has(day) ? combinacoes.getJSONArray(day) : new JSONArray();
                    if (combos.length() > 0) {
                        for (int j = 0; j < combos.length(); j++) {
                            JPanel comboItem = new JPanel(new BorderLayout());
                            comboItem.setOpaque(false);
                            JLabel comboText = new JLabel(combos.getString(j));
                            comboText.setForeground(new Color(204, 204, 204));
                            comboText.setFont(new Font("Arial", Font.PLAIN, 12));
                            comboItem.add(comboText, BorderLayout.WEST);
                            dayPanel.add(comboItem);
                        }
                    } else {
                        JLabel empty = new JLabel("Nenhuma combinação.");
                        empty.setForeground(new Color(102, 102, 102));
                        empty.setFont(new Font("Arial", Font.PLAIN, 11));
                        dayPanel.add(empty);
                    }
                    subSection.add(dayPanel);
                    subSection.add(Box.createRigidArea(new Dimension(0, 3)));
                }
            } else {
                JLabel empty = new JLabel("Nenhum dia disponível.");
                empty.setForeground(new Color(102, 102, 102));
                empty.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(empty);
            }

            parent.add(subSection);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void renderModoConfig(JPanel parent) {
        try {
            JSONObject academia = configData.getJSONObject("academia");
            JSONObject peso = academia.getJSONObject("peso");
            int diasFreq = getDiasFrequentados();
            String inicioDisplay = academia.isNull("inicio") ? "Não definida" : formatDataBR(academia.getString("inicio"));

            JPanel subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);

            JLabel subTitle = new JLabel("Data de Início");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JPanel valueDisplay = new JPanel(new BorderLayout());
            valueDisplay.setOpaque(false);
            valueDisplay.setBackground(new Color(13, 13, 13));
            valueDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            JLabel info = new JLabel(inicioDisplay + (academia.has("inicio") && !academia.isNull("inicio") && diasFreq > 0 ? " (" + diasFreq + " dias)" : ""));
            info.setForeground(new Color(238, 238, 238));
            info.setFont(new Font("Arial", Font.PLAIN, 14));
            valueDisplay.add(info, BorderLayout.WEST);

            JButton editBtn = new JButton(academia.isNull("inicio") ? "Definir" : "Editar");
            editBtn.setBackground(new Color(26, 58, 26));
            editBtn.setForeground(new Color(139, 195, 74));
            editBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            editBtn.setFocusPainted(false);
            editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editBtn.addActionListener(e -> mostrarEditarInicio());
            valueDisplay.add(editBtn, BorderLayout.EAST);

            subSection.add(valueDisplay);
            parent.add(subSection);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Peso");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JLabel pesoLabel = new JLabel("Peso Atual");
            pesoLabel.setForeground(new Color(136, 136, 136));
            pesoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            subSection.add(pesoLabel);

            JPanel pesoDisplay = new JPanel(new BorderLayout());
            pesoDisplay.setOpaque(false);
            pesoDisplay.setBackground(new Color(13, 13, 13));
            pesoDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            String pesoAtual = peso.isNull("atual") ? "--" : peso.getDouble("atual") + " kg";
            JLabel atualLabel = new JLabel(pesoAtual);
            atualLabel.setForeground(new Color(238, 238, 238));
            atualLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            pesoDisplay.add(atualLabel, BorderLayout.WEST);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            btnPanel.setOpaque(false);
            JButton addPesoBtn = new JButton("Registrar");
            addPesoBtn.setBackground(new Color(26, 58, 26));
            addPesoBtn.setForeground(new Color(139, 195, 74));
            addPesoBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            addPesoBtn.setFocusPainted(false);
            addPesoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addPesoBtn.addActionListener(e -> mostrarRegistrarPeso());
            btnPanel.add(addPesoBtn);

            JButton histBtn = new JButton("Histórico");
            histBtn.setBackground(new Color(42, 42, 42));
            histBtn.setForeground(new Color(204, 204, 204));
            histBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            histBtn.setFocusPainted(false);
            histBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            histBtn.addActionListener(e -> mostrarHistoricoPeso());
            btnPanel.add(histBtn);
            pesoDisplay.add(btnPanel, BorderLayout.EAST);

            subSection.add(pesoDisplay);

            JLabel intervaloLabel = new JLabel("Intervalo para Pesagem (dias)");
            intervaloLabel.setForeground(new Color(136, 136, 136));
            intervaloLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            subSection.add(intervaloLabel);

            JPanel intervaloDisplay = new JPanel(new BorderLayout());
            intervaloDisplay.setOpaque(false);
            intervaloDisplay.setBackground(new Color(13, 13, 13));
            intervaloDisplay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            JLabel intervaloVal = new JLabel(peso.getInt("intervalo") + " dias");
            intervaloVal.setForeground(new Color(238, 238, 238));
            intervaloVal.setFont(new Font("Arial", Font.PLAIN, 14));
            intervaloDisplay.add(intervaloVal, BorderLayout.WEST);

            JButton editIntervaloBtn = new JButton("Editar");
            editIntervaloBtn.setBackground(new Color(26, 58, 26));
            editIntervaloBtn.setForeground(new Color(139, 195, 74));
            editIntervaloBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            editIntervaloBtn.setFocusPainted(false);
            editIntervaloBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editIntervaloBtn.addActionListener(e -> mostrarEditarIntervalo());
            intervaloDisplay.add(editIntervaloBtn, BorderLayout.EAST);

            subSection.add(intervaloDisplay);

            if (!peso.isNull("meta")) {
                JLabel metaLabel = new JLabel("Meta: " + peso.getDouble("meta") + " kg");
                metaLabel.setForeground(new Color(102, 102, 102));
                metaLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(metaLabel);
            }

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Dias de Descanso");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JPanel gridPanel = new JPanel(new GridLayout(0, 3, 10, 5));
            gridPanel.setOpaque(false);
            JSONArray diasDescanso = academia.getJSONArray("diasDescanso");
            for (String d : DIAS_SEMANA) {
                boolean checked = false;
                for (int i = 0; i < diasDescanso.length(); i++) {
                    if (diasDescanso.getString(i).equals(d)) {
                        checked = true;
                        break;
                    }
                }
                JCheckBox cb = new JCheckBox(d);
                cb.setSelected(checked);
                cb.setForeground(new Color(170, 170, 170));
                cb.setBackground(new Color(13, 13, 13));
                cb.setFocusPainted(false);
                cb.addActionListener(e -> {
                    try {
                        JSONArray novos = new JSONArray();
                        Component[] comps = ((JPanel) cb.getParent()).getComponents();
                        for (Component comp : comps) {
                            if (comp instanceof JCheckBox) {
                                JCheckBox c = (JCheckBox) comp;
                                if (c.isSelected()) novos.put(c.getText());
                            }
                        }
                        configData.getJSONObject("academia").put("diasDescanso", novos);
                        salvarDados();
                        renderDados();
                    } catch (JSONException ex) {}
                });
                gridPanel.add(cb);
            }
            subSection.add(gridPanel);
            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Objetivos");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONArray objetivos = academia.getJSONArray("objetivos");
            for (int i = 0; i < objetivos.length(); i++) {
                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setBackground(new Color(13, 13, 13));
                item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(42, 42, 42)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                JLabel lbl = new JLabel(objetivos.getString(i));
                lbl.setForeground(new Color(238, 238, 238));
                lbl.setFont(new Font("Arial", Font.PLAIN, 13));
                item.add(lbl, BorderLayout.WEST);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
                actions.setOpaque(false);
                JButton editObj = new JButton("✎");
                editObj.setForeground(new Color(136, 170, 255));
                editObj.setBackground(null);
                editObj.setBorder(null);
                editObj.setFocusPainted(false);
                editObj.setCursor(new Cursor(Cursor.HAND_CURSOR));
                int idx = i;
                editObj.addActionListener(e -> mostrarEditarObjetivo(idx));
                actions.add(editObj);

                JButton delObj = new JButton("✕");
                delObj.setForeground(new Color(255, 102, 102));
                delObj.setBackground(null);
                delObj.setBorder(null);
                delObj.setFocusPainted(false);
                delObj.setCursor(new Cursor(Cursor.HAND_CURSOR));
                delObj.addActionListener(e -> {
                    mostrarConfirmacao("Excluir Objetivo", "Tem certeza que deseja excluir este objetivo?", () -> {
                        try {
                            JSONArray objs = configData.getJSONObject("academia").getJSONArray("objetivos");
                            objs.remove(idx);
                            salvarDados();
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                });
                actions.add(delObj);
                item.add(actions, BorderLayout.EAST);
                subSection.add(item);
            }

            JButton addObjBtn = new JButton("+ Adicionar Objetivo");
            addObjBtn.setBackground(new Color(26, 58, 26));
            addObjBtn.setForeground(new Color(139, 195, 74));
            addObjBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            addObjBtn.setFocusPainted(false);
            addObjBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addObjBtn.addActionListener(e -> mostrarAdicionarObjetivo());
            subSection.add(addObjBtn);

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Treinos");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONArray treinos = academia.getJSONArray("treinos");
            for (int i = 0; i < treinos.length(); i++) {
                JSONObject treino = treinos.getJSONObject(i);
                JPanel treinoPanel = new JPanel();
                treinoPanel.setLayout(new BoxLayout(treinoPanel, BoxLayout.Y_AXIS));
                treinoPanel.setOpaque(false);
                treinoPanel.setBackground(new Color(13, 13, 13));
                treinoPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(42, 42, 42)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));

                JPanel headerPanel = new JPanel(new BorderLayout());
                headerPanel.setOpaque(false);
                JLabel nomeLabel = new JLabel(treino.getString("nome"));
                nomeLabel.setForeground(new Color(238, 238, 238));
                nomeLabel.setFont(new Font("Arial", Font.BOLD, 13));
                headerPanel.add(nomeLabel, BorderLayout.WEST);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
                actions.setOpaque(false);
                JButton editTreino = new JButton("✎");
                editTreino.setForeground(new Color(136, 170, 255));
                editTreino.setBackground(null);
                editTreino.setBorder(null);
                editTreino.setFocusPainted(false);
                editTreino.setCursor(new Cursor(Cursor.HAND_CURSOR));
                int treinoIdx = i;
                editTreino.addActionListener(e -> mostrarEditarTreino(treinoIdx));
                actions.add(editTreino);

                JButton delTreino = new JButton("✕");
                delTreino.setForeground(new Color(255, 102, 102));
                delTreino.setBackground(null);
                delTreino.setBorder(null);
                delTreino.setFocusPainted(false);
                delTreino.setCursor(new Cursor(Cursor.HAND_CURSOR));
                delTreino.addActionListener(e -> {
                    mostrarConfirmacao("Excluir Treino", "Tem certeza que deseja excluir este treino?", () -> {
                        try {
                            JSONArray ts = configData.getJSONObject("academia").getJSONArray("treinos");
                            ts.remove(treinoIdx);
                            salvarDados();
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                });
                actions.add(delTreino);
                headerPanel.add(actions, BorderLayout.EAST);

                treinoPanel.add(headerPanel);

                JLabel diaLabel = new JLabel("Dia: " + treino.getString("dia"));
                diaLabel.setForeground(new Color(170, 170, 170));
                diaLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                treinoPanel.add(diaLabel);

                if (treino.has("objetivo") && !treino.isNull("objetivo") && !treino.getString("objetivo").isEmpty()) {
                    JLabel objLabel = new JLabel("Objetivo: " + treino.getString("objetivo"));
                    objLabel.setForeground(new Color(170, 170, 170));
                    objLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                    treinoPanel.add(objLabel);
                }

                JPanel exContainer = new JPanel();
                exContainer.setLayout(new BoxLayout(exContainer, BoxLayout.Y_AXIS));
                exContainer.setOpaque(false);
                treinoPanel.add(exContainer);

                renderExerciciosLista(treinoIdx, exContainer);

                JButton addExBtn = new JButton("+ Exercício");
                addExBtn.setBackground(new Color(26, 58, 26));
                addExBtn.setForeground(new Color(139, 195, 74));
                addExBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
                addExBtn.setFocusPainted(false);
                addExBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                addExBtn.addActionListener(e -> mostrarAdicionarExercicio(treinoIdx));
                treinoPanel.add(addExBtn);

                subSection.add(treinoPanel);
                subSection.add(Box.createRigidArea(new Dimension(0, 5)));
            }

            JButton addTreinoBtn = new JButton("+ Adicionar Treino");
            addTreinoBtn.setBackground(new Color(26, 58, 26));
            addTreinoBtn.setForeground(new Color(139, 195, 74));
            addTreinoBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            addTreinoBtn.setFocusPainted(false);
            addTreinoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addTreinoBtn.addActionListener(e -> mostrarAdicionarTreino());
            subSection.add(addTreinoBtn);

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Roupas");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONObject roupas = academia.getJSONObject("roupas");
            String[] cats = {"camisas", "calcas", "tenis"};
            String[] catLabels = {"Camisas", "Calças", "Tênis"};
            for (int c = 0; c < cats.length; c++) {
                JLabel catLabel = new JLabel(catLabels[c]);
                catLabel.setForeground(new Color(136, 136, 136));
                catLabel.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(catLabel);

                JSONArray items = roupas.getJSONArray(cats[c]);
                for (int i = 0; i < items.length(); i++) {
                    JPanel item = new JPanel(new BorderLayout());
                    item.setOpaque(false);
                    item.setBackground(new Color(13, 13, 13));
                    item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(42, 42, 42)),
                        BorderFactory.createEmptyBorder(3, 6, 3, 6)
                    ));
                    JLabel lbl = new JLabel(items.getString(i));
                    lbl.setForeground(new Color(204, 204, 204));
                    lbl.setFont(new Font("Arial", Font.PLAIN, 12));
                    item.add(lbl, BorderLayout.WEST);

                    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
                    actions.setOpaque(false);
                    JButton editRoupa = new JButton("✎");
                    editRoupa.setForeground(new Color(136, 170, 255));
                    editRoupa.setBackground(null);
                    editRoupa.setBorder(null);
                    editRoupa.setFocusPainted(false);
                    editRoupa.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    int roupaIdx = i;
                    String catFinal = cats[c];
                    editRoupa.addActionListener(e -> mostrarEditarRoupa(catFinal, roupaIdx));
                    actions.add(editRoupa);

                    JButton delRoupa = new JButton("✕");
                    delRoupa.setForeground(new Color(255, 102, 102));
                    delRoupa.setBackground(null);
                    delRoupa.setBorder(null);
                    delRoupa.setFocusPainted(false);
                    delRoupa.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    delRoupa.addActionListener(e -> {
                        mostrarConfirmacao("Excluir Roupa", "Tem certeza que deseja excluir esta roupa?", () -> {
                            try {
                                JSONObject r = configData.getJSONObject("academia").getJSONObject("roupas");
                                r.getJSONArray(catFinal).remove(roupaIdx);
                                salvarDados();
                                renderDados();
                            } catch (JSONException ex) {}
                        });
                    });
                    actions.add(delRoupa);
                    item.add(actions, BorderLayout.EAST);
                    subSection.add(item);
                }

                JButton addRoupaBtn = new JButton("+ Adicionar " + catLabels[c].toLowerCase());
                addRoupaBtn.setBackground(new Color(26, 58, 26));
                addRoupaBtn.setForeground(new Color(139, 195, 74));
                addRoupaBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
                addRoupaBtn.setFocusPainted(false);
                addRoupaBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                String catFinal2 = cats[c];
                addRoupaBtn.addActionListener(e -> mostrarAdicionarRoupa(catFinal2));
                subSection.add(addRoupaBtn);
                subSection.add(Box.createRigidArea(new Dimension(0, 3)));
            }

            parent.add(subSection);

            subSection = new JPanel();
            subSection.setLayout(new BoxLayout(subSection, BoxLayout.Y_AXIS));
            subSection.setOpaque(false);
            subSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 42, 42)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
            ));

            subTitle = new JLabel("Combinações por Dia");
            subTitle.setForeground(new Color(153, 153, 153));
            subTitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subSection.add(subTitle);

            JSONObject combinacoes = academia.getJSONObject("combinacoes");
            JSONArray diasDescanso2 = academia.getJSONArray("diasDescanso");
            JSONArray workingDays = new JSONArray();
            for (String d : DIAS_SEMANA) {
                boolean isDescanso = false;
                for (int i = 0; i < diasDescanso2.length(); i++) {
                    if (diasDescanso2.getString(i).equals(d)) {
                        isDescanso = true;
                        break;
                    }
                }
                if (!isDescanso) workingDays.put(d);
            }

            if (workingDays.length() > 0) {
                for (int i = 0; i < workingDays.length(); i++) {
                    String day = workingDays.getString(i);
                    JPanel dayPanel = new JPanel();
                    dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
                    dayPanel.setOpaque(false);
                    dayPanel.setBackground(new Color(13, 13, 13));
                    dayPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(26, 26, 26)),
                        BorderFactory.createEmptyBorder(4, 6, 4, 6)
                    ));

                    JLabel dayTitle = new JLabel(day);
                    dayTitle.setForeground(new Color(170, 170, 170));
                    dayTitle.setFont(new Font("Arial", Font.BOLD, 12));
                    dayPanel.add(dayTitle);

                    JSONArray combos = combinacoes.has(day) ? combinacoes.getJSONArray(day) : new JSONArray();
                    for (int j = 0; j < combos.length(); j++) {
                        JPanel comboItem = new JPanel(new BorderLayout());
                        comboItem.setOpaque(false);
                        JLabel comboText = new JLabel(combos.getString(j));
                        comboText.setForeground(new Color(204, 204, 204));
                        comboText.setFont(new Font("Arial", Font.PLAIN, 12));
                        comboItem.add(comboText, BorderLayout.WEST);

                        JButton delCombo = new JButton("✕");
                        delCombo.setForeground(new Color(255, 102, 102));
                        delCombo.setBackground(null);
                        delCombo.setBorder(null);
                        delCombo.setFocusPainted(false);
                        delCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        int comboIdx = j;
                        String dayFinal = day;
                        delCombo.addActionListener(e -> {
                            mostrarConfirmacao("Excluir Combinação", "Tem certeza que deseja excluir esta combinação?", () -> {
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
                        comboItem.add(delCombo, BorderLayout.EAST);
                        dayPanel.add(comboItem);
                    }

                    JSONObject roupas2 = academia.getJSONObject("roupas");
                    JPanel comboSelects = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
                    comboSelects.setOpaque(false);

                    JComboBox<String> camisaCombo = new JComboBox<>();
                    camisaCombo.addItem("");
                    JSONArray camisas = roupas2.getJSONArray("camisas");
                    for (int j = 0; j < camisas.length(); j++) camisaCombo.addItem(camisas.getString(j));

                    JComboBox<String> calcaCombo = new JComboBox<>();
                    calcaCombo.addItem("");
                    JSONArray calcas = roupas2.getJSONArray("calcas");
                    for (int j = 0; j < calcas.length(); j++) calcaCombo.addItem(calcas.getString(j));

                    JComboBox<String> tenisCombo = new JComboBox<>();
                    tenisCombo.addItem("");
                    JSONArray tenis = roupas2.getJSONArray("tenis");
                    for (int j = 0; j < tenis.length(); j++) tenisCombo.addItem(tenis.getString(j));

                    comboSelects.add(camisaCombo);
                    comboSelects.add(calcaCombo);
                    comboSelects.add(tenisCombo);

                    JButton addComboBtn = new JButton("Adicionar");
                    addComboBtn.setBackground(new Color(26, 58, 26));
                    addComboBtn.setForeground(new Color(139, 195, 74));
                    addComboBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
                    addComboBtn.setFocusPainted(false);
                    addComboBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    String dayFinal2 = day;
                    addComboBtn.addActionListener(e -> {
                        try {
                            String cam = (String) camisaCombo.getSelectedItem();
                            String cal = (String) calcaCombo.getSelectedItem();
                            String ten = (String) tenisCombo.getSelectedItem();
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
                                JOptionPane.showMessageDialog(null, "Selecione pelo menos uma peça.");
                                return;
                            }
                            JSONObject comb = configData.getJSONObject("academia").getJSONObject("combinacoes");
                            if (!comb.has(dayFinal2)) comb.put(dayFinal2, new JSONArray());
                            comb.getJSONArray(dayFinal2).put(comboText);
                            salvarDados();
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                    comboSelects.add(addComboBtn);
                    dayPanel.add(comboSelects);

                    subSection.add(dayPanel);
                    subSection.add(Box.createRigidArea(new Dimension(0, 3)));
                }
            } else {
                JLabel empty = new JLabel("Nenhum dia disponível. Defina os dias de descanso primeiro.");
                empty.setForeground(new Color(102, 102, 102));
                empty.setFont(new Font("Arial", Font.PLAIN, 11));
                subSection.add(empty);
            }

            parent.add(subSection);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void renderExerciciosLista(int treinoIdx, JPanel container) {
        container.removeAll();
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            if (!treino.has("exercicios")) return;

            JSONArray exercicios = treino.getJSONArray("exercicios");
            for (int i = 0; i < exercicios.length(); i++) {
                JSONObject ex = exercicios.getJSONObject(i);
                JPanel exItem = new JPanel(new BorderLayout());
                exItem.setOpaque(false);
                exItem.setBackground(new Color(13, 13, 13));
                exItem.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(42, 42, 42)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));

                String details = ex.getString("exercise");
                if (ex.has("sets")) details += " • " + ex.getInt("sets") + "x" + ex.getInt("reps");
                if (ex.has("load")) details += " • " + ex.getDouble("load") + "kg";

                JLabel info = new JLabel(details);
                info.setForeground(new Color(204, 204, 204));
                info.setFont(new Font("Arial", Font.PLAIN, 12));
                exItem.add(info, BorderLayout.WEST);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
                actions.setOpaque(false);
                JButton editEx = new JButton("✎");
                editEx.setForeground(new Color(136, 170, 255));
                editEx.setBackground(null);
                editEx.setBorder(null);
                editEx.setFocusPainted(false);
                editEx.setCursor(new Cursor(Cursor.HAND_CURSOR));
                int exIdx = i;
                editEx.addActionListener(e -> mostrarEditarExercicio(treinoIdx, exIdx));
                actions.add(editEx);

                JButton delEx = new JButton("✕");
                delEx.setForeground(new Color(255, 102, 102));
                delEx.setBackground(null);
                delEx.setBorder(null);
                delEx.setFocusPainted(false);
                delEx.setCursor(new Cursor(Cursor.HAND_CURSOR));
                delEx.addActionListener(e -> {
                    mostrarConfirmacao("Excluir Exercício", "Tem certeza que deseja excluir este exercício?", () -> {
                        try {
                            JSONArray ts = configData.getJSONObject("academia").getJSONArray("treinos");
                            JSONObject t = ts.getJSONObject(treinoIdx);
                            t.getJSONArray("exercicios").remove(exIdx);
                            salvarDados();
                            renderDados();
                        } catch (JSONException exc) {}
                    });
                });
                actions.add(delEx);
                exItem.add(actions, BorderLayout.EAST);

                container.add(exItem);
                container.add(Box.createRigidArea(new Dimension(0, 3)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        container.revalidate();
        container.repaint();
    }

    private void addLabel(JPanel parent, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(136, 136, 136));
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        parent.add(label);
        parent.add(Box.createRigidArea(new Dimension(0, 3)));
    }

    private void mostrarEditarInicio() {
        subModalBox.removeAll();
        JLabel title = new JLabel("Definir Data de Início");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        String current = "";
        try {
            current = configData.getJSONObject("academia").isNull("inicio") ? "" : configData.getJSONObject("academia").getString("inicio");
        } catch (JSONException e) {}
        JTextField dateField = new JTextField(current);
        dateField.setMaximumSize(new Dimension(300, 30));
        dateField.setBackground(new Color(10, 10, 10));
        dateField.setForeground(new Color(221, 221, 221));
        dateField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        dateField.setHorizontalAlignment(JTextField.CENTER);
        dateField.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(dateField);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            String val = dateField.getText().trim();
            if (val.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Selecione uma data válida.");
                return;
            }
            try {
                configData.getJSONObject("academia").put("inicio", val);
                salvarDados();
                subModal.setVisible(false);
                renderDados();
            } catch (JSONException ex) {}
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarEditarIntervalo() {
        subModalBox.removeAll();
        JLabel title = new JLabel("Intervalo para Pesagem");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        int current = 7;
        try {
            current = configData.getJSONObject("academia").getJSONObject("peso").getInt("intervalo");
        } catch (JSONException e) {}
        JTextField field = new JTextField(String.valueOf(current));
        field.setMaximumSize(new Dimension(200, 30));
        field.setBackground(new Color(10, 10, 10));
        field.setForeground(new Color(221, 221, 221));
        field.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(field);

        JLabel note = new JLabel("A cada quantos dias você deve pesar?");
        note.setForeground(new Color(102, 102, 102));
        note.setFont(new Font("Arial", Font.PLAIN, 11));
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(note);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            try {
                int val = Integer.parseInt(field.getText().trim());
                if (val < 1) throw new NumberFormatException();
                configData.getJSONObject("academia").getJSONObject("peso").put("intervalo", val);
                salvarDados();
                subModal.setVisible(false);
                renderDados();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Valor inválido.");
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarRegistrarPeso() {
        subModalBox.removeAll();
        JLabel title = new JLabel("Registrar Peso");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        JLabel pesoLabel = new JLabel("Peso (kg)");
        pesoLabel.setForeground(new Color(136, 136, 136));
        pesoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        pesoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(pesoLabel);

        JTextField pesoField = new JTextField(10);
        pesoField.setMaximumSize(new Dimension(200, 30));
        pesoField.setBackground(new Color(10, 10, 10));
        pesoField.setForeground(new Color(221, 221, 221));
        pesoField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        pesoField.setHorizontalAlignment(JTextField.CENTER);
        pesoField.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(pesoField);

        JLabel metaLabel = new JLabel("Meta (opcional)");
        metaLabel.setForeground(new Color(136, 136, 136));
        metaLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        metaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(metaLabel);

        double currentMeta = 0;
        try {
            currentMeta = configData.getJSONObject("academia").getJSONObject("peso").isNull("meta") ? 0 : configData.getJSONObject("academia").getJSONObject("peso").getDouble("meta");
        } catch (JSONException e) {}
        JTextField metaField = new JTextField(currentMeta > 0 ? String.valueOf(currentMeta) : "");
        metaField.setMaximumSize(new Dimension(200, 30));
        metaField.setBackground(new Color(10, 10, 10));
        metaField.setForeground(new Color(221, 221, 221));
        metaField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        metaField.setHorizontalAlignment(JTextField.CENTER);
        metaField.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(metaField);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            try {
                double peso = Double.parseDouble(pesoField.getText().trim());
                if (peso <= 0) throw new NumberFormatException();
                String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                JSONObject pesoObj = configData.getJSONObject("academia").getJSONObject("peso");
                JSONArray historico = pesoObj.getJSONArray("historico");
                JSONObject novo = new JSONObject();
                novo.put("peso", peso);
                novo.put("data", hoje);
                historico.put(novo);
                pesoObj.put("atual", peso);
                pesoObj.put("ultimoRegistro", LocalDateTime.now().toString());

                String metaStr = metaField.getText().trim();
                if (!metaStr.isEmpty()) {
                    double meta = Double.parseDouble(metaStr);
                    if (meta > 0) pesoObj.put("meta", meta);
                }

                salvarDados();
                subModal.setVisible(false);
                renderDados();
                if (treinoAtual != null) renderTreinoCard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Peso inválido.");
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarHistoricoPeso() {
        subModalBox.removeAll();
        JLabel title = new JLabel("Histórico de Peso");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setMaximumSize(new Dimension(380, 200));
        scroll.setPreferredSize(new Dimension(380, 200));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(26, 26, 26)));

        try {
            JSONArray historico = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
            for (int i = 0; i < historico.length(); i++) {
                JSONObject item = historico.getJSONObject(i);
                JPanel entry = new JPanel(new BorderLayout());
                entry.setOpaque(false);
                entry.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(26, 26, 26)),
                    BorderFactory.createEmptyBorder(3, 6, 3, 6)
                ));
                JLabel info = new JLabel(item.getDouble("peso") + " kg (" + item.getString("data") + ")");
                info.setForeground(new Color(187, 187, 187));
                info.setFont(new Font("Arial", Font.PLAIN, 12));
                entry.add(info, BorderLayout.WEST);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
                actions.setOpaque(false);
                int idx = i;
                JButton editBtn = new JButton("✎");
                editBtn.setForeground(new Color(136, 170, 255));
                editBtn.setBackground(null);
                editBtn.setBorder(null);
                editBtn.setFocusPainted(false);
                editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                editBtn.addActionListener(e -> mostrarEditarPeso(idx));
                actions.add(editBtn);

                JButton delBtn = new JButton("✕");
                delBtn.setForeground(new Color(255, 102, 102));
                delBtn.setBackground(null);
                delBtn.setBorder(null);
                delBtn.setFocusPainted(false);
                delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                delBtn.addActionListener(e -> {
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
                            subModal.setVisible(false);
                            renderDados();
                        } catch (JSONException ex) {}
                    });
                });
                actions.add(delBtn);
                entry.add(actions, BorderLayout.EAST);

                listPanel.add(entry);
            }
        } catch (JSONException e) {}

        if (listPanel.getComponentCount() == 0) {
            JLabel empty = new JLabel("Nenhum registro.");
            empty.setForeground(new Color(102, 102, 102));
            empty.setFont(new Font("Arial", Font.PLAIN, 11));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(empty);
        }

        subModalBox.add(scroll);

        JButton delAllBtn = new JButton("Excluir Todo Histórico");
        delAllBtn.setBackground(new Color(58, 26, 26));
        delAllBtn.setForeground(new Color(255, 102, 102));
        delAllBtn.setBorder(BorderFactory.createLineBorder(new Color(90, 42, 42)));
        delAllBtn.setFocusPainted(false);
        delAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        delAllBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        delAllBtn.addActionListener(e -> {
            mostrarConfirmacao("Excluir Histórico", "Tem certeza que deseja excluir todo o histórico de peso?", () -> {
                try {
                    JSONArray hist = new JSONArray();
                    configData.getJSONObject("academia").getJSONObject("peso").put("historico", hist);
                    configData.getJSONObject("academia").getJSONObject("peso").put("atual", JSONObject.NULL);
                    configData.getJSONObject("academia").getJSONObject("peso").put("ultimoRegistro", JSONObject.NULL);
                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (JSONException ex) {}
            });
        });
        subModalBox.add(delAllBtn);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setOpaque(false);
        JButton closeBtn = new JButton("Fechar");
        closeBtn.setBackground(new Color(42, 42, 42));
        closeBtn.setForeground(new Color(204, 204, 204));
        closeBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> subModal.setVisible(false));
        btnRow.add(closeBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarEditarPeso(int idx) {
        try {
            JSONArray historico = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
            JSONObject item = historico.getJSONObject(idx);

            subModalBox.removeAll();
            JLabel title = new JLabel("Editar Registro");
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JLabel pesoLabel = new JLabel("Peso (kg)");
            pesoLabel.setForeground(new Color(136, 136, 136));
            pesoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            pesoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(pesoLabel);

            JTextField pesoField = new JTextField(String.valueOf(item.getDouble("peso")));
            pesoField.setMaximumSize(new Dimension(200, 30));
            pesoField.setBackground(new Color(10, 10, 10));
            pesoField.setForeground(new Color(221, 221, 221));
            pesoField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            pesoField.setHorizontalAlignment(JTextField.CENTER);
            pesoField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(pesoField);

            JLabel dataLabel = new JLabel("Data");
            dataLabel.setForeground(new Color(136, 136, 136));
            dataLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            dataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(dataLabel);

            JTextField dataField = new JTextField(item.getString("data"));
            dataField.setMaximumSize(new Dimension(200, 30));
            dataField.setBackground(new Color(10, 10, 10));
            dataField.setForeground(new Color(221, 221, 221));
            dataField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            dataField.setHorizontalAlignment(JTextField.CENTER);
            dataField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(dataField);
            subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            btnRow.setOpaque(false);
            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.setBackground(new Color(42, 42, 42));
            cancelBtn.setForeground(new Color(204, 204, 204));
            cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> subModal.setVisible(false));

            JButton saveBtn = new JButton("Salvar");
            saveBtn.setBackground(new Color(26, 58, 26));
            saveBtn.setForeground(new Color(139, 195, 74));
            saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            saveBtn.setFocusPainted(false);
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> {
                try {
                    double peso = Double.parseDouble(pesoField.getText().trim());
                    String data = dataField.getText().trim();
                    if (peso <= 0 || data.isEmpty()) throw new NumberFormatException();
                    JSONArray hist = configData.getJSONObject("academia").getJSONObject("peso").getJSONArray("historico");
                    JSONObject updated = hist.getJSONObject(idx);
                    updated.put("peso", peso);
                    updated.put("data", data);
                    if (idx == hist.length() - 1) {
                        configData.getJSONObject("academia").getJSONObject("peso").put("atual", peso);
                    }
                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Valores inválidos.");
                }
            });

            btnRow.add(cancelBtn);
            btnRow.add(saveBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarObjetivo() {
        subModalBox.removeAll();
        JLabel title = new JLabel("Novo Objetivo");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        JTextField field = new JTextField(20);
        field.setMaximumSize(new Dimension(300, 30));
        field.setBackground(new Color(10, 10, 10));
        field.setForeground(new Color(221, 221, 221));
        field.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(field);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            String val = field.getText().trim();
            if (val.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Digite um objetivo.");
                return;
            }
            try {
                configData.getJSONObject("academia").getJSONArray("objetivos").put(val);
                salvarDados();
                subModal.setVisible(false);
                renderDados();
            } catch (JSONException ex) {}
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarEditarObjetivo(int idx) {
        try {
            JSONArray objetivos = configData.getJSONObject("academia").getJSONArray("objetivos");
            String current = objetivos.getString(idx);

            subModalBox.removeAll();
            JLabel title = new JLabel("Editar Objetivo");
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JTextField field = new JTextField(current);
            field.setMaximumSize(new Dimension(300, 30));
            field.setBackground(new Color(10, 10, 10));
            field.setForeground(new Color(221, 221, 221));
            field.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(field);
            subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            btnRow.setOpaque(false);
            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.setBackground(new Color(42, 42, 42));
            cancelBtn.setForeground(new Color(204, 204, 204));
            cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> subModal.setVisible(false));

            JButton saveBtn = new JButton("Salvar");
            saveBtn.setBackground(new Color(26, 58, 26));
            saveBtn.setForeground(new Color(139, 195, 74));
            saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            saveBtn.setFocusPainted(false);
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> {
                String val = field.getText().trim();
                if (val.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Digite um objetivo.");
                    return;
                }
                try {
                    JSONArray objs = configData.getJSONObject("academia").getJSONArray("objetivos");
                    objs.put(idx, val);
                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (JSONException ex) {}
            });

            btnRow.add(cancelBtn);
            btnRow.add(saveBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarTreino() {
        subModalBox.removeAll();
        JLabel title = new JLabel("Novo Treino");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        JLabel nomeLabel = new JLabel("Nome");
        nomeLabel.setForeground(new Color(136, 136, 136));
        nomeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(nomeLabel);

        JTextField nomeField = new JTextField(20);
        nomeField.setMaximumSize(new Dimension(300, 30));
        nomeField.setBackground(new Color(10, 10, 10));
        nomeField.setForeground(new Color(221, 221, 221));
        nomeField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        nomeField.setHorizontalAlignment(JTextField.CENTER);
        nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(nomeField);

        JLabel diaLabel = new JLabel("Dia *");
        diaLabel.setForeground(new Color(136, 136, 136));
        diaLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        diaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(diaLabel);

        JComboBox<String> diaCombo = new JComboBox<>();
        diaCombo.addItem("");
        for (String d : DIAS_SEMANA) diaCombo.addItem(d);
        diaCombo.setMaximumSize(new Dimension(300, 30));
        diaCombo.setBackground(new Color(10, 10, 10));
        diaCombo.setForeground(new Color(221, 221, 221));
        diaCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(diaCombo);

        JLabel objLabel = new JLabel("Objetivo (opcional)");
        objLabel.setForeground(new Color(136, 136, 136));
        objLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        objLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(objLabel);

        JComboBox<String> objCombo = new JComboBox<>();
        objCombo.addItem("");
        try {
            JSONArray objetivos = configData.getJSONObject("academia").getJSONArray("objetivos");
            for (int i = 0; i < objetivos.length(); i++) objCombo.addItem(objetivos.getString(i));
        } catch (JSONException e) {}
        objCombo.setMaximumSize(new Dimension(300, 30));
        objCombo.setBackground(new Color(10, 10, 10));
        objCombo.setForeground(new Color(221, 221, 221));
        objCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(objCombo);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            String nome = nomeField.getText().trim();
            String dia = (String) diaCombo.getSelectedItem();
            String obj = (String) objCombo.getSelectedItem();
            if (nome.isEmpty() || dia == null || dia.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nome e dia são obrigatórios.");
                return;
            }
            try {
                JSONObject treino = new JSONObject();
                treino.put("nome", nome);
                treino.put("dia", dia);
                treino.put("objetivo", obj != null && !obj.isEmpty() ? obj : JSONObject.NULL);
                treino.put("exercicios", new JSONArray());
                configData.getJSONObject("academia").getJSONArray("treinos").put(treino);
                salvarDados();
                subModal.setVisible(false);
                renderDados();
            } catch (JSONException ex) {}
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarEditarTreino(int idx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(idx);

            subModalBox.removeAll();
            JLabel title = new JLabel("Editar Treino");
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JLabel nomeLabel = new JLabel("Nome");
            nomeLabel.setForeground(new Color(136, 136, 136));
            nomeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            nomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(nomeLabel);

            JTextField nomeField = new JTextField(treino.getString("nome"));
            nomeField.setMaximumSize(new Dimension(300, 30));
            nomeField.setBackground(new Color(10, 10, 10));
            nomeField.setForeground(new Color(221, 221, 221));
            nomeField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            nomeField.setHorizontalAlignment(JTextField.CENTER);
            nomeField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(nomeField);

            JLabel diaLabel = new JLabel("Dia *");
            diaLabel.setForeground(new Color(136, 136, 136));
            diaLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            diaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(diaLabel);

            JComboBox<String> diaCombo = new JComboBox<>();
            diaCombo.addItem("");
            for (String d : DIAS_SEMANA) diaCombo.addItem(d);
            diaCombo.setSelectedItem(treino.getString("dia"));
            diaCombo.setMaximumSize(new Dimension(300, 30));
            diaCombo.setBackground(new Color(10, 10, 10));
            diaCombo.setForeground(new Color(221, 221, 221));
            diaCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(diaCombo);

            JLabel objLabel = new JLabel("Objetivo (opcional)");
            objLabel.setForeground(new Color(136, 136, 136));
            objLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            objLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(objLabel);

            JComboBox<String> objCombo = new JComboBox<>();
            objCombo.addItem("");
            try {
                JSONArray objetivos = configData.getJSONObject("academia").getJSONArray("objetivos");
                for (int i = 0; i < objetivos.length(); i++) objCombo.addItem(objetivos.getString(i));
            } catch (JSONException e) {}
            if (treino.has("objetivo") && !treino.isNull("objetivo")) {
                objCombo.setSelectedItem(treino.getString("objetivo"));
            }
            objCombo.setMaximumSize(new Dimension(300, 30));
            objCombo.setBackground(new Color(10, 10, 10));
            objCombo.setForeground(new Color(221, 221, 221));
            objCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(objCombo);
            subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            btnRow.setOpaque(false);
            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.setBackground(new Color(42, 42, 42));
            cancelBtn.setForeground(new Color(204, 204, 204));
            cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> subModal.setVisible(false));

            JButton saveBtn = new JButton("Salvar");
            saveBtn.setBackground(new Color(26, 58, 26));
            saveBtn.setForeground(new Color(139, 195, 74));
            saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            saveBtn.setFocusPainted(false);
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> {
                String nome = nomeField.getText().trim();
                String dia = (String) diaCombo.getSelectedItem();
                String obj = (String) objCombo.getSelectedItem();
                if (nome.isEmpty() || dia == null || dia.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Nome e dia são obrigatórios.");
                    return;
                }
                try {
                    treino.put("nome", nome);
                    treino.put("dia", dia);
                    treino.put("objetivo", obj != null && !obj.isEmpty() ? obj : JSONObject.NULL);
                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (JSONException ex) {}
            });

            btnRow.add(cancelBtn);
            btnRow.add(saveBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarExercicio(int treinoIdx) {
        subModalBox.removeAll();
        JLabel title = new JLabel("Adicionar Exercício");
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        JLabel exLabel = new JLabel("Exercício *");
        exLabel.setForeground(new Color(136, 136, 136));
        exLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        exLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(exLabel);

        JTextField exField = new JTextField(20);
        exField.setMaximumSize(new Dimension(300, 30));
        exField.setBackground(new Color(10, 10, 10));
        exField.setForeground(new Color(221, 221, 221));
        exField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        exField.setHorizontalAlignment(JTextField.CENTER);
        exField.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(exField);

        JPanel formRow = new JPanel(new GridLayout(1, 3, 6, 0));
        formRow.setOpaque(false);
        formRow.setMaximumSize(new Dimension(300, 30));

        JPanel setPanel = new JPanel();
        setPanel.setLayout(new BoxLayout(setPanel, BoxLayout.Y_AXIS));
        setPanel.setOpaque(false);
        JLabel setLabel = new JLabel("Séries *");
        setLabel.setForeground(new Color(136, 136, 136));
        setLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        setLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        setPanel.add(setLabel);
        JTextField setField = new JTextField("3");
        setField.setBackground(new Color(10, 10, 10));
        setField.setForeground(new Color(221, 221, 221));
        setField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        setField.setHorizontalAlignment(JTextField.CENTER);
        setPanel.add(setField);
        formRow.add(setPanel);

        JPanel repPanel = new JPanel();
        repPanel.setLayout(new BoxLayout(repPanel, BoxLayout.Y_AXIS));
        repPanel.setOpaque(false);
        JLabel repLabel = new JLabel("Repetições *");
        repLabel.setForeground(new Color(136, 136, 136));
        repLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        repLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        repPanel.add(repLabel);
        JTextField repField = new JTextField("10");
        repField.setBackground(new Color(10, 10, 10));
        repField.setForeground(new Color(221, 221, 221));
        repField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        repField.setHorizontalAlignment(JTextField.CENTER);
        repPanel.add(repField);
        formRow.add(repPanel);

        JPanel loadPanel = new JPanel();
        loadPanel.setLayout(new BoxLayout(loadPanel, BoxLayout.Y_AXIS));
        loadPanel.setOpaque(false);
        JLabel loadLabel = new JLabel("Carga (kg) *");
        loadLabel.setForeground(new Color(136, 136, 136));
        loadLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        loadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadPanel.add(loadLabel);
        JTextField loadField = new JTextField("20");
        loadField.setBackground(new Color(10, 10, 10));
        loadField.setForeground(new Color(221, 221, 221));
        loadField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        loadField.setHorizontalAlignment(JTextField.CENTER);
        loadPanel.add(loadField);
        formRow.add(loadPanel);

        subModalBox.add(formRow);

        JLabel metaLabel = new JLabel("Meta de Carga (kg, opcional)");
        metaLabel.setForeground(new Color(136, 136, 136));
        metaLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        metaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(metaLabel);

        JTextField metaField = new JTextField(20);
        metaField.setMaximumSize(new Dimension(300, 30));
        metaField.setBackground(new Color(10, 10, 10));
        metaField.setForeground(new Color(221, 221, 221));
        metaField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        metaField.setHorizontalAlignment(JTextField.CENTER);
        metaField.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(metaField);

        JLabel descLabel = new JLabel("Descanso entre séries (opcional)");
        descLabel.setForeground(new Color(136, 136, 136));
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(descLabel);

        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        descPanel.setOpaque(false);
        JTextField descMinField = new JTextField(3);
        descMinField.setPreferredSize(new Dimension(50, 25));
        descMinField.setBackground(new Color(10, 10, 10));
        descMinField.setForeground(new Color(221, 221, 221));
        descMinField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        descMinField.setHorizontalAlignment(JTextField.CENTER);
        descPanel.add(descMinField);
        descPanel.add(new JLabel(":"));
        JTextField descSecField = new JTextField(3);
        descSecField.setPreferredSize(new Dimension(50, 25));
        descSecField.setBackground(new Color(10, 10, 10));
        descSecField.setForeground(new Color(221, 221, 221));
        descSecField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        descSecField.setHorizontalAlignment(JTextField.CENTER);
        descPanel.add(descSecField);
        subModalBox.add(descPanel);

        JCheckBox warmupCheck = new JCheckBox("Série de aquecimento");
        warmupCheck.setForeground(new Color(170, 170, 170));
        warmupCheck.setBackground(new Color(13, 13, 13));
        warmupCheck.setFocusPainted(false);
        warmupCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(warmupCheck);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            try {
                String exercise = exField.getText().trim();
                int sets = Integer.parseInt(setField.getText().trim());
                int reps = Integer.parseInt(repField.getText().trim());
                double load = Double.parseDouble(loadField.getText().trim());
                if (exercise.isEmpty() || sets < 1 || reps < 1 || load <= 0) {
                    throw new NumberFormatException();
                }
                JSONObject exercicio = new JSONObject();
                exercicio.put("exercise", exercise);
                exercicio.put("sets", sets);
                exercicio.put("reps", reps);
                exercicio.put("load", load);

                String metaStr = metaField.getText().trim();
                if (!metaStr.isEmpty()) {
                    double meta = Double.parseDouble(metaStr);
                    if (meta > 0) exercicio.put("metaCarga", meta);
                }

                int descMin = descMinField.getText().trim().isEmpty() ? 0 : Integer.parseInt(descMinField.getText().trim());
                int descSec = descSecField.getText().trim().isEmpty() ? 0 : Integer.parseInt(descSecField.getText().trim());
                int descanso = descMin * 60 + descSec;
                if (descanso > 0) exercicio.put("descanso", descanso);

                exercicio.put("warmup", warmupCheck.isSelected());
                exercicio.put("loadHistory", new JSONArray());

                JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
                JSONObject treino = treinos.getJSONObject(treinoIdx);
                if (!treino.has("exercicios")) treino.put("exercicios", new JSONArray());
                treino.getJSONArray("exercicios").put(exercicio);
                salvarDados();
                subModal.setVisible(false);
                renderDados();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Valores inválidos. Verifique os campos.");
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarEditarExercicio(int treinoIdx, int exIdx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            JSONObject ex = treino.getJSONArray("exercicios").getJSONObject(exIdx);

            subModalBox.removeAll();
            JLabel title = new JLabel("Editar Exercício");
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JLabel exLabel = new JLabel("Exercício *");
            exLabel.setForeground(new Color(136, 136, 136));
            exLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            exLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(exLabel);

            JTextField exField = new JTextField(ex.getString("exercise"));
            exField.setMaximumSize(new Dimension(300, 30));
            exField.setBackground(new Color(10, 10, 10));
            exField.setForeground(new Color(221, 221, 221));
            exField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            exField.setHorizontalAlignment(JTextField.CENTER);
            exField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(exField);

            JPanel formRow = new JPanel(new GridLayout(1, 3, 6, 0));
            formRow.setOpaque(false);
            formRow.setMaximumSize(new Dimension(300, 30));

            JPanel setPanel = new JPanel();
            setPanel.setLayout(new BoxLayout(setPanel, BoxLayout.Y_AXIS));
            setPanel.setOpaque(false);
            JLabel setLabel = new JLabel("Séries *");
            setLabel.setForeground(new Color(136, 136, 136));
            setLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            setLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            setPanel.add(setLabel);
            JTextField setField = new JTextField(String.valueOf(ex.getInt("sets")));
            setField.setBackground(new Color(10, 10, 10));
            setField.setForeground(new Color(221, 221, 221));
            setField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            setField.setHorizontalAlignment(JTextField.CENTER);
            setPanel.add(setField);
            formRow.add(setPanel);

            JPanel repPanel = new JPanel();
            repPanel.setLayout(new BoxLayout(repPanel, BoxLayout.Y_AXIS));
            repPanel.setOpaque(false);
            JLabel repLabel = new JLabel("Repetições *");
            repLabel.setForeground(new Color(136, 136, 136));
            repLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            repLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            repPanel.add(repLabel);
            JTextField repField = new JTextField(String.valueOf(ex.getInt("reps")));
            repField.setBackground(new Color(10, 10, 10));
            repField.setForeground(new Color(221, 221, 221));
            repField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            repField.setHorizontalAlignment(JTextField.CENTER);
            repPanel.add(repField);
            formRow.add(repPanel);

            JPanel loadPanel = new JPanel();
            loadPanel.setLayout(new BoxLayout(loadPanel, BoxLayout.Y_AXIS));
            loadPanel.setOpaque(false);
            JLabel loadLabel = new JLabel("Carga (kg) *");
            loadLabel.setForeground(new Color(136, 136, 136));
            loadLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            loadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            loadPanel.add(loadLabel);
            JTextField loadField = new JTextField(String.valueOf(ex.getDouble("load")));
            loadField.setBackground(new Color(10, 10, 10));
            loadField.setForeground(new Color(221, 221, 221));
            loadField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            loadField.setHorizontalAlignment(JTextField.CENTER);
            loadPanel.add(loadField);
            formRow.add(loadPanel);

            subModalBox.add(formRow);

            JLabel metaLabel = new JLabel("Meta de Carga (kg, opcional)");
            metaLabel.setForeground(new Color(136, 136, 136));
            metaLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            metaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(metaLabel);

            JTextField metaField = new JTextField(ex.has("metaCarga") && !ex.isNull("metaCarga") ? String.valueOf(ex.getDouble("metaCarga")) : "");
            metaField.setMaximumSize(new Dimension(300, 30));
            metaField.setBackground(new Color(10, 10, 10));
            metaField.setForeground(new Color(221, 221, 221));
            metaField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            metaField.setHorizontalAlignment(JTextField.CENTER);
            metaField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(metaField);

            JLabel descLabel = new JLabel("Descanso entre séries (opcional)");
            descLabel.setForeground(new Color(136, 136, 136));
            descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(descLabel);

            JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            descPanel.setOpaque(false);
            int descanso = ex.has("descanso") && !ex.isNull("descanso") ? ex.getInt("descanso") : 0;
            JTextField descMinField = new JTextField(String.valueOf(descanso / 60), 3);
            descMinField.setPreferredSize(new Dimension(50, 25));
            descMinField.setBackground(new Color(10, 10, 10));
            descMinField.setForeground(new Color(221, 221, 221));
            descMinField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            descMinField.setHorizontalAlignment(JTextField.CENTER);
            descPanel.add(descMinField);
            descPanel.add(new JLabel(":"));
            JTextField descSecField = new JTextField(String.valueOf(descanso % 60), 3);
            descSecField.setPreferredSize(new Dimension(50, 25));
            descSecField.setBackground(new Color(10, 10, 10));
            descSecField.setForeground(new Color(221, 221, 221));
            descSecField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            descSecField.setHorizontalAlignment(JTextField.CENTER);
            descPanel.add(descSecField);
            subModalBox.add(descPanel);

            JCheckBox warmupCheck = new JCheckBox("Série de aquecimento");
            warmupCheck.setSelected(ex.has("warmup") && ex.getBoolean("warmup"));
            warmupCheck.setForeground(new Color(170, 170, 170));
            warmupCheck.setBackground(new Color(13, 13, 13));
            warmupCheck.setFocusPainted(false);
            warmupCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(warmupCheck);

            JButton histBtn = new JButton("Editar Histórico de Carga");
            histBtn.setBackground(new Color(42, 42, 42));
            histBtn.setForeground(new Color(204, 204, 204));
            histBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            histBtn.setFocusPainted(false);
            histBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            histBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            histBtn.addActionListener(e -> {
                subModal.setVisible(false);
                mostrarHistoricoCarga(treinoIdx, exIdx);
            });
            subModalBox.add(histBtn);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            btnRow.setOpaque(false);
            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.setBackground(new Color(42, 42, 42));
            cancelBtn.setForeground(new Color(204, 204, 204));
            cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> subModal.setVisible(false));

            JButton saveBtn = new JButton("Salvar");
            saveBtn.setBackground(new Color(26, 58, 26));
            saveBtn.setForeground(new Color(139, 195, 74));
            saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            saveBtn.setFocusPainted(false);
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> {
                try {
                    String exercise = exField.getText().trim();
                    int sets = Integer.parseInt(setField.getText().trim());
                    int reps = Integer.parseInt(repField.getText().trim());
                    double load = Double.parseDouble(loadField.getText().trim());
                    if (exercise.isEmpty() || sets < 1 || reps < 1 || load <= 0) {
                        throw new NumberFormatException();
                    }
                    ex.put("exercise", exercise);
                    ex.put("sets", sets);
                    ex.put("reps", reps);
                    ex.put("load", load);

                    String metaStr = metaField.getText().trim();
                    if (!metaStr.isEmpty()) {
                        double meta = Double.parseDouble(metaStr);
                        if (meta > 0) ex.put("metaCarga", meta);
                        else ex.remove("metaCarga");
                    } else {
                        ex.remove("metaCarga");
                    }

                    int descMin = descMinField.getText().trim().isEmpty() ? 0 : Integer.parseInt(descMinField.getText().trim());
                    int descSec = descSecField.getText().trim().isEmpty() ? 0 : Integer.parseInt(descSecField.getText().trim());
                    int descanso2 = descMin * 60 + descSec;
                    if (descanso2 > 0) ex.put("descanso", descanso2);
                    else ex.remove("descanso");

                    ex.put("warmup", warmupCheck.isSelected());

                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (Exception ex2) {
                    JOptionPane.showMessageDialog(null, "Valores inválidos. Verifique os campos.");
                }
            });

            btnRow.add(cancelBtn);
            btnRow.add(saveBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    private void mostrarHistoricoCarga(int treinoIdx, int exIdx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            JSONObject ex = treino.getJSONArray("exercicios").getJSONObject(exIdx);

            subModalBox.removeAll();
            JLabel title = new JLabel("Histórico de Carga - " + ex.getString("exercise"));
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);
            JScrollPane scroll = new JScrollPane(listPanel);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setMaximumSize(new Dimension(380, 200));
            scroll.setPreferredSize(new Dimension(380, 200));
            scroll.setBorder(BorderFactory.createLineBorder(new Color(26, 26, 26)));

            JSONArray history = ex.has("loadHistory") ? ex.getJSONArray("loadHistory") : new JSONArray();
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.getJSONObject(i);
                JPanel entry = new JPanel(new BorderLayout());
                entry.setOpaque(false);
                entry.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(26, 26, 26)),
                    BorderFactory.createEmptyBorder(3, 6, 3, 6)
                ));
                String info = item.getDouble("load") + "kg x " + item.getInt("reps") + " reps";
                if (item.has("date")) info += " (" + item.getString("date") + ")";
                JLabel infoLabel = new JLabel(info);
                infoLabel.setForeground(new Color(187, 187, 187));
                infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                entry.add(infoLabel, BorderLayout.WEST);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
                actions.setOpaque(false);
                int idx = i;
                JButton editBtn = new JButton("✎");
                editBtn.setForeground(new Color(136, 170, 255));
                editBtn.setBackground(null);
                editBtn.setBorder(null);
                editBtn.setFocusPainted(false);
                editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                editBtn.addActionListener(e -> {
                    subModal.setVisible(false);
                    mostrarEditarHistoricoCarga(treinoIdx, exIdx, idx);
                });
                actions.add(editBtn);

                JButton delBtn = new JButton("✕");
                delBtn.setForeground(new Color(255, 102, 102));
                delBtn.setBackground(null);
                delBtn.setBorder(null);
                delBtn.setFocusPainted(false);
                delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                delBtn.addActionListener(e -> {
                    mostrarConfirmacao("Excluir Registro", "Tem certeza que deseja excluir este registro?", () -> {
                        try {
                            JSONArray hist = ex.getJSONArray("loadHistory");
                            hist.remove(idx);
                            salvarDados();
                            subModal.setVisible(false);
                            renderDados();
                        } catch (JSONException ex2) {}
                    });
                });
                actions.add(delBtn);
                entry.add(actions, BorderLayout.EAST);

                listPanel.add(entry);
            }

            if (history.length() == 0) {
                JLabel empty = new JLabel("Nenhum registro.");
                empty.setForeground(new Color(102, 102, 102));
                empty.setFont(new Font("Arial", Font.PLAIN, 11));
                empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                listPanel.add(empty);
            }

            subModalBox.add(scroll);

            JButton delAllBtn = new JButton("Excluir Todo Histórico");
            delAllBtn.setBackground(new Color(58, 26, 26));
            delAllBtn.setForeground(new Color(255, 102, 102));
            delAllBtn.setBorder(BorderFactory.createLineBorder(new Color(90, 42, 42)));
            delAllBtn.setFocusPainted(false);
            delAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            delAllBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            delAllBtn.addActionListener(e -> {
                mostrarConfirmacao("Excluir Histórico", "Tem certeza que deseja excluir todo o histórico de carga?", () -> {
                    try {
                        ex.put("loadHistory", new JSONArray());
                        salvarDados();
                        subModal.setVisible(false);
                        renderDados();
                    } catch (JSONException ex2) {}
                });
            });
            subModalBox.add(delAllBtn);
            subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnRow.setOpaque(false);
            JButton closeBtn = new JButton("Fechar");
            closeBtn.setBackground(new Color(42, 42, 42));
            closeBtn.setForeground(new Color(204, 204, 204));
            closeBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> {
                subModal.setVisible(false);
                renderDados();
            });
            btnRow.add(closeBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    private void mostrarEditarHistoricoCarga(int treinoIdx, int exIdx, int histIdx) {
        try {
            JSONArray treinos = configData.getJSONObject("academia").getJSONArray("treinos");
            JSONObject treino = treinos.getJSONObject(treinoIdx);
            JSONObject ex = treino.getJSONArray("exercicios").getJSONObject(exIdx);
            JSONObject item = ex.getJSONArray("loadHistory").getJSONObject(histIdx);

            subModalBox.removeAll();
            JLabel title = new JLabel("Editar Registro de Carga");
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JLabel loadLabel = new JLabel("Carga (kg)");
            loadLabel.setForeground(new Color(136, 136, 136));
            loadLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            loadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(loadLabel);

            JTextField loadField = new JTextField(String.valueOf(item.getDouble("load")));
            loadField.setMaximumSize(new Dimension(200, 30));
            loadField.setBackground(new Color(10, 10, 10));
            loadField.setForeground(new Color(221, 221, 221));
            loadField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            loadField.setHorizontalAlignment(JTextField.CENTER);
            loadField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(loadField);

            JLabel repsLabel = new JLabel("Repetições");
            repsLabel.setForeground(new Color(136, 136, 136));
            repsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            repsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(repsLabel);

            JTextField repsField = new JTextField(String.valueOf(item.getInt("reps")));
            repsField.setMaximumSize(new Dimension(200, 30));
            repsField.setBackground(new Color(10, 10, 10));
            repsField.setForeground(new Color(221, 221, 221));
            repsField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            repsField.setHorizontalAlignment(JTextField.CENTER);
            repsField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(repsField);

            JLabel dataLabel = new JLabel("Data");
            dataLabel.setForeground(new Color(136, 136, 136));
            dataLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            dataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(dataLabel);

            JTextField dataField = new JTextField(item.has("date") ? item.getString("date") : "");
            dataField.setMaximumSize(new Dimension(200, 30));
            dataField.setBackground(new Color(10, 10, 10));
            dataField.setForeground(new Color(221, 221, 221));
            dataField.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            dataField.setHorizontalAlignment(JTextField.CENTER);
            dataField.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(dataField);
            subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            btnRow.setOpaque(false);
            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.setBackground(new Color(42, 42, 42));
            cancelBtn.setForeground(new Color(204, 204, 204));
            cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> {
                subModal.setVisible(false);
                mostrarHistoricoCarga(treinoIdx, exIdx);
            });

            JButton saveBtn = new JButton("Salvar");
            saveBtn.setBackground(new Color(26, 58, 26));
            saveBtn.setForeground(new Color(139, 195, 74));
            saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            saveBtn.setFocusPainted(false);
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> {
                try {
                    double load = Double.parseDouble(loadField.getText().trim());
                    int reps = Integer.parseInt(repsField.getText().trim());
                    String data = dataField.getText().trim();
                    if (load <= 0 || reps < 1 || data.isEmpty()) {
                        throw new NumberFormatException();
                    }
                    JSONObject updated = new JSONObject();
                    updated.put("load", load);
                    updated.put("reps", reps);
                    updated.put("date", data);
                    JSONArray hist = ex.getJSONArray("loadHistory");
                    hist.put(histIdx, updated);
                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (Exception ex2) {
                    JOptionPane.showMessageDialog(null, "Valores inválidos.");
                }
            });

            btnRow.add(cancelBtn);
            btnRow.add(saveBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    private void mostrarAdicionarRoupa(String categoria) {
        String label = categoria.equals("camisas") ? "Camisa" : categoria.equals("calcas") ? "Calça" : "Tênis";

        subModalBox.removeAll();
        JLabel title = new JLabel("Nova " + label);
        title.setForeground(new Color(170, 170, 170));
        title.setFont(new Font("Arial", Font.PLAIN, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(title);

        JTextField field = new JTextField(20);
        field.setMaximumSize(new Dimension(300, 30));
        field.setBackground(new Color(10, 10, 10));
        field.setForeground(new Color(221, 221, 221));
        field.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(field);

        JCheckBox sweatCheck = new JCheckBox("Marca suor");
        sweatCheck.setForeground(new Color(170, 170, 170));
        sweatCheck.setBackground(new Color(13, 13, 13));
        sweatCheck.setFocusPainted(false);
        sweatCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        subModalBox.add(sweatCheck);
        subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.setBackground(new Color(42, 42, 42));
        cancelBtn.setForeground(new Color(204, 204, 204));
        cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> subModal.setVisible(false));

        JButton saveBtn = new JButton("Salvar");
        saveBtn.setBackground(new Color(26, 58, 26));
        saveBtn.setForeground(new Color(139, 195, 74));
        saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> {
            String val = field.getText().trim();
            if (val.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nome é obrigatório.");
                return;
            }
            if (sweatCheck.isSelected()) val += " (suor)";
            try {
                JSONObject roupas = configData.getJSONObject("academia").getJSONObject("roupas");
                roupas.getJSONArray(categoria).put(val);
                salvarDados();
                subModal.setVisible(false);
                renderDados();
            } catch (JSONException ex) {}
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        subModalBox.add(btnRow);

        subModal.pack();
        subModal.setLocationRelativeTo(this);
        subModal.setVisible(true);
    }

    private void mostrarEditarRoupa(String categoria, int idx) {
        try {
            JSONObject roupas = configData.getJSONObject("academia").getJSONObject("roupas");
            String item = roupas.getJSONArray(categoria).getString(idx);
            boolean isSweat = item.contains("(suor)");
            String cleanName = item.replace(" (suor)", "");

            subModalBox.removeAll();
            JLabel title = new JLabel("Editar Roupa");
            title.setForeground(new Color(170, 170, 170));
            title.setFont(new Font("Arial", Font.PLAIN, 16));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(title);

            JTextField field = new JTextField(cleanName);
            field.setMaximumSize(new Dimension(300, 30));
            field.setBackground(new Color(10, 10, 10));
            field.setForeground(new Color(221, 221, 221));
            field.setBorder(BorderFactory.createLineBorder(new Color(42, 42, 42)));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(field);

            JCheckBox sweatCheck = new JCheckBox("Marca suor");
            sweatCheck.setSelected(isSweat);
            sweatCheck.setForeground(new Color(170, 170, 170));
            sweatCheck.setBackground(new Color(13, 13, 13));
            sweatCheck.setFocusPainted(false);
            sweatCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
            subModalBox.add(sweatCheck);
            subModalBox.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            btnRow.setOpaque(false);
            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.setBackground(new Color(42, 42, 42));
            cancelBtn.setForeground(new Color(204, 204, 204));
            cancelBtn.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> subModal.setVisible(false));

            JButton saveBtn = new JButton("Salvar");
            saveBtn.setBackground(new Color(26, 58, 26));
            saveBtn.setForeground(new Color(139, 195, 74));
            saveBtn.setBorder(BorderFactory.createLineBorder(new Color(42, 90, 42)));
            saveBtn.setFocusPainted(false);
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> {
                String val = field.getText().trim();
                if (val.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Nome é obrigatório.");
                    return;
                }
                if (sweatCheck.isSelected()) val += " (suor)";
                try {
                    JSONObject r = configData.getJSONObject("academia").getJSONObject("roupas");
                    r.getJSONArray(categoria).put(idx, val);
                    salvarDados();
                    subModal.setVisible(false);
                    renderDados();
                } catch (JSONException ex) {}
            });

            btnRow.add(cancelBtn);
            btnRow.add(saveBtn);
            subModalBox.add(btnRow);

            subModal.pack();
            subModal.setLocationRelativeTo(this);
            subModal.setVisible(true);
        } catch (JSONException e) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainActivity();
        });
    }
}
EOF