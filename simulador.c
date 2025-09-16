/**
 * @file simulador.c
 * @brief TP1 - Simulador de Execução Dinâmica de Processos de SO.
 *
 * Este programa simula um sistema operativo com um escalonador de processos
 * multinível. Carrega programas escritos numa linguagem Assembly hipotética,
 * gere os seus estados e executa-os de acordo com as políticas de escalonamento.
 *
 * - Fila de Tempo Real: Escalonamento Round Robin com 2 níveis de prioridade estática.
 * - Fila de Melhor Esforço: Escalonamento First-Come, First-Served (FCFS).
 *
 * O simulador opera num ciclo de tempo discreto, mostrando o estado de todos
 * os processos a cada unidade de tempo.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <ctype.h>
#include <time.h>

#define MAX_CODE_SIZE 100
#define MAX_DATA_SIZE 100
#define MAX_LINE_LENGTH 256
#define MAX_PROCESSES 10
#define MAX_LABEL_LENGTH 50

// Enum para os opcodes das instruções
typedef enum {
    ADD, SUB, MULT, DIV,
    LOAD, STORE,
    BRANY, BRPOS, BRZERO, BRNEG,
    SYSCALL,
    INVALIDO
} OpCode;

// Enum para os tipos de operando
typedef enum {
    IMEDIATO, DIRETO, LABEL, VALOR_SYSCALL
} TipoOperando;

// Enum para os estados do processo
typedef enum {
    PRONTO, EXECUTANDO, BLOQUEADO, FINALIZADO, NOVO
} EstadoProcesso;

// Estrutura para representar uma instrução
typedef struct {
    OpCode opcode;
    TipoOperando tipo_op;
    int valor_op; // Pode ser um valor, índice de variável ou índice de linha (para labels)
    char label_str[MAX_LABEL_LENGTH]; // Guarda o nome do label para resolução
} Instrucao;

// Estrutura para mapear nomes de variáveis/labels para índices
typedef struct {
    char nome[MAX_LABEL_LENGTH];
    int indice;
} MapaNomeIndice;

// Estrutura para o Bloco de Controlo de Processo (PCB)
typedef struct Processo {
    int pid;
    int pc; // Program Counter
    int acc; // Acumulador
    EstadoProcesso estado;

    // Detalhes do escalonamento
    bool eh_tempo_real;
    int prioridade; // 0=alta, 1=baixa
    int quantum_total;
    int quantum_restante;
    int tempo_chegada;
    int tempo_bloqueado_restante;

    // Memória do processo
    Instrucao codigo[MAX_CODE_SIZE];
    int num_instrucoes;
    int memoria_dados[MAX_DATA_SIZE];
    int num_variaveis;

    // Mapeamentos
    MapaNomeIndice mapa_variaveis[MAX_DATA_SIZE];
    MapaNomeIndice mapa_labels[MAX_CODE_SIZE];
    int num_labels;


    struct Processo* proximo;
} Processo;

// --- Filas do Escalonador ---
Processo* fila_tempo_real_p0 = NULL; // Prioridade alta
Processo* fila_tempo_real_p1 = NULL; // Prioridade baixa
Processo* fila_melhor_esforco = NULL;
Processo* lista_bloqueados = NULL;
Processo* lista_finalizados = NULL;
Processo* lista_novos = NULL; // Processos a aguardar tempo de chegada

Processo* processo_executando = NULL;
int tempo_global = 0;
int proximo_pid = 0;


// --- Funções Auxiliares e de Fila ---

/**
 * @brief Adiciona um processo ao final de uma fila.
 * @param cabeca Ponteiro para a cabeça da fila.
 * @param p Processo a ser adicionado.
 */
void enfileirar(Processo** cabeca, Processo* p) {
    p->proximo = NULL;
    if (*cabeca == NULL) {
        *cabeca = p;
        return;
    }
    Processo* atual = *cabeca;
    while (atual->proximo != NULL) {
        atual = atual->proximo;
    }
    atual->proximo = p;
}

