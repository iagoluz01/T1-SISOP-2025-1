document.addEventListener('DOMContentLoaded', function() {
    const processManager = new ProcessManager();
    const scheduler = new ProcessScheduler(processManager);

    let simulationRunning = false;
    let simulationInterval = null;

    // Elementos da interface
    const quantumInput = document.getElementById('quantum');
    const processNameInput = document.getElementById('processName');
    const arrivalTimeInput = document.getElementById('arrivalTime');
    const processTypeSelect = document.getElementById('processType');
    const prioritySelect = document.getElementById('priority');
    const priorityDiv = document.getElementById('priorityDiv');
    const codeEditor = document.getElementById('codeEditor');
    const addProcessButton = document.getElementById('addProcess');
    const startButton = document.getElementById('startSimulation');
    const pauseButton = document.getElementById('pauseSimulation');
    const stepButton = document.getElementById('stepSimulation');
    const resetButton = document.getElementById('resetSimulation');
    const currentTimeDisplay = document.getElementById('currentTime');
    const outputConsole = document.getElementById('outputConsole');
    const inputField = document.getElementById('inputField');
    const submitInputButton = document.getElementById('submitInput');
    const realtimeQueueDisplay = document.getElementById('realtimeQueue');
    const besteffortQueueDisplay = document.getElementById('besteffortQueue');
    const runningProcessDisplay = document.getElementById('runningProcess');
    const blockedProcessesDisplay = document.getElementById('blockedProcesses');
    const finishedProcessesDisplay = document.getElementById('finishedProcesses');

    // Mostrar/ocultar o seletor de prioridade com base no tipo de processo
    processTypeSelect.addEventListener('change', function() {
        priorityDiv.style.display =
            processTypeSelect.value === 'realtime' ? 'block' : 'none';
    });

    // Inicializar a exibição
    priorityDiv.style.display = 'block';

    // Adicionar processo
    addProcessButton.addEventListener('click', function() {
        const name = processNameInput.value.trim() || `Processo ${processManager.nextProcessId}`;
        const arrivalTime = parseInt(arrivalTimeInput.value) || 0;
        const type = processTypeSelect.value;
        const priority = type === 'realtime' ? parseInt(prioritySelect.value) : 1;
        const code = codeEditor.value.trim();

        if (!code) {
            alert('O código do processo não pode estar vazio.');
            return;
        }

        try {
            const process = processManager.createProcess(name, code, arrivalTime, type, priority);

            // Configurar o handler de chamada de sistema para este processo
            process.interpreter.setSystemCallHandler((index, interpreter) => {
                switch (index) {
                    case -1: // Código especial para "Ola"
                        appendOutput("Ola");
                        break;
                    case 0: // Finalizar processo
                        processManager.terminateProcess(process);
                        break;
                    case 1: // Imprimir valor
                        appendOutput(`${process.name} imprime: ${interpreter.acc}`);
                        // Bloquear por tempo aleatório entre 3 e 5
                        const printBlockTime = Math.floor(Math.random() * 3) + 3;
                        processManager.blockProcess(process, printBlockTime);
                        break;
                    case 2: // Ler valor
                        appendOutput(`${process.name} aguarda entrada...`);
                        interpreter.waitingForInput = true;
                        break;
                }
            });

            alert(`Processo "${name}" adicionado com sucesso!`);
            processNameInput.value = '';
            arrivalTimeInput.value = '0';
            // Não limpar o editor para facilitar a adição de processos semelhantes
        } catch (error) {
            alert(`Erro ao adicionar processo: ${error.message}`);
        }

        updateUI();
    });

    // Iniciar simulação
    startButton.addEventListener('click', function() {
        if (simulationRunning) return;

        scheduler.setQuantum(parseInt(quantumInput.value) || 2);
        simulationRunning = true;

        simulationInterval = setInterval(() => {
            simulationStep();
        }, 1000);

        updateUI();
    });

    // Pausar simulação
    pauseButton.addEventListener('click', function() {
        if (!simulationRunning) return;

        clearInterval(simulationInterval);
        simulationRunning = false;

        updateUI();
    });

    // Passo a passo
    stepButton.addEventListener('click', function() {
        simulationStep();
        updateUI();
    });

    // Reiniciar simulação
    resetButton.addEventListener('click', function() {
        if (simulationRunning) {
            clearInterval(simulationInterval);
            simulationRunning = false;
        }

        processManager.reset();
        outputConsole.innerHTML = '';

        updateUI();
    });

    // Enviar entrada para processo em espera
    submitInputButton.addEventListener('click', function() {
        const inputValue = inputField.value.trim();

        if (!inputValue) {
            alert('Por favor, digite um valor.');
            return;
        }

        const runningProcess = processManager.getRunningProcess();

        if (runningProcess && runningProcess.interpreter.waitingForInput) {
            runningProcess.interpreter.handleInput(inputValue);
            appendOutput(`Entrada recebida: ${inputValue}`);
            inputField.value = '';

            // Verificar se o input é 0101 para mostrar "Ola"
            if (inputValue === "0101") {
                appendOutput("Ola");
            }

            // Bloquear por tempo aleatório entre 3 e 5
            const inputBlockTime = Math.floor(Math.random() * 3) + 3;
            processManager.blockProcess(runningProcess, inputBlockTime);

            updateUI();
        } else {
            alert('Nenhum processo está esperando entrada no momento.');
        }
    });

    // Função para executar um passo da simulação
    function simulationStep() {
        processManager.advanceTime();
        scheduler.schedule();
        scheduler.executeCurrentProcess();

        currentTimeDisplay.textContent = processManager.currentTime;

        updateUI();
    }

    // Função para adicionar saída ao console
    function appendOutput(text) {
        const line = document.createElement('div');
        line.textContent = `[${processManager.currentTime}] ${text}`;
        outputConsole.appendChild(line);
        outputConsole.scrollTop = outputConsole.scrollHeight;
    }

    // Atualizar a interface
    function updateUI() {
        currentTimeDisplay.textContent = processManager.currentTime;

        // Atualizar filas
        updateQueueDisplay(
            realtimeQueueDisplay,
            processManager.getReadyProcesses().filter(p => p.type === 'realtime')
        );

        updateQueueDisplay(
            besteffortQueueDisplay,
            processManager.getReadyProcesses().filter(p => p.type === 'besteffort')
        );

        // Atualizar processo em execução
        const runningProcess = processManager.getRunningProcess();
        runningProcessDisplay.innerHTML = '';

        if (runningProcess) {
            const processCard = createProcessCard(runningProcess, 'running');
            runningProcessDisplay.appendChild(processCard);
        }

        // Atualizar processos bloqueados
        updateProcessDisplay(blockedProcessesDisplay, processManager.getBlockedProcesses(), 'blocked');

        // Atualizar processos finalizados
        updateProcessDisplay(finishedProcessesDisplay, processManager.getFinishedProcesses(), 'finished');
    }

    // Atualizar exibição de fila
    function updateQueueDisplay(element, processes) {
        element.innerHTML = '';

        processes.forEach(process => {
            const processCard = createProcessCard(process);
            element.appendChild(processCard);
        });

        if (processes.length === 0) {
            element.innerHTML = '<div class="empty-queue">Vazia</div>';
        }
    }

    // Atualizar exibição de processos
    function updateProcessDisplay(element, processes, className) {
        element.innerHTML = '';

        processes.forEach(process => {
            const processCard = createProcessCard(process, className);
            element.appendChild(processCard);
        });

        if (processes.length === 0) {
            element.innerHTML = '<div class="empty-queue">Nenhum</div>';
        }
    }

    // Criar card de processo
    function createProcessCard(process, className = '') {
        const card = document.createElement('div');
        card.className = `process-card ${className}`;

        const header = document.createElement('div');
        header.className = 'process-header';
        header.innerHTML = `<strong>${process.name}</strong> (ID: ${process.id})`;

        const details = document.createElement('div');
        details.className = 'process-details';
        details.innerHTML = `
            <div>Tipo: ${process.type === 'realtime' ? 'Tempo Real' : 'Melhor Esforço'}</div>
            ${process.type === 'realtime' ? `<div>Prioridade: ${process.priority === 0 ? 'Alta' : 'Baixa'}</div>` : ''}
            <div>Chegada: ${process.arrivalTime}</div>
            <div>Estado: ${translateState(process.state)}</div>
            <div>Tempo de execução: ${process.executionTime}</div>
            <div>Tempo de espera: ${process.waitingTime}</div>
            ${process.state === 'blocked' ? `<div>Desbloqueio em: ${process.blockEndTime}</div>` : ''}
            ${process.state === 'running' ? `<div>Quantum restante: ${process.quantumLeft}</div>` : ''}
        `;

        card.appendChild(header);
        card.appendChild(details);

        return card;
    }

    // Traduzir estado do processo
    function translateState(state) {
        const states = {
            'new': 'Novo',
            'ready': 'Pronto',
            'running': 'Executando',
            'blocked': 'Bloqueado',
            'exit': 'Finalizado'
        };

        return states[state] || state;
    }

    // Inicializar UI
    updateUI();
});
* {
    box-sizing: border-box;
    font-family: Arial, sans-serif;
}

