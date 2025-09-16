class AssemblyInterpreter {
    constructor() {
        this.reset();
    }

    reset() {
        this.acc = 0;        // Acumulador
        this.pc = 0;         // Program counter
        this.code = [];      // Instruções
        this.data = {};      // Área de dados
        this.labels = {};    // Mapeamento de labels para posições no código
        this.running = false;
        this.blocked = false;
        this.blockTime = 0;
        this.waitingForInput = false;
        this.systemCallHandler = null;
    }

    parseProgram(programText) {
        this.reset();

        let lines = programText.split('\n')
            .map(line => line.trim())
            .filter(line => line && !line.startsWith('#'));

        let inCodeSection = false;
        let inDataSection = false;
        let lineCounter = 0;

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];

            if (line.toLowerCase() === '.code') {
                inCodeSection = true;
                inDataSection = false;
                continue;
            } else if (line.toLowerCase() === '.endcode') {
                inCodeSection = false;
                continue;
            } else if (line.toLowerCase() === '.data') {
                inDataSection = true;
                inCodeSection = false;
                continue;
            } else if (line.toLowerCase() === '.enddata') {
                inDataSection = false;
                continue;
            }

            if (inCodeSection) {
                // Verificar se há um label na linha
                if (line.includes(':')) {
                    const [label, instruction] = line.split(':').map(part => part.trim());
                    this.labels[label] = lineCounter;

                    if (instruction) {
                        this.code.push(instruction);
                        lineCounter++;
                    }
                } else {
                    this.code.push(line);
                    lineCounter++;
                }
            } else if (inDataSection) {
                const parts = line.split(/\s+/);
                if (parts.length >= 2) {
                    const varName = parts[0];
                    const varValue = parseInt(parts[1]);
                    this.data[varName] = varValue;
                }
            }
        }

        this.running = true;
        return true;
    }

    setSystemCallHandler(handler) {
        this.systemCallHandler = handler;
    }

    resolveOperand(operand) {
        if (operand.startsWith('#')) {
            // Modo de endereçamento imediato
            return parseInt(operand.substring(1));
        } else {
            // Modo de endereçamento direto
            if (this.data.hasOwnProperty(operand)) {
                return this.data[operand];
            } else {
                throw new Error(`Variável ${operand} não encontrada`);
            }
        }
    }

    executeInstruction() {
        if (!this.running || this.blocked || this.waitingForInput) {
            return false;
        }

        if (this.pc >= this.code.length) {
            this.running = false;
            return false;
        }

        const instruction = this.code[this.pc];
        const parts = instruction.split(/\s+/);
        const opcode = parts[0].toUpperCase();
        const operand = parts.length > 1 ? parts[1] : null;

        switch (opcode) {
            case 'ADD':
                this.acc += this.resolveOperand(operand);
                this.pc++;
                break;
            case 'SUB':
                this.acc -= this.resolveOperand(operand);
                this.pc++;
                break;
            case 'MULT':
                this.acc *= this.resolveOperand(operand);
                this.pc++;
                break;
            case 'DIV':
                const divisor = this.resolveOperand(operand);
                if (divisor === 0) {
                    throw new Error('Divisão por zero');
                }
                this.acc = Math.floor(this.acc / divisor);
                this.pc++;
                break;
            case 'LOAD':
                this.acc = this.resolveOperand(operand);
                this.pc++;
                break;
            case 'STORE':
                if (operand.startsWith('#')) {
                    throw new Error('STORE não pode usar endereçamento imediato');
                }
                this.data[operand] = this.acc;
                this.pc++;
                break;
            case 'BRANY':
                if (this.labels.hasOwnProperty(operand)) {
                    this.pc = this.labels[operand];
                } else {
                    throw new Error(`Label ${operand} não encontrado`);
                }
                break;
            case 'BRPOS':
                if (this.acc > 0) {
                    if (this.labels.hasOwnProperty(operand)) {
                        this.pc = this.labels[operand];
                    } else {
                        throw new Error(`Label ${operand} não encontrado`);
                    }
                } else {
                    this.pc++;
                }
                break;
            case 'BRZERO':
                if (this.acc === 0) {
                    if (this.labels.hasOwnProperty(operand)) {
                        this.pc = this.labels[operand];
                    } else {
                        throw new Error(`Label ${operand} não encontrado`);
                    }
                } else {
                    this.pc++;
                }
                break;
            case 'BRNEG':
                if (this.acc < 0) {
                    if (this.labels.hasOwnProperty(operand)) {
                        this.pc = this.labels[operand];
                    } else {
                        throw new Error(`Label ${operand} não encontrado`);
                    }
                } else {
                    this.pc++;
                }
                break;
            case 'SYSCALL':
                const index = parseInt(operand);
                if (this.systemCallHandler) {
                    this.systemCallHandler(index, this);
                }
                this.pc++;
                break;
            default:
                throw new Error(`Instrução desconhecida: ${opcode}`);
        }

        return true;
    }

    handleInput(value) {
        if (this.waitingForInput) {
            this.acc = parseInt(value);
            this.waitingForInput = false;

            // Verificar a condição especial para imprimir "Ola"
            if (value === "0101") {
                console.log("Ola");
                if (this.systemCallHandler) {
                    this.systemCallHandler(-1, this); // código especial para "Ola"
                }
            }

            return true;
        }
        return false;
    }
}

