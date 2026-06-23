package modelo.memoriaCache;

import Simulador.MainMemory;

public class CacheAssociativo extends Cache {
    private LinhaCache[] linhas;
    private int numLinhas;

    public CacheAssociativo(MainMemory mp, int tamanhoCache, int tamanhoBloco) {
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
        int tag = indiceBloco; 
        int offset = endereco % tamanhoBloco;

        for (int i = 0; i < numLinhas; i++) {
            if (linhas[i].validade && linhas[i].tag == tag) {
                hits++;
                lastAccessHit = true; // Informa a UI que foi HIT
                linhas[i].ultimoAcesso = System.nanoTime(); 
                return linhas[i].dados[offset];
            }
        }

        misses++;
        lastAccessHit = false; // Informa a UI que foi MISS
        int linhaSubstituicao = 0;
        long tempoMaisAntigo = Long.MAX_VALUE;

        for (int i = 0; i < numLinhas; i++) {
            if (!linhas[i].validade) {
                linhaSubstituicao = i;
                break; 
            }
            if (linhas[i].ultimoAcesso < tempoMaisAntigo) {
                tempoMaisAntigo = linhas[i].ultimoAcesso;
                linhaSubstituicao = i;
            }
        }

        int enderecoBase = indiceBloco * tamanhoBloco;
        for (int i = 0; i < tamanhoBloco; i++) {
            linhas[linhaSubstituicao].dados[i] = memoriaPrincipal.getManual(enderecoBase + i);
        }
        linhas[linhaSubstituicao].tag = tag;
        linhas[linhaSubstituicao].validade = true;
        linhas[linhaSubstituicao].ultimoAcesso = System.nanoTime();

        return linhas[linhaSubstituicao].dados[offset];
    }

    @Override
    public void write(int endereco, short valor) {
        int indiceBloco = endereco / tamanhoBloco;
        int tag = indiceBloco;
        int offset = endereco % tamanhoBloco;

        memoriaPrincipal.setManual(endereco, valor);

        for (int i = 0; i < numLinhas; i++) {
            if (linhas[i].validade && linhas[i].tag == tag) {
                linhas[i].dados[offset] = valor;
                linhas[i].ultimoAcesso = System.nanoTime();
                hits++;
                lastAccessHit = true;
                return;
            }
        }
        misses++;
        lastAccessHit = false;
    }

    // Método exigido pela interface gráfica
    @Override
    public LinhaCache[] getLines() {
        return this.linhas;
    }
}