# Instruções para o Simulador de Escalonamento de Processos (Terminal)

## Requisitos
- Java Development Kit (JDK) 17 ou superior
- Terminal de comando (Command Prompt, PowerShell, Bash, etc.)

## Como Compilar e Executar

### No Windows:
1. Abra um terminal na pasta do projeto
2. Execute:
   ```
   compile_and_run.bat
   ```

### No Linux/macOS:
1. Abra um terminal na pasta do projeto
2. Compile:
   ```
   mkdir -p bin
   javac -d bin src/*.java
   ```
3. Execute:
   ```
   java -cp bin Main
   ```

## Usando o Simulador no Terminal

O simulador apresenta um menu com as seguintes opções:

1. **Configurar quantum**: Define o quantum para processos de tempo real
2. **Adicionar processo**: Adiciona um novo processo ao simulador
3. **Iniciar simulação**: Inicia a simulação automática
4. **Pausar simulação**: Pausa a simulação automática
5. **Avançar um passo**: Executa um único passo da simulação
6. **Reiniciar simulação**: Limpa todos os processos e reinicia o tempo
7. **Exibir processos**: Mostra o estado atual de todos os processos
8. **Sair**: Encerra o simulador

### Adicionando um Processo

Ao selecionar a opção de adicionar um processo, você deverá fornecer:

1. Nome do processo (opcional)
2. Tempo de chegada
3. Tipo do processo (tempo real ou melhor esforço)
4. Prioridade (apenas para processos de tempo real)
5. Nome do arquivo com código assembly que deseja rodar

### Chamadas de Sistema (SYSCALL)

- **SYSCALL 0**: Termina o processo
- **SYSCALL 1**: Imprime o valor do acumulador
- **SYSCALL 2**: Solicita entrada do usuário

### Função Especial "Ola"

Para testar a funcionalidade especial que imprime "Ola", forneça o valor `0101` quando um processo solicitar entrada (após uma chamada SYSCALL 2).

### Visualizando o Estado da Simulação

Use a opção 7 do menu para exibir o estado atual de todos os processos:
- Filas de processos prontos (tempo real e melhor esforço)
- Processo em execução
- Processos bloqueados
- Processos finalizados

Para cada processo, são mostrados detalhes como ID, nome, tipo, prioridade, tempo de execução e tempo de espera.
