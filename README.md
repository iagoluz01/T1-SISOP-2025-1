

# TP1 - Projeto de Execução Dinâmica de Processos[^1]

## Sistemas Operacionais[^1]

O presente trabalho tem por objetivo explorar temas referentes ao escalonamento e troca entre processos que utilizam um dado processador.  É previsto o desenvolvimento de um ambiente que empregue uma política de escalonamento específica, bem como gerencie a inclusão e remoção de processos que ocupam o processador.  A carga de processos deverá ser realizada a partir de programas que utilizarão uma linguagem assembly hipotética.[^1]

## Descrição de programas[^1]

O usuário deverá ser capaz de descrever pequenos programas a serem executados pelo ambiente.  O ambiente de execução é baseado em acumulador.  Assim, para a execução de um programa, dois registradores estão presentes: (i) o acumulador (`acc`) onde as operações são realizadas e (ii) o ponteiro da instrução em execução (`pc`).  A linguagem de programação hipotética a ser empregada pelo usuário para a programação e que manipula os dois registradores descritos é apresentada na Tabela 1, definida em quatro categorias conforme listado na primeira coluna.[^1]

### Tabela 1 — Mnemônicos e funções[^1]

| Categoria [^1] | Mnemônico [^1] | Função [^1] |
| :-- | :-- | :-- |
| Aritmético [^1] | ADD op1 [^1] | `acc = acc + (op1)` [^1] |
| Aritmético [^1] | SUB op1 [^1] | `acc = acc – (op1)` [^1] |
| Aritmético [^1] | MULT op1 [^1] | `acc = acc * (op1)` [^1] |
| Aritmético [^1] | DIV op1 [^1] | `acc = acc / (op1)` [^1] |
| Memória [^1] | LOAD op1 [^1] | `acc = (op1)` [^1] |
| Memória [^1] | STORE op1 [^1] | `(op1) = acc` [^1] |
| Salto [^1] | BRANY label [^1] | `pc <- label` [^1] |
| Salto [^1] | BRPOS label [^1] | Se $acc > 0$ então $pc \leftarrow op1$ [^1] |
| Salto [^1] | BRZERO label [^1] | Se $acc = 0$ então $pc \leftarrow op1$ [^1] |
| Salto [^1] | BRNEG label [^1] | Se $acc < 0$ então $pc \leftarrow op1$ [^1] |
| Sistema [^1] | SYSCALL index [^1] | Chamada de sistema [^1] |

As instruções da categoria aritmético podem operar em modo de endereçamento direto ou imediato com a memória.  No modo de endereçamento imediato, o valor de `op1` pode ser o valor real a ser considerado na operação.  No modo de endereçamento direto, `op1` representa a variável na área de dados cujo valor deve ser capturado, sendo a diferenciação no código assembly dada pela presença do caractere sustenido (`#`) para o modo imediato, ou a ausência deste caractere para o modo direto.[^1]

Um exemplo desta situação é ilustrado na linha 3 do código de exemplo da Figura 1.  Duas instruções são utilizadas na categoria memória, representando leitura e escrita em memória de dados, sendo que a leitura pode ser realizada tanto em modo imediato quanto direto, conforme descrito anteriormente.  Já o comando de escrita (`STORE`) dá suporte apenas ao modo direto de operação, ou seja, um dado somente pode ser escrito em uma variável declarada na área de dados.[^1]

Para os mnemônicos de saltos condicionais, `label` representa a posição da área de código que deve ser assumida por `pc`.  Há quatro tipos de saltos, sendo um incondicional e três condicionais, assumindo-se as condições destacadas na Tabela 1.  Para a criação de labels, utilize um “nome” alfanumérico seguido de dois pontos (:), conforme ilustrado na Figura 1, e um exemplo de salto condicional está na linha 5 da mesma figura.[^1]

A última categoria da Tabela 1 representa a instrução de operação com o sistema, devendo ser possível associar três tipos de pedidos de operação com os valores ‘0’, ‘1’ e ‘2’.  A chamada de sistema com valor ‘0’ caracteriza um pedido de finalização/encerramento do programa (halt), a com valor ‘1’ caracteriza um pedido de impressão de um valor inteiro na tela, e a com valor ‘2’ caracteriza um pedido de leitura de um valor inteiro via teclado.  Às chamadas de sistema com valores ‘1’ e ‘2’ deve ser associada uma situação de bloqueio do processo por um valor aleatório entre 3 e 5 unidades de tempo.[^1]

```asm
1  .code
2    LOAD variable
3    ponto1: SUB #1
4    SYSCALL 1
5    BRPOS ponto1
6    SYSCALL 0
7  .endcode

8  .data
9    Variable 10
10 .enddata

#Define o início da área de código
# Carrega em acc o conteúdo de variable
# Subtrai do acc um valor constante (i.e. 1)
# Imprime na tela o conteúdo do acumulador
# Caso acc>0 deve voltar à linha marcada por “ponto1”
# Sinaliza o fim do programa
#Define o final da área de código

#Define o início da área de dados
# Conteúdo da posição 1 da área de dados é 10
#Define o final da área de dados
```