/**
 * @brief Remove e retorna o primeiro processo de uma fila.
 * @param cabeca Ponteiro para a cabeça da fila.
 * @return O processo removido ou NULL se a fila estiver vazia.
 */
Processo* desenfileirar(Processo** cabeca) {
    if (*cabeca == NULL) {
        return NULL;
    }
    Processo* p = *cabeca;
    *cabeca = p->proximo;
    p->proximo = NULL;
    return p;
}

/**
 * @brief Remove um processo específico de uma lista.
 * @param cabeca Ponteiro para a cabeça da lista.
 * @param pid O PID do processo a ser removido.
 * @return O processo removido ou NULL se não for encontrado.
 */
Processo* remover_da_lista(Processo** cabeca, int pid) {
    Processo* atual = *cabeca;
    Processo* anterior = NULL;

    while (atual != NULL && atual->pid != pid) {
        anterior = atual;
        atual = atual->proximo;
    }

    if (atual == NULL) return NULL; // Não encontrado

    if (anterior == NULL) { // Era a cabeça da lista
        *cabeca = atual->proximo;
    } else {
        anterior->proximo = atual->proximo;
    }
    atual->proximo = NULL;
    return atual;
}


// --- Funções de Parsing do Assembly ---

/**
 * @brief Mapeia uma string de mnemónico para o seu OpCode correspondente (case-insensitive).
 * @param str A string do mnemónico.
 * @return O OpCode.
 */
OpCode string_para_opcode(const char* str) {
    char upper_str[MAX_LINE_LENGTH];
    int i = 0;
    while (str[i]) {
        upper_str[i] = toupper(str[i]);
        i++;
    }
    upper_str[i] = '\0';

    if (strcmp(upper_str, "ADD") == 0) return ADD;
    if (strcmp(upper_str, "SUB") == 0) return SUB;
    if (strcmp(upper_str, "MULT") == 0) return MULT;
    if (strcmp(upper_str, "DIV") == 0) return DIV;
    if (strcmp(upper_str, "LOAD") == 0) return LOAD;
    if (strcmp(upper_str, "STORE") == 0) return STORE;
    if (strcmp(upper_str, "BRANY") == 0) return BRANY;
    if (strcmp(upper_str, "BRPOS") == 0) return BRPOS;
    if (strcmp(upper_str, "BRZERO") == 0) return BRZERO;
    if (strcmp(upper_str, "BRNEG") == 0) return BRNEG;
    if (strcmp(upper_str, "SYSCALL") == 0) return SYSCALL;
    return INVALIDO;
}

/**
 * @brief Resolve o índice de uma variável pelo nome.
 * @param p O processo.
 * @param nome O nome da variável.
 * @return O índice na memória de dados ou -1 se não encontrar.
 */
int resolve_variavel(Processo* p, const char* nome) {
    for (int i = 0; i < p->num_variaveis; i++) {
        if (strcmp(p->mapa_variaveis[i].nome, nome) == 0) {
            return p->mapa_variaveis[i].indice;
        }
    }
    return -1;
}

/**
 * @brief Resolve o índice de um label pelo nome.
 * @param p O processo.
 * @param nome O nome do label.
 * @return O índice na memória de código ou -1 se não encontrar.
 */
int resolve_label(Processo* p, const char* nome) {
    for (int i = 0; i < p->num_labels; i++) {
        if (strcmp(p->mapa_labels[i].nome, nome) == 0) {
            return p->mapa_labels[i].indice;
        }
    }
    return -1;
}


/**
 * @brief Carrega um programa Assembly de um ficheiro para uma estrutura de Processo.
 * @param nome_arquivo O caminho do ficheiro .asm.
 * @param eh_tempo_real Se o processo é de tempo real.
 * @param tempo_chegada O tempo de chegada do processo.
 * @param prioridade A prioridade (para tempo real).
 * @param quantum O quantum (para tempo real).
 * @return Um ponteiro para o novo Processo criado, ou NULL em caso de erro.
 */
