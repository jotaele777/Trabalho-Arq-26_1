package Simulador;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import modelo.memoriaCache.LinhaCache;

public class Screen extends JFrame {

    private CPU cpu;
    private boolean loaded = false;

    // Elementos da Interface
    private JLabel lblMacro, lblMicro;
    private JTextArea txtProgram;
    
    // Labels dos Registradores
    private JLabel lblPC, lblAC, lblSP, lblIR, lblMPC, lblMAR, lblMBR, lblMIR;
    private JLabel lblCacheStatus; 
    private JLabel lblStats; 

    // Painel visual da Cache
    private JPanel cachePanel;
    private JLabel[] cacheLinesLabels;

    public Screen() {
        super("Simulador MIC-1 com Cache Integrada");
        cpu = new CPU();
        
        initUI();
        updateCounters();
    }

    private void initUI() {
        setSize(1100, 750); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL SUPERIOR (Instruções) ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        lblMacro = new JLabel("Macro: Aguardando...", SwingConstants.CENTER);
        lblMacro.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblMacro.setForeground(new Color(0, 0, 139)); 
        
        lblMicro = new JLabel("Micro: ---", SwingConstants.CENTER);
        lblMicro.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lblMicro.setForeground(new Color(139, 0, 0)); 
        
        topPanel.add(lblMacro);
        topPanel.add(lblMicro);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(topPanel, BorderLayout.NORTH);

        // --- PAINEL CENTRAL (Dividido em CPU e Cache) ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0)); 

        // COLUNA 1: Estado da CPU
        JPanel cpuPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        cpuPanel.setBorder(new TitledBorder("Estado da CPU"));
        
        lblPC = createRegLabel("PC");
        lblAC = createRegLabel("AC");
        lblSP = createRegLabel("SP");
        lblIR = createRegLabel("IR");
        lblMPC = createRegLabel("MPC");
        lblMAR = createRegLabel("MAR");
        lblMBR = createRegLabel("MBR");
        lblMIR = createRegLabel("MIR");

        cpuPanel.add(lblPC); cpuPanel.add(lblMPC);
        cpuPanel.add(lblAC); cpuPanel.add(lblMAR);
        cpuPanel.add(lblSP); cpuPanel.add(lblMBR);
        cpuPanel.add(lblIR); cpuPanel.add(lblMIR);
        
        // COLUNA 2: Visualização da Cache
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(new TitledBorder("Cache de Dados (L1)"));
        
        lblCacheStatus = new JLabel("STATUS: ---", SwingConstants.CENTER);
        lblCacheStatus.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblCacheStatus.setOpaque(true);
        lblCacheStatus.setBackground(Color.LIGHT_GRAY);
        memoryPanel.add(lblCacheStatus, BorderLayout.NORTH);

        // Grid com as linhas da cache
        int cacheSize = cpu.cache.getSize(); 
        cachePanel = new JPanel(new GridLayout(cacheSize, 1, 2, 2));
        cacheLinesLabels = new JLabel[cacheSize];
        
        for(int i=0; i<cacheSize; i++) {
            cacheLinesLabels[i] = new JLabel(" L" + i + ": [Inválido]", SwingConstants.LEFT);
            cacheLinesLabels[i].setBorder(BorderFactory.createLineBorder(Color.GRAY));
            cacheLinesLabels[i].setOpaque(true);
            cacheLinesLabels[i].setBackground(Color.WHITE);
            cachePanel.add(cacheLinesLabels[i]);
        }
        
        JScrollPane scrollCache = new JScrollPane(cachePanel);
        memoryPanel.add(scrollCache, BorderLayout.CENTER);
        
        // Estatísticas
        JPanel statsPanel = new JPanel(new FlowLayout());
        lblStats = new JLabel("Hits: 0 | Misses: 0 | Hit Rate: 0%");
        lblStats.setFont(new Font("SansSerif", Font.BOLD, 14));
        statsPanel.add(lblStats);
        statsPanel.setBackground(new Color(230, 230, 250)); 
        memoryPanel.add(statsPanel, BorderLayout.SOUTH);

        centerPanel.add(cpuPanel);
        centerPanel.add(memoryPanel);
        add(centerPanel, BorderLayout.CENTER);

        // --- PAINEL INFERIOR (Controles e Código) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        txtProgram = new JTextArea(8, 40);
        txtProgram.setText("// Digite seu código Assembly aqui...\nLOCO 10\nSTOD 100");
        txtProgram.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bottomPanel.add(new JScrollPane(txtProgram), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnLoad = new JButton("Carregar Código");
        JButton btnNextMacro = new JButton("Próx. Macro");
        JButton btnNextMicro = new JButton("Próx. Micro");
        JButton btnRun = new JButton("Executar (Run)");
        JButton btnClear = new JButton("Limpar");

        btnLoad.addActionListener(e -> loadProgram());
        btnNextMacro.addActionListener(e -> { if(loaded) { cpu.nextMacro(); updateUI(); }});
        btnNextMicro.addActionListener(e -> { if(loaded) { cpu.nextMicro(); updateUI(); }});
        btnClear.addActionListener(e -> clear());
        btnRun.addActionListener(e -> runSimulation());

        btnPanel.add(btnLoad);
        btnPanel.add(btnNextMicro);
        btnPanel.add(btnNextMacro);
        btnPanel.add(btnRun);
        btnPanel.add(btnClear);
        
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JLabel createRegLabel(String name) {
        JLabel l = new JLabel(name + ": 0");
        l.setFont(new Font("Monospaced", Font.BOLD, 14));
        l.setBorder(BorderFactory.createEtchedBorder());
        return l;
    }

    private void updateUI() {
        updateCounters();
        updateCacheVisuals();
    }

    private void updateCounters() {
        lblPC.setText(String.format("PC: %d", cpu.getRegisters()[0].get()));
        lblAC.setText(String.format("AC: %d", cpu.getRegisters()[1].get()));
        lblSP.setText(String.format("SP: %d", cpu.getRegisters()[2].get()));
        lblIR.setText(String.format("IR: %d", cpu.getRegisters()[3].get()));
        
        lblMPC.setText(String.format("MPC: %d", cpu.getMPC().get()));
        lblMAR.setText(String.format("MAR: %d", cpu.getMAR().get()));
        lblMBR.setText(String.format("MBR: %d", cpu.getMBR().get()));
        lblMIR.setText(String.format("MIR: %d", cpu.getMIR().get()));

        int pc = cpu.getRegisters()[0].get();
        int mpc = cpu.getMPC().get();
        
        int currentInstAddr = Math.max(0, (mpc <= 1) ? pc : pc - 1);
        
        String macroText = "FETCHING...";
        if (cpu.macroInstructionTable != null && cpu.macroInstructionTable.containsKey((short) currentInstAddr)) {
            macroText = cpu.macroInstructionTable.get((short) currentInstAddr);
        }
        lblMacro.setText("Macro: " + macroText);
        lblMicro.setText("Micro: " + cpu.getCurrentMicroInst());
        
        int hits = cpu.cache.getHits();
        int misses = cpu.cache.getMisses();
        int total = hits + misses;
        float rate = (total > 0) ? ((float)hits / total) * 100 : 0;
        
        lblStats.setText(String.format("Hits: %d  |  Misses: %d  |  Hit Rate: %.1f%%", hits, misses, rate));
    }

    private void updateCacheVisuals() {
        LinhaCache[] lines = cpu.cache.getLines();
        
        for (int i = 0; i < lines.length; i++) {
            LinhaCache line = lines[i];
            String status = line.validade ? (line.dirty ? "[Sujo]" : "[Limpo]") : "[Inválido]";
            String dadosFormatados = Arrays.toString(line.dados);
            String text = String.format(" L%02d | Tag: %d | Dados: %s | %s", i, line.tag, dadosFormatados, status);
            cacheLinesLabels[i].setText(text);
            
            // Cor base branca/gelo
            cacheLinesLabels[i].setBackground(line.validade ? new Color(245, 255, 245) : Color.WHITE);
        }

        // Recupera o status do ÚLTIMO acesso real à memória para não piscar mais em Cinza
        int lastAddr = cpu.getMAR().get() & 0xFFFF;
        int index = (lastAddr / cpu.cache.getTamanhoBloco()) % cpu.cache.getSize(); 
        
        if (cpu.cache.isLastAccessHit()) { 
            lblCacheStatus.setText("ÚLTIMO ACESSO: HIT! (Endereço " + lastAddr + ")");
            lblCacheStatus.setBackground(new Color(170, 255, 170)); // Verde Claro
            if(index < lines.length && lines[index].validade) cacheLinesLabels[index].setBackground(new Color(170, 255, 170)); 
        } else {
            lblCacheStatus.setText("ÚLTIMO ACESSO: MISS! (Endereço " + lastAddr + ")");
            lblCacheStatus.setBackground(new Color(255, 180, 180)); // Vermelho Claro
            if(index < lines.length && lines[index].validade) cacheLinesLabels[index].setBackground(new Color(255, 180, 180)); 
        }
    }

    private void loadProgram() {
        String code = txtProgram.getText();
        if (code.trim().isEmpty()) return;

        try {
            File temp = File.createTempFile("prog", ".asm");
            FileWriter fw = new FileWriter(temp);
            fw.write(code);
            fw.close();
            
            cpu.loadProgram(temp.getAbsolutePath());
            loaded = true;
            lblMacro.setText("Programa Carregado!");
            updateUI();
            temp.deleteOnExit();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + e.getMessage());
        }
    }

    private void clear() {
        cpu.clearProgram();
        loaded = false;
        updateUI();
        lblCacheStatus.setText("Memória Limpa");
        lblCacheStatus.setBackground(Color.LIGHT_GRAY);
        lblStats.setText("Hits: 0 | Misses: 0 | Hit Rate: 0%");
    }

    private void runSimulation() {
        if (!loaded) {
            JOptionPane.showMessageDialog(this, "Carregue um programa primeiro!");
            return;
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Consumer<CPU> observer = (c) -> {
                    try {
                        SwingUtilities.invokeLater(() -> updateUI()); 
                        Thread.sleep(100); 
                    } catch (InterruptedException e) {}
                };
                cpu.runWithObserver(observer);
                return null;
            }
        };
        worker.execute();
    }
}