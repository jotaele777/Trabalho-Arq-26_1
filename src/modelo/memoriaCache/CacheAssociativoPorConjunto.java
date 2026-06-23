package modelo.memoriaCache;

import Simulador.MainMemory;

public class CacheAssociativoPorConjunto extends Cache {
    private LinhaCache[][] conjuntos;
    private int numConjuntos;
    private int vias; 
    
    // Array auxiliar (1D) apenas para enviar para a Interface Gráfica
    private LinhaCache[] linhasFlattened; 

    public CacheAssociativoPorConjunto(MainMemory mp, int tamanhoCache, int tamanhoBloco, int vias) {
        super(mp, tamanhoCache, tamanhoBloco);
        this.vias = vias;
        int numLinhasTotal = tamanhoCache / tamanhoBloco;
        this.numConjuntos = numLinhasTotal / vias;
        
        this.conjuntos = new LinhaCache[this.numConjuntos][this.vias];
        this.linhasFlattened = new LinhaCache[numLinhasTotal];

        int count = 0;
        for (int i = 0; i < this.numConjuntos; i++) {
            for (int j = 0; j < this.vias; j++) {
                LinhaCache novaLinha = new LinhaCache(tamanhoBloco);
                this.conjuntos[i][j] = novaLinha;
                // Guarda a mesma referência na lista simples para a UI
                this.linhasFlattened[count++] = novaLinha; 
            }
        }
    }

    @Override
    public short read(int endereco) {
        int indiceBloco = endereco / tamanhoBloco;
        int indiceConjunto = indiceBloco % numConjuntos;
        int tag = indiceBloco / numConjuntos;
        int offset = endereco % tamanhoBloco;

        LinhaCache[] conjunto = conjuntos[indiceConjunto];

        for (int i = 0; i < vias; i++) {
            if (conjunto[i].validade && conjunto[i].tag == tag) {
                hits++;
                lastAccessHit = true;
                conjunto[i].ultimoAcesso = System.nanoTime();
                return conjunto[i].dados[offset];
            }
        }

        misses++;
        lastAccessHit = false;
        int viaSubstituicao = 0;
        long tempoMaisAntigo = Long.MAX_VALUE;

        for (int i = 0; i < vias; i++) {
            if (!conjunto[i].validade) {
                viaSubstituicao = i;
                break;
            }
            if (conjunto[i].ultimoAcesso < tempoMaisAntigo) {
                tempoMaisAntigo = conjunto[i].ultimoAcesso;
                viaSubstituicao = i;
            }
        }

        int enderecoBase = indiceBloco * tamanhoBloco;
        for (int i = 0; i < tamanhoBloco; i++) {
            conjunto[viaSubstituicao].dados[i] = memoriaPrincipal.getManual(enderecoBase + i);
        }
        conjunto[viaSubstituicao].tag = tag;
        conjunto[viaSubstituicao].validade = true;
        conjunto[viaSubstituicao].ultimoAcesso = System.nanoTime();

        return conjunto[viaSubstituicao].dados[offset];
    }

    @Override
    public void write(int endereco, short valor) {
        int indiceBloco = endereco / tamanhoBloco;
        int indiceConjunto = indiceBloco % numConjuntos;
        int tag = indiceBloco / numConjuntos;
        int offset = endereco % tamanhoBloco;

        memoriaPrincipal.setManual(endereco, valor);
        LinhaCache[] conjunto = conjuntos[indiceConjunto];

        for (int i = 0; i < vias; i++) {
            if (conjunto[i].validade && conjunto[i].tag == tag) {
                conjunto[i].dados[offset] = valor;
                conjunto[i].ultimoAcesso = System.nanoTime();
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
        return this.linhasFlattened;
    }
}