# Instruções para Executar o Simulador de Escalonamento de Processos

## Requisitos
- Um navegador web moderno (Chrome, Firefox, Edge, Safari)
- Não é necessário nenhum servidor web ou instalação adicional

## Passos para Executar

1. **Abrir o simulador**:
   - Navegue até a pasta do projeto `C:\Users\I554709\WebstormProjects\T1-SISOP-2025-1`
   - Clique duas vezes no arquivo `index.html` para abri-lo em seu navegador web
   - Alternativamente, você pode arrastar o arquivo `index.html` para uma janela do navegador

2. **Configurar o Quantum**:
   - No painel de configurações à esquerda, defina o valor do quantum para processos de tempo real
   - O valor padrão é 2, mas você pode ajustá-lo conforme necessário

3. **Adicionar Processos**:
   - Preencha o formulário de adição de processos:
     - Nome: Um identificador para o processo
     - Tempo de chegada: Momento em que o processo deve entrar no sistema
     - Tipo: Escolha entre "Tempo Real" ou "Melhor Esforço"
     - Prioridade: Para processos de tempo real, escolha entre Alta (0) ou Baixa (1)
     - Código Assembly: Escreva o código do processo usando a linguagem assembly hipotética

4. **Controlar a Simulação**:
   - Iniciar: Começa a simulação automática com intervalos de 1 segundo
   - Pausar: Interrompe a simulação automática
   - Avançar: Executa um único passo da simulação
   - Reiniciar: Limpa todos os processos e reinicia o tempo

5. **Interagir com Processos**:
   - Quando um processo solicita entrada (SYSCALL 2), digite o valor na caixa de entrada e clique em "Enviar"
   - Para testar a funcionalidade especial, digite "0101" na entrada quando solicitado

## Exemplo de Código Assembly

Você pode testar o simulador com os seguintes exemplos de código:

### Exemplo 1: Contagem regressiva e impressão
```
.code
  LOAD variable
  ponto1: SUB #1
  SYSCALL 1
  BRPOS ponto1
  SYSCALL 0
.endcode

.data
  variable 10
.enddata
```

### Exemplo 2: Leitura de entrada e impressão
```
.code
  SYSCALL 2
  SYSCALL 1
  SYSCALL 0
.endcode

.data
.enddata
```

### Exemplo 3: Teste da função especial (Ola)
```
.code
  SYSCALL 2
  SYSCALL 1
  SYSCALL 0
.endcode

.data
.enddata
```
Para este exemplo, digite "0101" quando solicitada uma entrada.

## Observando a Simulação

- A área de saída mostra os valores impressos pelos processos
- Os painéis de estado mostram os processos em cada estado (pronto, executando, bloqueado, finalizado)
- Para cada processo, você pode ver informações como tempo de execução, tempo de espera e quantum restante

## Notas Importantes

- Os processos de tempo real têm prioridade sobre os de melhor esforço
- Um processo de melhor esforço será interrompido se um processo de tempo real ficar pronto
- Processos bloqueados (após operações de E/S) permanecem nesse estado por 3-5 unidades de tempo
- Quando você digita "0101" em qualquer entrada solicitada por um processo, "Ola" será exibido na área de saída