Processo* carregar_programa(const char* nome_arquivo, bool eh_tempo_real, int tempo_chegada, int prioridade, int quantum) {
    FILE* f = fopen(nome_arquivo, "r");
    if (!f) {
        perror("Erro ao abrir ficheiro");
        return NULL;
    }

    Processo* p = (Processo*)calloc(1, sizeof(Processo));
    if (!p) {
        perror("Erro de alocação de memória para processo");
        fclose(f);
        return NULL;
    }

    // Inicialização do PCB
    p->pid = proximo_pid++;
    p->pc = 0;
    p->acc = 0;
    p->estado = NOVO;
    p->proximo = NULL;
    p->eh_tempo_real = eh_tempo_real;
    p->tempo_chegada = tempo_chegada;
    p->prioridade = prioridade;
    p->quantum_total = quantum;
    p->quantum_restante = quantum;

    char linha[MAX_LINE_LENGTH];
    bool lendo_codigo = false, lendo_dados = false;

    // --- Primeira Passagem: Mapear labels ---
    int num_linha_codigo = 0;
    while (fgets(linha, sizeof(linha), f)) {
        char* token = strtok(linha, " \t\n\r");
        if (!token || token[0] == '#') continue;

        if (strcmp(token, ".code") == 0) { lendo_codigo = true; lendo_dados = false; continue; }
        if (strcmp(token, ".endcode") == 0) { lendo_codigo = false; continue; }
        if (strcmp(token, ".data") == 0) { lendo_dados = true; lendo_codigo = false; continue; }
        if (strcmp(token, ".enddata") == 0) { lendo_dados = false; continue; }

        if (lendo_codigo) {
            char* label = strchr(token, ':');
            if (label) {
                *label = '\0'; // Remove o ':'
                strcpy(p->mapa_labels[p->num_labels].nome, token);
                p->mapa_labels[p->num_labels].indice = num_linha_codigo;
                p->num_labels++;
                token = strtok(NULL, " \t\n\r"); // Pega o próximo token (a instrução)
                if(!token) continue;
            }
            num_linha_codigo++;
        }
    }
    rewind(f); // Volta ao início do ficheiro para a segunda passagem

    // --- Segunda Passagem: Ler instruções e dados ---
    lendo_codigo = false;
    lendo_dados = false;
    while (fgets(linha, sizeof(linha), f)) {
        char* original_linha = strdup(linha); // Cópia para parsing
        char* token = strtok(linha, " \t\n\r");

        if (!token || token[0] == '#') {
            free(original_linha);
            continue;
        }

        if (strcmp(token, ".code") == 0) { lendo_codigo = true; lendo_dados = false; free(original_linha); continue; }
        if (strcmp(token, ".endcode") == 0) { lendo_codigo = false; free(original_linha); continue; }
        if (strcmp(token, ".data") == 0) { lendo_dados = true; lendo_codigo = false; free(original_linha); continue; }
        if (strcmp(token, ".enddata") == 0) { lendo_dados = false; free(original_linha); continue; }

        if (lendo_codigo) {
            char* label_check = strchr(token, ':');
            if(label_check) {
                token = strtok(NULL, " \t\n\r");
                if(!token) {
                    free(original_linha);
                    continue;
                }
            }

            Instrucao* instr = &p->codigo[p->num_instrucoes];
            instr->opcode = string_para_opcode(token);
            char* operando_str = strtok(NULL, " \t\n\r");

            if (operando_str && operando_str[0] == '#') { // Comentário
                operando_str = NULL;
            }

            if (operando_str) {
                 if (instr->opcode >= BRANY && instr->opcode <= BRNEG) {
                    instr->tipo_op = LABEL;
                    strcpy(instr->label_str, operando_str);
                } else if (instr->opcode == SYSCALL) {
                    instr->tipo_op = VALOR_SYSCALL;
                    instr->valor_op = atoi(operando_str);
                } else if (operando_str[0] == '#') { // Imediato
                    instr->tipo_op = IMEDIATO;
                    instr->valor_op = atoi(&operando_str[1]);
                } else { // Direto
                    instr->tipo_op = DIRETO;
                    strcpy(instr->label_str, operando_str); // Guarda nome para resolver depois
                }
            }
            p->num_instrucoes++;

        } else if (lendo_dados) {
            strcpy(p->mapa_variaveis[p->num_variaveis].nome, token);
            p->mapa_variaveis[p->num_variaveis].indice = p->num_variaveis;

            char* valor_str = strtok(NULL, " \t\n\r");
            p->memoria_dados[p->num_variaveis] = atoi(valor_str);
            p->num_variaveis++;
        }
        free(original_linha);
    }
    fclose(f);

    // --- Terceira Passagem (Resolução de Nomes) ---
    for(int i = 0; i < p->num_instrucoes; i++) {
        Instrucao* instr = &p->codigo[i];
        if (instr->tipo_op == DIRETO) {
            instr->valor_op = resolve_variavel(p, instr->label_str);
            if (instr->valor_op == -1) {
                fprintf(stderr, "Erro: Variável '%s' não declarada no processo %d.\n", instr->label_str, p->pid);
                free(p);
                return NULL;
            }
        } else if (instr->tipo_op == LABEL) {
            instr->valor_op = resolve_label(p, instr->label_str);
             if (instr->valor_op == -1) {
                fprintf(stderr, "Erro: Label '%s' não declarada no processo %d.\n", instr->label_str, p->pid);
                free(p);
                return NULL;
            }
        }
    }

    printf("Programa '%s' carregado para o PID %d.\n", nome_arquivo, p->pid);
    enfileirar(&lista_novos, p);

    return p;
}


