package Simulador;

public class Register {
    private short value;
    private final String name;

    public Register(String name, short initValue) {
        this.name = name;
        this.value = initValue;
    }

    public short get() {
        return this.value;
    }

    public void set(short value) {
        this.value = value;
    }

    public String getName() {
        return this.name;
    }
    
    @Override
    public String toString() {
        return Short.toString(value);
    }
}