Figura 1 — Código exemplo.[^1]

Como finalização da descrição dos processos, devem ser assumidas as seguintes características.[^1]

- Ocupação de memória: Cada instrução ocupa uma posição da memória, independente de sua categoria; cada variável ocupa uma posição da memória; o número de posições de um programa é definido pelo número de instruções somado ao número de variáveis; a organização da ocupação da memória por parte dos processos está fora do escopo deste trabalho.[^1]
- Os valores atribuídos às variáveis ou às instruções que operam de modo imediato podem assumir valores tanto positivos quanto negativos.[^1]


## Políticas de escalonamento[^1]

O sistema deverá implementar um escalonamento baseado em filas multinível, atribuindo cada processo admitido a uma de duas filas: processos de tempo real ou processos de melhor esforço.  Processos na fila de tempo real têm prioridade sobre os de melhor esforço, de modo que um processo de melhor esforço só será escalonado se não existir processo apto na fila de tempo real, com preempção do processo de melhor esforço caso um processo de tempo real fique apto.[^1]

Cada fila de processos terá sua própria política: a fila de tempo real utilizará Round Robin (RR) com quantum definível e com prioridade por processo, enquanto a fila de melhor esforço utilizará FCFS.  Para cada processo será definido o seu tempo de chegada no sistema (admissão), havendo informações adicionais sobre políticas de escalonamento no Moodle da disciplina e no capítulo 6 do livro-texto.[^1]

Para a política RR, devem-se assumir dois níveis de prioridade: baixa prioridade (1) e alta prioridade (0), com prioridade padrão baixa no momento da carga do processo.  Uma vez carregado, o processo não pode sofrer alteração de prioridade (prioridade estática), devendo, em caso de prioridade igual à de outros, ser inserido ao final da sequência de processos com a mesma prioridade.[^1]

O algoritmo, a cada intervalo de tempo, interrompe o processador, reavalia as prioridades e decide qual processo deverá ocupar o processador no próximo instante, sendo o quantum de cada processo definido no início da operação.  Um processo deixa o estado de execução quando: (i) tem seu timeout alcançado pelo quantum (RR), (ii) existe um processo de mais alta prioridade pronto para execução, ou (iii) realiza uma chamada de sistema.[^1]

No caso de encerramento via chamada de sistema, o processo deve avançar para o estado de `exit` e o espaço que ocupava na memória deve ser liberado.  Em caso de impressão ou leitura via chamada de sistema, o processo assume o estado de bloqueado por intervalo aleatório entre 3 e 5 unidades de tempo e, passado este período, avança para o estado de pronto, retornando à sua fila de origem.[^1]

Para os algoritmos de escalonamento implementados, assume-se que, se um processo A for removido do processador para que um processo B o ocupe, quando da retomada de A, este deve seguir sua execução do último ponto de parada.  O tempo necessário para troca de contexto deve ser desconsiderado.[^1]

## Interface da aplicação[^1]

O ambiente a ser desenvolvido deve permitir definir quais processos serão carregados, seus instantes de carga (arrival time) e se o processo é de tempo real ou de melhor esforço.  Para RR, além do instante de carga, a interface deve permitir definir a prioridade para cada processo e o quantum; para FCFS, além do instante de carga, a interface deve permitir definir o tempo de execução.[^1]

Considere que os espaços necessários para o controle dos processos (PCB), tais como os valores de `PC`, `ACC` e o estado em que se encontra, fazem parte de um espaço reservado para o sistema operacional.  Como resultado da operação, deve-se permitir observar, para as duas filas, os estados diferentes (pronto, executando, bloqueado, finalizado) a cada instante de tempo.[^1]

Acrescente uma rotina no programa que imprima na tela a string “Ola” cada vez que for digitado o valor 0101.[^1]

## Informações adicionais[^1]

O trabalho deverá ser realizado em grupos de 4 alunos (obrigatoriamente), devendo ser entregue o código-fonte do programa e um manual do usuário em PDF com instruções de compilação e execução.  A linguagem de programação é de escolha do grupo, desde que seja possível compilar e executar no ambiente computacional disponível nas salas de aula laboratório do prédio 32.[^1]

A data de entrega está prevista para o dia 18/09/2025, até às 17h, e as apresentações serão realizadas a partir do material postado no Moodle.  Cada aluno deverá assinalar no Moodle o grupo ao qual pertence, e um integrante deverá escolher um horário para o grupo apresentar o trabalho no dia 18/09/2025 ou 23/09/2025, dentre os horários disponíveis no Moodle.[^1]

O trabalho deverá ser entregue no Moodle por meio de um arquivo compactado (.zip) cujo nome contenha o nome e sobrenome de todos os integrantes do grupo, sendo o material postado de inteira responsabilidade do aluno.  Arquivos corrompidos que impeçam a avaliação serão considerados como não entrega, trabalhos com erro de compilação não serão considerados, e casos de plágio/cópia ou realizados por sistemas de IA (por exemplo, ChatGPT, Gemini, Copilot e outros) receberão nota zero.[^1]

<div style="text-align: center">⁂</div>