// --- Lógica do Simulador e CPU ---

/**
 * @brief Executa uma única instrução do processo em execução.
 */
void executar_instrucao() {
    if (!processo_executando || processo_executando->pc >= processo_executando->num_instrucoes) {
        // Se o PC for inválido, finaliza o processo para evitar ciclos infinitos.
        if(processo_executando) {
            printf("AVISO: PC fora dos limites para o PID %d. A finalizar processo.\n", processo_executando->pid);
            processo_executando->estado = FINALIZADO;
        }
        return;
    }

    Processo* p = processo_executando;
    Instrucao instr = p->codigo[p->pc];
    int operando_val = 0;

    // Obter valor do operando
    if (instr.tipo_op == IMEDIATO) {
        operando_val = instr.valor_op;
    } else if (instr.tipo_op == DIRETO) {
        operando_val = p->memoria_dados[instr.valor_op];
    }

    p->pc++; // Incremento padrão, saltos podem sobrescrever

    switch (instr.opcode) {
        case ADD: p->acc += operando_val; break;
        case SUB: p->acc -= operando_val; break;
        case MULT: p->acc *= operando_val; break;
        case DIV:
            if(operando_val == 0) {
                printf("ERRO: Divisão por zero no PID %d. A finalizar.\n", p->pid);
                p->estado = FINALIZADO;
            } else {
                p->acc /= operando_val;
            }
            break;
        case LOAD: p->acc = operando_val; break;
        case STORE: p->memoria_dados[instr.valor_op] = p->acc; break;
        case BRANY: p->pc = instr.valor_op; break;
        case BRPOS: if (p->acc > 0) p->pc = instr.valor_op; break;
        case BRZERO: if (p->acc == 0) p->pc = instr.valor_op; break;
        case BRNEG: if (p->acc < 0) p->pc = instr.valor_op; break;
        case SYSCALL:
            switch (instr.valor_op) {
                case 0: // Finalizar
                    p->estado = FINALIZADO;
                    printf("INFO: PID %d solicitou finalização (SYSCALL 0).\n", p->pid);
                    break;
                case 1: // Imprimir
                    printf("SAÍDA PID %d: %d\n", p->pid, p->acc);
                    p->estado = BLOQUEADO;
                    p->tempo_bloqueado_restante = (rand() % 3) + 3; // 3 a 5
                    break;
                case 2: // Ler
                    printf("ENTRADA para PID %d (digite um número inteiro): ", p->pid);
                    scanf("%d", &p->acc);
                    p->estado = BLOQUEADO;
                    p->tempo_bloqueado_restante = (rand() % 3) + 3; // 3 a 5
                    break;
            }
            break;
        default:
             printf("ERRO: Instrução inválida no PID %d. A finalizar.\n", p->pid);
             p->estado = FINALIZADO;
             break;
    }
}

