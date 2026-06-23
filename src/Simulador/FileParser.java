package Simulador;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileParser {

    public static Map<Short, String> loadMP(MainMemory MP, String filepath) {
        List<String> validLines = new ArrayList<>();

        try {
            BufferedReader bf = null;
            try {
                bf = getBufferedReader(filepath);
            } catch (IOException e) {
                bf = new BufferedReader(new FileReader(filepath));
            }
            
            String line;
            while ((line = bf.readLine()) != null) {
                line = line.trim();
                
                // Ignora linhas totalmente vazias
                if (line.isEmpty()) continue;
                
                // Remove comentários inline (ex: "//" ou ";")
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) line = line.substring(0, commentIndex).trim();
                
                commentIndex = line.indexOf(";");
                if (commentIndex != -1) line = line.substring(0, commentIndex).trim();
                
                // Se a linha ficou vazia após limpar os comentários, pula
                if (!line.isEmpty()) {
                    validLines.add(line);
                }
            }
            bf.close();

            // Passa todas as linhas limpas para o nosso Montador de Duas Passagens
            return Assembler.assembleAndLoad(validLines, MP);

        } catch (Exception e) {
            System.err.println("Erro ao carregar Macroprograma: " + e.getMessage());
            // Lança exceção para a Interface Gráfica exibir um pop-up de erro elegante
            throw new RuntimeException(e.getMessage());
        }
    }

    public static int[] getControlMemory() {
        int[] output = new int[256];
        try {
            // Tenta buscar no Classpath (pasta src ou pacote base)
            InputStream inputStream = FileParser.class.getResourceAsStream("/controlMemory.txt");
            if (inputStream == null) {
                // Tenta buscar na raiz do projeto (Working Directory)
                inputStream = new FileInputStream("controlMemory.txt");
            }
            
            BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int index = 0;
            
            while ((line = bf.readLine()) != null && index < output.length) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//")) continue;
                
                // Remove espaços e converte a string binária de 32 bits
                String cleanLine = line.replaceAll("\\s+", "");
                if (cleanLine.length() >= 32) {
                    output[index] = Integer.parseUnsignedInt(cleanLine, 2);
                    index++;
                }
            }
            bf.close();
        } catch (Exception e) {
            System.err.println("ERRO: Falha ao ler memória de controle (controlMemory.txt).");
            return new int[256];
        }
        return output;
    }

    public static String [] getMicroProgramCode(String filename) {
        List<String> programLines = new ArrayList<>();
        String line;

        try {
            BufferedReader bf = null;
            try {
                bf = getBufferedReader(filename);
            } catch (Exception e) {
                bf = new BufferedReader(new FileReader(filename));
            }

            while ((line = bf.readLine()) != null) {
                programLines.add(line);
            }
        } catch (IOException e) {
            System.out.println("Falha ao carregar código do microprograma.");
            return new String[0];
        }
        return programLines.toArray(new String[0]);
    }

    private static BufferedReader getBufferedReader(String resourcePath) throws IOException {
        InputStream inputStream = FileParser.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Recurso não encontrado: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(inputStream));
    }
}