body {
    margin: 0;
    padding: 20px;
    background-color: #f5f5f5;
}

h1 {
    text-align: center;
    color: #333;
}

.container {
    display: flex;
    gap: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.config-panel, .simulation-panel {
    background: white;
    padding: 15px;
    border-radius: 8px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.config-panel {
    flex: 1;
}

.simulation-panel {
    flex: 2;
}

.process-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.process-form div {
    display: flex;
    flex-direction: column;
}

label {
    margin-bottom: 5px;
    font-weight: bold;
}

input, select, textarea {
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
}

button {
    padding: 10px 15px;
    background-color: #4CAF50;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-weight: bold;
}

button:hover {
    background-color: #45a049;
}

.controls {
    display: flex;
    gap: 10px;
    margin-bottom: 15px;
}

#pauseSimulation {
    background-color: #f39c12;
}

#resetSimulation {
    background-color: #e74c3c;
}

#stepSimulation {
    background-color: #3498db;
}

.time-display {
    font-size: 18px;
    margin-bottom: 15px;
    font-weight: bold;
}

.io-panel {
    display: flex;
    gap: 15px;
    margin-bottom: 15px;
}

.output-area, .input-area {
    flex: 1;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 10px;
}

#outputConsole {
    height: 100px;
    overflow-y: auto;
    background-color: #f9f9f9;
    padding: 5px;
    border: 1px solid #eee;
    font-family: monospace;
}

.queues {
    display: flex;
    gap: 15px;
}

.queue {
    flex: 1;
}

.queue-display, .process-display {
    min-height: 100px;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 10px;
    margin-bottom: 15px;
}

.process-card {
    background-color: #f9f9f9;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 8px;
    margin-bottom: 8px;
}

.process-card.running {
    background-color: #d5f5e3;
    border-color: #2ecc71;
}

.process-card.blocked {
    background-color: #fdebd0;
    border-color: #f39c12;
}

.process-card.finished {
    background-color: #ebdef0;
    border-color: #8e44ad;
}

#codeEditor {
    font-family: monospace;
    resize: vertical;
}

