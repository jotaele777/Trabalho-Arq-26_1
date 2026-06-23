package Simulador;

import modelo.memoriaCache.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Consumer;

public class CPU {
    // Registradores Principais
    private Register MAR = new Register("MAR", (short)0);
    private Register MBR = new Register("MBR", (short)0);
    private Register MPC = new Register("MPC", (short)0);
    private Register32bit MIR = new Register32bit("MIR", 0);
    
    // Sistema de Memória e Cache
    public MainMemory MP;
    public Cache cache; 

    // Barramentos Internos e Latches
    private short busLine_C; 
    private short latch_A, latch_B;
    private byte decodedA, decodedB; 

    // Componentes
    Clock clock;
    private Register[] registers;
    private int[] controlMemory;
    private ALU alu;
    private Amux amux;
    private Shifter shifter;

    // Controle de Execução
    private String currentMacroInst;
    private String currentMicroInst;
    public String[] microprogramCode;
    public Map<Short, String> macroInstructionTable;
    private PrintWriter logWriter;

    public CPU() {
        clock = new Clock();
        initRegisters();
        
        MP = new MainMemory();
        // Inicializa com Mapeamento Direto por padrão
        //cache = new CacheMapeamentoDireto(MP, 16, 4);
        cache = new CacheAssociativo(MP, 16, 4);
        
        controlMemory = FileParser.getControlMemory();
        if(controlMemory == null || controlMemory.length == 0) {
            System.err.println("AVISO: Memória de Controle pode estar vazia ou não foi carregada corretamente.");
        }
        
        microprogramCode = FileParser.getMicroProgramCode("/dataFiles/microprogram.txt");

        alu = new ALU();
        amux = new Amux();
        shifter = new Shifter();
    }
    
    private void initRegisters() {
        registers = new Register[16];
        registers[0] = new Register("PC", (short) 0);
        registers[1] = new Register("AC", (short) 0);
        registers[2] = new Register("SP", (short) 4096);
        registers[3] = new Register("IR", (short) 0);
        registers[4] = new Register("TIR", (short) 0);
        registers[5] = new Register("0", (short) 0);
        registers[6] = new Register("+1", (short) 1);
        registers[7] = new Register("-1", (short) -1);
        registers[8] = new Register("AMASK", (short) 0x0FFF);
        registers[9] = new Register("SMASK", (short) 0x00FF);
        registers[10] = new Register("A", (short) 0);
        registers[11] = new Register("B", (short) 0);
        registers[12] = new Register("C", (short) 0);
        registers[13] = new Register("D", (short) 0);
        registers[14] = new Register("E", (short) 0);
        registers[15] = new Register("F", (short) 0);
    }

    public void loadProgram(String filepath) {
        clearProgram();
        macroInstructionTable = FileParser.loadMP(MP, filepath);
    }

    // --- CICLO DE EXECUÇÃO ---

    public void runFirstSubcycle() {
        // Fetch da Microinstrução
        if(MPC.get() < controlMemory.length) {
            MIR.set(controlMemory[MPC.get()]);
        }
        clock.increment();
    }

    public void runSecondSubcycle() {
        decodedA = get_BUSA_Field();
        decodedB = get_BUSB_Field();
        latch_A = registers[decodedA].get();
        latch_B = registers[decodedB].get();
        clock.increment();
    }

    public void runThirdSubcycle() {
        byte signal_AMUX = get_AMUX_Field();
        byte signal_ALU = get_ALU_Field();
        byte signal_SHIFTER = get_SHIFTER_Field();
        byte signal_MAR = get_MAR_Field();

        amux.decideOutput(signal_AMUX, latch_A, MBR.get());
        
        if (signal_MAR == 1) {
            MAR.set(latch_B);
        }
        
        alu.execute(signal_ALU, amux.getOutput(), latch_B);
        shifter.execute(signal_SHIFTER, alu.getOutput());
        clock.increment();
    }

    public void runFourthSubcycle() {
        byte signal_ENC = get_ENC_Field();
        byte signal_RD = get_RD_Field();
        byte signal_WR = get_WR_Field();
        byte signal_MBR = get_MBR_Field();
        byte decodedC = get_BUSC_Field();

        busLine_C = shifter.getOutput();
        
        if (signal_MBR == 1) {
            MBR.set(busLine_C);
        }
        
        if (signal_ENC == 1 && decodedC != 5 && decodedC != 6 && decodedC != 7) { 
            registers[decodedC].set(busLine_C);
        }

        // --- INTEGRAÇÃO COM A CACHE ---
        if (signal_RD == 1) {
            short cleanAddress = (short) (MAR.get() & 0x0FFF);
            short data = cache.read(cleanAddress);
            MBR.set(data);
        }
        
        if (signal_WR == 1) {
            short cleanAddress = (short) (MAR.get() & 0x0FFF);
            cache.write(cleanAddress, MBR.get());
        }

        calculateNextMPC();
        clock.increment();
    }

