class Process {
    constructor(id, name, code, arrivalTime, type, priority = 1) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.arrivalTime = arrivalTime;
        this.type = type; // 'realtime' ou 'besteffort'
        this.priority = priority; // 0 (alta) ou 1 (baixa) para tempo real
        this.state = 'new'; // new, ready, running, blocked, exit
        this.interpreter = new AssemblyInterpreter();
        this.interpreter.parseProgram(code);
        this.blockEndTime = 0;
        this.quantumLeft = 0;
        this.executionTime = 0; // Tempo total de execução
        this.waitingTime = 0;   // Tempo total de espera
    }

    saveState() {
        return {
            acc: this.interpreter.acc,
            pc: this.interpreter.pc,
            data: {...this.interpreter.data},
            running: this.interpreter.running,
            blocked: this.interpreter.blocked,
            blockTime: this.interpreter.blockTime,
            waitingForInput: this.interpreter.waitingForInput
        };
    }

    restoreState(state) {
        this.interpreter.acc = state.acc;
        this.interpreter.pc = state.pc;
        this.interpreter.data = {...state.data};
        this.interpreter.running = state.running;
        this.interpreter.blocked = state.blocked;
        this.interpreter.blockTime = state.blockTime;
        this.interpreter.waitingForInput = state.waitingForInput;
    }
}

class ProcessManager {
    constructor() {
        this.processes = [];
        this.nextProcessId = 1;
        this.currentTime = 0;
    }

    createProcess(name, code, arrivalTime, type, priority) {
        const process = new Process(
            this.nextProcessId++,
            name,
            code,
            arrivalTime,
            type,
            priority
        );
        this.processes.push(process);
        return process;
    }

    getProcessesByState(state) {
        return this.processes.filter(p => p.state === state);
    }

    getReadyProcesses() {
        return this.processes.filter(p => p.state === 'ready');
    }

    getRunningProcess() {
        return this.processes.find(p => p.state === 'running');
    }

    getBlockedProcesses() {
        return this.processes.filter(p => p.state === 'blocked');
    }

    getFinishedProcesses() {
        return this.processes.filter(p => p.state === 'exit');
    }

    getProcessById(id) {
        return this.processes.find(p => p.id === id);
    }

    advanceTime() {
        this.currentTime++;

        // Verificar processos que chegaram
        this.processes.forEach(process => {
            if (process.state === 'new' && process.arrivalTime <= this.currentTime) {
                process.state = 'ready';
            }
        });

        // Verificar processos bloqueados
        this.getBlockedProcesses().forEach(process => {
            if (process.blockEndTime <= this.currentTime) {
                process.state = 'ready';
                process.interpreter.blocked = false;
            }
        });

        // Atualizar tempo de espera para processos prontos
        this.getReadyProcesses().forEach(process => {
            process.waitingTime++;
        });

        // Atualizar tempo de execução para o processo em execução
        const runningProcess = this.getRunningProcess();
        if (runningProcess) {
            runningProcess.executionTime++;
            runningProcess.quantumLeft--;
        }
    }

    blockProcess(process, duration) {
        if (process && process.state === 'running') {
            process.state = 'blocked';
            process.interpreter.blocked = true;
            process.blockEndTime = this.currentTime + duration;
        }
    }

    terminateProcess(process) {
        if (process) {
            process.state = 'exit';
            process.interpreter.running = false;
        }
    }

    reset() {
        this.processes = [];
        this.nextProcessId = 1;
        this.currentTime = 0;
    }
}