/**
 * @brief Adiciona um processo à fila de pronto apropriada.
 * @param p O processo a ser adicionado.
 */
void mover_para_pronto(Processo* p) {
    p->estado = PRONTO;
    if (p->eh_tempo_real) {
        if (p->prioridade == 0) {
            enfileirar(&fila_tempo_real_p0, p);
        } else {
            enfileirar(&fila_tempo_real_p1, p);
        }
    } else {
        enfileirar(&fila_melhor_esforco, p);
    }
}

/**
 * @brief Seleciona o próximo processo a ser executado com base nas regras de escalonamento.
 * @return O processo selecionado.
 */
Processo* selecionar_proximo_processo() {
    if (fila_tempo_real_p0 != NULL) return desenfileirar(&fila_tempo_real_p0);
    if (fila_tempo_real_p1 != NULL) return desenfileirar(&fila_tempo_real_p1);
    if (fila_melhor_esforco != NULL) return desenfileirar(&fila_melhor_esforco);
    return NULL;
}


/**
 * @brief Imprime o estado atual de todas as filas.
 */
void imprimir_estado_sistema() {
    printf("--- Tempo: %2d ---\n", tempo_global);

    if (processo_executando) {
        printf("A executar : PID %d (PC=%d, ACC=%d)\n", processo_executando->pid, processo_executando->pc, processo_executando->acc);
    } else {
        printf("A executar : Nenhum\n");
    }

    printf("Prontos TR_P0: ");
    for(Processo* p = fila_tempo_real_p0; p; p=p->proximo) printf("PID %d | ", p->pid);
    printf("\n");

    printf("Prontos TR_P1: ");
    for(Processo* p = fila_tempo_real_p1; p; p=p->proximo) printf("PID %d | ", p->pid);
    printf("\n");

    printf("Prontos ME   : ");
    for(Processo* p = fila_melhor_esforco; p; p=p->proximo) printf("PID %d | ", p->pid);
    printf("\n");

    printf("Bloqueados   : ");
    for(Processo* p = lista_bloqueados; p; p=p->proximo) printf("PID %d (%d) | ", p->pid, p->tempo_bloqueado_restante);
    printf("\n");
    printf("-----------------\n\n");
}

/**
 * @brief O ciclo principal do simulador.
 */
