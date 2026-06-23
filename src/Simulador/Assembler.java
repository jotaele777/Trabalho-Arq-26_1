package Simulador;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler {
    
    public static Map<Short, String> assembleAndLoad(List<String> lines, MainMemory MP) {
        Map<Short, String> instructionTable = new HashMap<>();
        Map<String, Integer> symbolTable = new HashMap<>();

        // -----------------------------------------------------
        // PASSAGEM 1: Mapeamento de Rótulos (Labels)
        // -----------------------------------------------------
        int pc = 0;
        for (String line : lines) {
            if (line.contains(":")) {
                String[] split = line.split(":", 2);
                String label = split[0].trim();
                symbolTable.put(label, pc); // Salva o endereço de memória onde a label está
                line = split[1].trim(); // Pega a instrução depois dos dois-pontos
            }
            if (!line.isEmpty()) {
                pc++; // Conta o endereço (PC) apenas se houver uma instrução na linha
            }
        }

        // -----------------------------------------------------
        // PASSAGEM 2: Tradução e Alocação Dinâmica de Variáveis
        // -----------------------------------------------------
        int currentAddr = 0;
        int varAlloc = 2048; // Início do espaço reservado dinamicamente para variáveis

        for (String originalLine : lines) {
            String line = originalLine;

            // Remove a label da string para sobrar só o mnemônico
            if (line.contains(":")) {
                line = line.split(":", 2)[1].trim();
            }

            if (line.isEmpty()) continue;

            String[] parts = line.replace(",", " ").trim().split("\\s+");
            String mnemonic = parts[0].toUpperCase();

            if (!Dicio.inst.containsKey(mnemonic)) { 
                throw new IllegalArgumentException("Mnemônico inválido: '" + mnemonic + "'");
            }

            String binaryPrefix = Dicio.inst.get(mnemonic); 
            StringBuilder binaryBuilder = new StringBuilder(binaryPrefix);

            try {
                // TIPO 1: Instruções com Endereço de Memória / Variáveis (12 bits)
                if (isAddressInstruction(mnemonic)) {
                    if (parts.length < 2) throw new IllegalArgumentException(mnemonic + " requer operando.");
                    String operand = parts[1];
                    
                    int address = resolveOperand(operand, symbolTable, varAlloc);
                    if (address == varAlloc) varAlloc++; // Se foi uma nova variável, avança o alocador

                    if (address < 0 || address > 4095) throw new IllegalArgumentException("Endereço fora do limite: " + address);
                    
                    // Aplica máscara (0xFFF) para lidar com constantes negativas no LOCO (-5, -1) em 12 bits
                    String binVal = String.format("%12s", Integer.toBinaryString(address & 0xFFF)).replace(' ', '0');
                    binaryBuilder.append(binVal);
                }
                // TIPO 2: Instruções com operandos curtos / Pilha (ex: INSP, DESP - 8 bits)
                else if (isShortAddressInstruction(mnemonic)) {
                    if (parts.length < 2) throw new IllegalArgumentException(mnemonic + " requer operando.");
                    
                    int value = Integer.parseInt(parts[1]);
                    if (value < 0 || value > 255) throw new IllegalArgumentException("Valor fora do limite (0-255): " + value);
                    
                    String binVal = String.format("%8s", Integer.toBinaryString(value & 0xFF)).replace(' ', '0');
                    binaryBuilder.append(binVal);
                }
                // TIPO 3: Instruções sem operando (HALT, POP, etc) não precisam de append

                String binaryFinal = binaryBuilder.toString();
                
                // Preenche com '0' à esquerda para garantir 16 bits exatos
                while (binaryFinal.length() < 16) {
                    binaryFinal = "0" + binaryFinal;
                }

                short machineCode = (short) Integer.parseInt(binaryFinal, 2);
                MP.setManual(currentAddr, machineCode);
                instructionTable.put((short) currentAddr, originalLine.trim());

                currentAddr++;

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Operando numérico mal formatado na linha: " + originalLine);
            }
        }

        return instructionTable;
    }

    /**
     * Resolve se o operando é um número (ex: 100), uma Label (ex: INICIO),
     * ou aloca como uma nova Variável Dinâmica (ex: var_x).
     */
    private static int resolveOperand(String operand, Map<String, Integer> symbolTable, int varAlloc) {
        // Se for puramente números (com ou sem sinal negativo)
        if (operand.matches("-?\\d+")) {
            return Integer.parseInt(operand);
        } 
        // Se for um Rótulo (Label) ou Variável já cadastrada
        else if (symbolTable.containsKey(operand)) {
            return symbolTable.get(operand);
        } 
        // Se for um nome de variável novo, salva e devolve o endereço dinâmico
        else {
            symbolTable.put(operand, varAlloc);
            return varAlloc;
        }
    }
    
    private static boolean isAddressInstruction(String m) {
        return m.equals("LODD") || m.equals("STOD") || m.equals("ADDD") || m.equals("SUBD") || 
               m.equals("JPOS") || m.equals("JZER") || m.equals("JUMP") || m.equals("LOCO") || 
               m.equals("LODL") || m.equals("STOL") || m.equals("ADDL") || m.equals("SUBL") || 
               m.equals("JNEG") || m.equals("JNZE") || m.equals("CALL");
    }

    private static boolean isShortAddressInstruction(String m) {
        return m.equals("INSP") || m.equals("DESP");
    }
}