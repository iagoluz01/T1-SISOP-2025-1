class ProcessScheduler {
    constructor(processManager) {
        this.processManager = processManager;
        this.quantum = 2; // Quantum padrão
        this.currentProcess = null;
    }

    setQuantum(quantum) {
        this.quantum = Math.max(1, parseInt(quantum) || 2);
    }

    schedule() {
        // Verificar se há algum processo em execução
        const runningProcess = this.processManager.getRunningProcess();

        // Se houver processo em execução
        if (runningProcess) {
            // Verificar se o quantum acabou
            if (runningProcess.quantumLeft <= 0 && runningProcess.type === 'realtime') {
                runningProcess.state = 'ready';
                this.currentProcess = null;
            }
            // Se o processo estiver esperando input, não fazemos nada
            else if (runningProcess.interpreter.waitingForInput) {
                return;
            }
            // Se o processo terminou ou foi bloqueado, já foi tratado pelo sistema
            else if (runningProcess.state === 'exit' || runningProcess.state === 'blocked') {
                this.currentProcess = null;
            }
        }

        // Se não há processo em execução, seleciona o próximo
        if (!this.currentProcess) {
            // Primeiro, verifica se há processos de tempo real prontos
            const realtimeProcesses = this.processManager.getReadyProcesses()
                .filter(p => p.type === 'realtime')
                .sort((a, b) => {
                    // Ordenar por prioridade (0 - alta, 1 - baixa)
                    if (a.priority !== b.priority) {
                        return a.priority - b.priority;
                    }
                    // Em caso de mesma prioridade, ordenar por tempo de chegada
                    return a.arrivalTime - b.arrivalTime;
                });

            // Se houver processos de tempo real prontos
            if (realtimeProcesses.length > 0) {
                this.currentProcess = realtimeProcesses[0];
                this.currentProcess.state = 'running';
                this.currentProcess.quantumLeft = this.quantum;
            } else {
                // Senão, verificar se há processos de melhor esforço prontos (FCFS)
                const besteffortProcesses = this.processManager.getReadyProcesses()
                    .filter(p => p.type === 'besteffort')
                    .sort((a, b) => a.arrivalTime - b.arrivalTime);

                if (besteffortProcesses.length > 0) {
                    this.currentProcess = besteffortProcesses[0];
                    this.currentProcess.state = 'running';
                    // Processos de melhor esforço não têm quantum limitado
                    this.currentProcess.quantumLeft = Infinity;
                }
            }
        }

        // Verificar se um processo de tempo real ficou pronto enquanto um de melhor esforço está executando
        if (this.currentProcess && this.currentProcess.type === 'besteffort') {
            const realtimeReady = this.processManager.getReadyProcesses()
                .some(p => p.type === 'realtime');

            if (realtimeReady) {
                // Preempção: interrompe o processo de melhor esforço
                this.currentProcess.state = 'ready';
                this.currentProcess = null;
                // O próximo ciclo de escalonamento selecionará o processo de tempo real
                this.schedule();
            }
        }

        return this.currentProcess;
    }

    executeCurrentProcess() {
        if (!this.currentProcess || this.currentProcess.state !== 'running') {
            return false;
        }

        try {
            return this.currentProcess.interpreter.executeInstruction();
        } catch (error) {
            console.error(`Erro na execução do processo ${this.currentProcess.id}: ${error.message}`);
            this.processManager.terminateProcess(this.currentProcess);
            return false;
        }
    }
}