void ciclo_simulador() {
     bool sistema_ativo = true;
     while(sistema_ativo) {
        imprimir_estado_sistema();

        // 1. Admitir novos processos
        Processo* p_novo = lista_novos;
        Processo* anterior = NULL;
        while(p_novo != NULL) {
            Processo* proximo_da_lista = p_novo->proximo;
            if(p_novo->tempo_chegada <= tempo_global) {
                printf("INFO: PID %d admitido no sistema.\n", p_novo->pid);
                Processo* p_admitido = remover_da_lista(&lista_novos, p_novo->pid);
                mover_para_pronto(p_admitido);
            }
            p_novo = proximo_da_lista;
        }


        // 2. Verificar processos bloqueados
        Processo* p_bloqueado = lista_bloqueados;
        while(p_bloqueado != NULL) {
            Processo* proximo_bloqueado = p_bloqueado->proximo;
            p_bloqueado->tempo_bloqueado_restante--;
            if (p_bloqueado->tempo_bloqueado_restante <= 0) {
                 printf("INFO: PID %d desbloqueado.\n", p_bloqueado->pid);
                 Processo* p_pronto = remover_da_lista(&lista_bloqueados, p_bloqueado->pid);
                 mover_para_pronto(p_pronto);
            }
            p_bloqueado = proximo_bloqueado;
        }

        // 3. Lógica de Preempção
        bool houve_preempcao = false;
        if (processo_executando && !processo_executando->eh_tempo_real) {
            if (fila_tempo_real_p0 != NULL || fila_tempo_real_p1 != NULL) {
                printf("INFO: Preempção! PID %d (ME) cede CPU para processo de TR.\n", processo_executando->pid);
                mover_para_pronto(processo_executando);
                processo_executando = NULL;
                houve_preempcao = true;
            }
        }

        // 4. Lidar com o processo que estava a executar
        if (processo_executando && !houve_preempcao) {
            if (processo_executando->estado == FINALIZADO) {
                 printf("INFO: PID %d finalizado e removido.\n", processo_executando->pid);
                 enfileirar(&lista_finalizados, processo_executando);
                 processo_executando = NULL;
            } else if (processo_executando->estado == BLOQUEADO) {
                 printf("INFO: PID %d bloqueado por E/S.\n", processo_executando->pid);
                 enfileirar(&lista_bloqueados, processo_executando);
                 processo_executando = NULL;
            } else if (processo_executando->eh_tempo_real && processo_executando->quantum_restante <= 0) {
                 printf("INFO: Quantum do PID %d esgotado.\n", processo_executando->pid);
                 processo_executando->quantum_restante = processo_executando->quantum_total;
                 mover_para_pronto(processo_executando);
                 processo_executando = NULL;
            }
        }

        // 5. Escalonar novo processo se a CPU estiver ociosa
        if (processo_executando == NULL) {
            processo_executando = selecionar_proximo_processo();
            if (processo_executando) {
                processo_executando->estado = EXECUTANDO;
                processo_executando->quantum_restante = processo_executando->quantum_total; // Reinicia o quantum
            }
        }

        // 6. Executar instrução
        if (processo_executando) {
            executar_instrucao();
            if (processo_executando->eh_tempo_real) {
                processo_executando->quantum_restante--;
            }
        } else {
            printf("INFO: CPU Ociosa.\n");
        }

        tempo_global++;

        // Critério de paragem
        sistema_ativo = (lista_novos != NULL || fila_tempo_real_p0 != NULL || fila_tempo_real_p1 != NULL || fila_melhor_esforco != NULL || lista_bloqueados != NULL || processo_executando != NULL);

        // Pequena pausa para facilitar a leitura
        #ifdef _WIN32
        // Sleep(1000); // 1 segundo no Windows
        #else
        // usleep(500000); // 0.5 segundos em sistemas POSIX
        #endif
     }
     printf("\n--- Simulação Finalizada em %d unidades de tempo ---\n", tempo_global);
}

void limpar_lista(Processo** cabeca) {
    Processo* atual = *cabeca;
    while(atual != NULL) {
        Processo* temp = atual;
        atual = atual->proximo;
        free(temp);
    }
    *cabeca = NULL;
}


int main() {
    srand(time(NULL));

    // --- Cenário de Teste com os 3 programas ---
    printf("A carregar novo cenário de simulação...\n");

    // Programa 1: Processo simples e rápido, de melhor esforço.
    carregar_programa("prog1.asm", false, 0, 1, 0);

    // Programa 2: Processo que calcula fibonacci, interativo (pede input).
    // Será de tempo real com baixa prioridade.
    carregar_programa("prog2.asm", true, 1, 1, 4);

    // Programa 3: Outro processo com ciclo.
    // Será de tempo real com alta prioridade para testar preempção.
    carregar_programa("prog3.asm", true, 3, 0, 3);


    ciclo_simulador();

    // Libertar memória de todos os processos finalizados
    limpar_lista(&lista_finalizados);

    return 0;
}

