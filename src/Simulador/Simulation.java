package Simulador;
import javax.swing.SwingUtilities;

public class Simulation {
    public static void main(String[] args) {
        // É boa prática rodar GUI dentro da thread do Swing
        SwingUtilities.invokeLater(() -> {
            new Screen();
        });
    }
}

/*
 * // TESTE 1: ARITMÉTICA BÁSICA
LOCO 10
STOD 50
LOCO 5
ADDD 50
STOD 51
SUBD 50
HALT



// TESTE 2: ESTRESSE DE CACHE (WRITE-BACK & EVICÇÃO)
 
LOCO 111
STOD 0
LOCO 222
STOD 16
LODD 0 
HALT


// TESTE DE CACHE: HITS E EVICÇÃO
LOCO 15
STOD 0
LOCO 20
ADDD 0
STOD 1
STOD 64
LODD 0
HALT


// Teste Definitivo: Labels e Variáveis!
INICIO: LOCO 50
        STOD contador
        LOCO 1
        ADDD contador
        STOD contador
        JUMP INICIO
        HALT
        
        
        
INICIO: LOCO 50
        STOD contador
        LOCO 1
        ADDD contador
        
linhas 48 e 251 
 */

