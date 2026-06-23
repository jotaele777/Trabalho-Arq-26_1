package Simulador;

public class Shifter {
    private short output;

    public Shifter() {
        this.output = 0;
    }

    public void execute(byte controlSignal, short input) {
        // 00: Pass Through (Não faz nada)
        // 01: Shift Right (Desloca direita - Divisão por 2)
        // 10: Shift Left (Desloca esquerda - Multiplicação por 2)
        
        switch (controlSignal) {
            case 0b00:
                this.output = input;
                break;
            case 0b01:
                this.output = (short)(input >> 1);
                break;
            case 0b10:
                this.output = (short)(input << 1);
                break;
            default:
                // Se vier 11 ou outro valor, não faz nada (proteção)
                this.output = input;
                break;
        }
    }
    
    // Corrigi o typo "getOuput" para "getOutput"
    public short getOutput() {
        return this.output;
    }
}