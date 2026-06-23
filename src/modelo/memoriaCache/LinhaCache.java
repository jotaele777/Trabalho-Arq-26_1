package modelo.memoriaCache;

public class LinhaCache {
    public int tag;
    public boolean validade;
    public boolean dirty; // Para lógica de Write-Back (opcional, mas bom ter)
    public short[] dados;
    public long ultimoAcesso; // Usado para a política LRU (Least Recently Used)

    public LinhaCache(int tamanhoBloco) {
        this.tag = -1;
        this.validade = false;
        this.dirty = false;
        this.dados = new short[tamanhoBloco];
        this.ultimoAcesso = System.nanoTime();
    }
}