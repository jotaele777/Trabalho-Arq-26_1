# Trabalho-Arq-26_1
Aqui está o repositório para armazener o trabalho da disciplina de Arquitetura de Computadores
Simulador de Microarquitetura MIC-1 com Cache L1

Este projeto implementa um simulador visual e interativo da microarquitetura MIC-1 (baseado no modelo de Tanenbaum) com uma adição arquitetural significativa: Hierarquia de Memória (Cache L1).

Desenvolvido como projeto prático para a disciplina de Arquitetura de Computadores.

🚀 Funcionalidades

Simulação de CPU em Nível de Subciclos: Visualização completa do ciclo de busca, decodificação e execução através das microinstruções.

Hierarquia de Memória (Polimorfismo): O simulador suporta três arquiteturas de Cache trocando apenas a instância no código:

Mapeamento Direto.

Totalmente Associativa (com política LRU - Least Recently Used).

Associativa por Conjunto.

Política de Escrita Inteligente: Implementação de política Write-Around (Não aloca na escrita), enviando dados direto para a RAM para não poluir o cache de instruções.

Visualização em Tempo Real: Acompanhamento de Hits, Misses, validade, tags, dados e o fenômeno de evicção (expulsão).

Montador (Assembler) de Duas Passagens Integrado: Aceita código Assembly (ex: LODD, STOD, LOCO), suporta o uso de Rótulos (Labels) e faz a alocação dinâmica de Variáveis na memória principal automaticamente.

Interface Gráfica: Desenvolvida nativamente em Java Swing.

📦 Como Executar

Pré-requisitos

Java 17 ou superior (testado no Java 22).

Rodando o JAR (Recomendado)

Baixe o arquivo SimuladorMIC1.jar. Não é necessário instalar dependências. No terminal, execute:

java -jar SimuladorMIC1.jar


(Dê um duplo clique no arquivo .jar caso o seu sistema operacional esteja configurado para abrir arquivos Java diretamente).

Via Docker

Para construir a imagem e rodar o container de forma isolada:

Construir a imagem:

docker build -t simulador-mic1 .


Rodar (Requer configuração de X11 para GUI no Windows/Linux/Mac):

docker run -it --rm --net=host -e DISPLAY=$DISPLAY simulador-mic1


🧪 Teste Demonstrativo (O Fenômeno do Thrashing)

Para testar o poder do simulador, a alocação de variáveis e o monitoramento da cache, cole o código abaixo na interface:

INICIO: LOCO 50
        STOD contador
        LOCO 1
        ADDD contador
        JUMP INICIO


O que observar:

No Mapeamento Direto: Você notará um alto número de Misses, pois a variável contador (alocada dinamicamente pelo nosso Montador) conflita com o espaço das instruções, gerando expulsões sucessivas na Linha L00.

Na Cache Associativa: A variável buscará uma linha vazia (L01), convivendo em harmonia com as instruções na L00 e elevando o Hit Rate para o nível máximo!
