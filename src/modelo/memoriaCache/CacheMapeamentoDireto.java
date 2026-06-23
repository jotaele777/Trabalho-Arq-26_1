package modelo.memoriaCache;

import Simulador.MainMemory;

public class CacheMapeamentoDireto extends Cache {
    private LinhaCache[] linhas;
    private int numLinhas;

    public CacheMapeamentoDireto(MainMemory mp, int tamanhoCache, int tamanhoBloco) {
        super(mp, tamanhoCache, tamanhoBloco);
        this.numLinhas = tamanhoCache / tamanhoBloco;
        this.linhas = new LinhaCache[this.numLinhas];

        for (int i = 0; i < this.numLinhas; i++) {
            this.linhas[i] = new LinhaCache(tamanhoBloco);
        }
    }

    @Override
    public short read(int endereco) {
        int indiceBloco = endereco / tamanhoBloco;
        int indiceLinha = indiceBloco % numLinhas;
        int tag = indiceBloco / numLinhas;
        int offset = endereco % tamanhoBloco;

        LinhaCache linha = linhas[indiceLinha];

        // HIT
        if (linha.validade && linha.tag == tag) {
            hits++;
            lastAccessHit = true; 
            return linha.dados[offset];
        }

        // MISS
        misses++;
        lastAccessHit = false; 
        
        int enderecoBase = indiceBloco * tamanhoBloco;
        for (int i = 0; i < tamanhoBloco; i++) {
            linha.dados[i] = memoriaPrincipal.getManual(enderecoBase + i);
        }
        linha.tag = tag;
        linha.validade = true;

        return linha.dados[offset];
    }

    @Override
    public void write(int endereco, short valor) {
        int indiceBloco = endereco / tamanhoBloco;
        int indiceLinha = indiceBloco % numLinhas;
        int tag = indiceBloco / numLinhas;
        int offset = endereco % tamanhoBloco;

        LinhaCache linha = linhas[indiceLinha];

        memoriaPrincipal.setManual(endereco, valor);

        if (linha.validade && linha.tag == tag) {
            linha.dados[offset] = valor;
            hits++;
            lastAccessHit = true; 
        } else {
            misses++;
            lastAccessHit = false; 
        }
    }

    @Override
    public LinhaCache[] getLines() {
        return this.linhas;
    }
}