    public void calculateNextMPC() {
        byte signal_COND = get_COND_Field();
        byte addr = get_ADDR_Field();
        boolean bitZ = alu.getZbit();
        boolean bitN = alu.getNbit();
        
        // Incrementa normalmente
        short nextMPC = (short) (MPC.get() + 1); 
        
        switch (signal_COND) {
            case 0b01: // Jump if Negative
                if(bitN) {
                    nextMPC = addr; 
                }
                break;

            case 0b10: // Jump if Zero
                if(bitZ) {
                    nextMPC = addr; 
                }
                break;

            case 0b11: // Unconditional Jump
                nextMPC = addr; 
                break;
                
            default: // 00: Não faz nada (segue o fluxo)
                break;
        }
        
        MPC.set(nextMPC);
    }
    
    // Métodos Auxiliares
    private int getMicroInstructionField(int shift, int mask) {
        return (MIR.get() >> shift) & mask;
    }
    
    public byte get_ADDR_Field() { return (byte)getMicroInstructionField(0, 0xFF); }
    public byte get_BUSA_Field() { return (byte)getMicroInstructionField(8, 0xF); }
    public byte get_BUSB_Field() { return (byte)getMicroInstructionField(12, 0xF); }
    public byte get_BUSC_Field() { return (byte)getMicroInstructionField(16, 0xF); }
    public byte get_ENC_Field() { return (byte)getMicroInstructionField(20, 0x1); }
    public byte get_WR_Field() { return (byte)getMicroInstructionField(21, 0x1); }
    public byte get_RD_Field() { return (byte)getMicroInstructionField(22, 0x1); }
    public byte get_MAR_Field() { return (byte)getMicroInstructionField(23, 0x1); }
    public byte get_MBR_Field() { return (byte)getMicroInstructionField(24, 0x1); }
    public byte get_SHIFTER_Field() { return (byte)getMicroInstructionField(25, 0x3); }
    public byte get_ALU_Field() { return (byte)getMicroInstructionField(27, 0x3); }
    public byte get_COND_Field() { return (byte)getMicroInstructionField(29, 0x3); }
    public byte get_AMUX_Field() { return (byte)getMicroInstructionField(31, 0x1); }

    // Logs e Controle da UI
    public void runWithObserver(Consumer<CPU> observer) {
        startLog("EXECUTION_LOG.txt");
        boolean running = true;
        
        while (running) {
            nextMicro(); // Executa um ciclo completo
            clock.incrementCounter();
            
            if(observer != null) observer.accept(this);

            if (registers[3].get() == -1 && MPC.get() == 0) { 
                 running = false;
            }
        }
        endLog();
    }

    private void startLog(String filename) {
        try {
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            logWriter.println("#----- CPU Execution Log (With Cache) -----#\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void endLog() {
        if(logWriter != null) logWriter.close();
    }

    private void saveCPUState() {
        if(logWriter == null) return;
        currentMacroInst = getMacroinstCode();
        currentMicroInst = getCurrentMicroInst();
        
        logWriter.printf("CLOCK %d | MPC: %d | MIR: %d | Cache Hits: %d | Cache Misses: %d\n", 
            clock.getClockCounter(), MPC.get(), MIR.get(), cache.getHits(), cache.getMisses());
    }
    
    public void clearProgram() {
        MP.clearMemory();
        // Recria a cache para zerar os blocos e as estatísticas de Hit/Miss
        cache = new CacheAssociativo(MP, 16, 4);
        //cache = new CacheMapeamentoDireto(MP, 16, 4);
        
        for(Register r : registers) r.set((short)0);
        registers[2].set((short)4096);
        registers[6].set((short)1);
        registers[7].set((short)-1);
        registers[8].set((short)0x0FFF);
        registers[9].set((short)0x00FF);
        
        MPC.set((short)0);
        clock.reset();
    }
    
    private String getMacroinstCode() {
        return (macroInstructionTable != null) ? macroInstructionTable.getOrDefault(registers[3].get(), "FETCHING...") : "UNKNOWN";
    }

    public void nextMicro() {
        if (registers[3].get() != -1) { 
            runFirstSubcycle();
            runSecondSubcycle();
            runThirdSubcycle();
            runFourthSubcycle();
            saveCPUState();
        }
    }

    public void nextMacro() {
        int safetyCount = 0; 
        
        // Executa pelo menos uma vez e continua enquanto MPC não voltar para 0
        do {
            nextMicro(); 
            safetyCount++;
        } while (MPC.get() != 0 && safetyCount < 2000);
        
        saveCPUState();
    }

    public String getCurrentMacroInst() {
        return getMacroinstCode();
    }

    public String getCurrentMicroInst() {
        if (microprogramCode != null && MPC.get() >= 0 && MPC.get() < microprogramCode.length) {
            return microprogramCode[MPC.get()];
        }
        return "NOP (MPC " + MPC.get() + ")";
    }

    public Register[] getRegisters() { return registers; }
    public Register getMAR() { return MAR; }
    public Register getMBR() { return MBR; }
    public Register getMPC() { return MPC; }
    public Register32bit getMIR() { return MIR; }
    public ALU getALU() { return alu; }
    public Amux getAmux() { return amux; }
    public Shifter getShifter() { return shifter; }
    public Clock getClock() { return clock; }
}