package Simulador;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MainMemory {
    public final static int MAIN_MEMORY_SIZE = 4096; // Mudado para int para evitar problemas de sinal
    private short[] memory = new short[MAIN_MEMORY_SIZE];

    public MainMemory() {
        clearMemory();
    }

    // Métodos diretos usados pela CacheController
    public void setManual(int addr, short value) {
        if (addr >= 0 && addr < MAIN_MEMORY_SIZE) {
            memory[addr] = value;
        } else {
            System.err.println("Erro: Escrita fora dos limites da memória: " + addr);
        }
    }

    public short getManual(int addr) {
        if (addr >= 0 && addr < MAIN_MEMORY_SIZE) {
            return memory[addr];
        }
        System.err.println("Erro: Leitura fora dos limites da memória: " + addr);
        return 0;
    }

    // Usado pelo FileParser para carregar o programa
    public void add(int addr, short value) {
        setManual(addr, value);
    }

    public void createMemoryLog(String filename) {
        try (PrintWriter memoryWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
            System.out.println("Log da MP iniciado.");
            memoryWriter.println("#----- MAIN MEMORY DUMP -----#");
            memoryWriter.println();

            for (int i = 0; i < MAIN_MEMORY_SIZE; i++) {
                if (memory[i] != 0) { // Otimização: Só loga o que não é zero para economizar disco
                    memoryWriter.printf("MP[%d]: %d\n", i, memory[i]);
                }
            }
            System.out.println("Log da memória criado com sucesso.");
        } catch (IOException e) {
            System.err.println("### ERRO GRAVE: Falha ao criar o arquivo de log da memória! ###");
            e.printStackTrace();
        }
    }

    public void clearMemory() {
        for (int i = 0; i < MAIN_MEMORY_SIZE; i++) { 
            memory[i] = 0;                               
        }
    }
}