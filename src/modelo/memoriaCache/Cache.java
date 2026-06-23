package modelo.memoriaCache;

import Simulador.MainMemory;

public abstract class Cache {
    protected MainMemory memoriaPrincipal;
    protected int hits;
    protected int misses;
    protected int tamanhoCache; 
    protected int tamanhoBloco; 
    protected boolean lastAccessHit = false; // Flag para a tela pintar de verde/vermelho

    public Cache(MainMemory memoriaPrincipal, int tamanhoCache, int tamanhoBloco) {
        this.memoriaPrincipal = memoriaPrincipal;
        this.tamanhoCache = tamanhoCache;
        this.tamanhoBloco = tamanhoBloco;
        this.hits = 0;
        this.misses = 0;
    }

    public abstract short read(int endereco);
    public abstract void write(int endereco, short valor);
    public abstract LinhaCache[] getLines(); // Obrigatório para a interface desenhar a tabela

    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    
    // Getters para a Interface Gráfica
    public int getSize() { return tamanhoCache / tamanhoBloco; }
    public int getTamanhoBloco() { return tamanhoBloco; }
    public boolean isLastAccessHit() { return lastAccessHit; }
    
    public void resetEstatisticas() {
        this.hits = 0;
        this.misses = 0;
        this.lastAccessHit = false;
    }
}