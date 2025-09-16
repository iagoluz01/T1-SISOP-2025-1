import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblyInterpreter {
    private int acc;                // Acumulador
    private int pc;                 // Program counter
    private List<String> code;      // Instruções
    private Map<String, Integer> data;     // Área de dados
    private Map<String, Integer> labels;   // Mapeamento de labels para posições no código
    private boolean running;
    private boolean blocked;
    private int blockTime;
    private boolean waitingForInput;
    private SystemCallHandler systemCallHandler;

    public interface SystemCallHandler {
        void handle(int index, AssemblyInterpreter interpreter);
    }

    public AssemblyInterpreter() {
        reset();
    }

    public void reset() {
        this.acc = 0;
        this.pc = 0;
        this.code = new ArrayList<>();
        this.data = new HashMap<>();
        this.labels = new HashMap<>();
        this.running = false;
        this.blocked = false;
        this.blockTime = 0;
        this.waitingForInput = false;
        this.systemCallHandler = null;
    }

    public boolean parseProgram(String programText) {
        reset();

        String[] linesArray = programText.split("\n");
        List<String> lines = new ArrayList<>();

        // Process lines, trimming and filtering comments
        for (String line : linesArray) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                lines.add(line);
            }
        }

        boolean inCodeSection = false;
        boolean inDataSection = false;
        int lineCounter = 0;

        for (String line : lines) {
            if (line.equalsIgnoreCase(".code")) {
                inCodeSection = true;
                inDataSection = false;
                continue;
            } else if (line.equalsIgnoreCase(".endcode")) {
                inCodeSection = false;
                continue;
            } else if (line.equalsIgnoreCase(".data")) {
                inDataSection = true;
                inCodeSection = false;
                continue;
            } else if (line.equalsIgnoreCase(".enddata")) {
                inDataSection = false;
                continue;
            }

            if (inCodeSection) {
                // Verificar se há um label na linha
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String label = parts[0].trim();
                    String instruction = parts.length > 1 ? parts[1].trim() : "";

                    labels.put(label, lineCounter);

                    if (!instruction.isEmpty()) {
                        code.add(instruction);
                        lineCounter++;
                    }
                } else {
                    code.add(line);
                    lineCounter++;
                }
            } else if (inDataSection) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String varName = parts[0];
                    int varValue = Integer.parseInt(parts[1]);
                    data.put(varName, varValue);
                }
            }
        }

        running = true;
        return true;
    }

    public void setSystemCallHandler(SystemCallHandler handler) {
        this.systemCallHandler = handler;
    }

    private int resolveOperand(String operand) {
        if (operand.startsWith("#")) {
            // Modo de endereçamento imediato
            return Integer.parseInt(operand.substring(1));
        } else {
            // Modo de endereçamento direto
            if (data.containsKey(operand)) {
                return data.get(operand);
            } else {
                throw new RuntimeException("Variável " + operand + " não encontrada");
            }
        }
    }

    public boolean executeInstruction() {
        if (!running || blocked || waitingForInput) {
            return false;
        }

        if (pc >= code.size()) {
            running = false;
            return false;
        }

        String instruction = code.get(pc);
        String[] parts = instruction.split("\\s+");
        String opcode = parts[0].toUpperCase();
        String operand = parts.length > 1 ? parts[1] : null;

        switch (opcode) {
            case "ADD":
                acc += resolveOperand(operand);
                pc++;
                break;
            case "SUB":
                acc -= resolveOperand(operand);
                pc++;
                break;
            case "MULT":
                acc *= resolveOperand(operand);
                pc++;
                break;
            case "DIV":
                int divisor = resolveOperand(operand);
                if (divisor == 0) {
                    throw new RuntimeException("Divisão por zero");
                }
                acc = acc / divisor;
                pc++;
                break;
            case "LOAD":
                acc = resolveOperand(operand);
                pc++;
                break;
            case "STORE":
                if (operand.startsWith("#")) {
                    throw new RuntimeException("STORE não pode usar endereçamento imediato");
                }
                data.put(operand, acc);
                pc++;
                break;
            case "BRANY":
                if (labels.containsKey(operand)) {
                    pc = labels.get(operand);
                } else {
                    throw new RuntimeException("Label " + operand + " não encontrado");
                }
                break;
            case "BRPOS":
                if (acc > 0) {
                    if (labels.containsKey(operand)) {
                        pc = labels.get(operand);
                    } else {
                        throw new RuntimeException("Label " + operand + " não encontrado");
                    }
                } else {
                    pc++;
                }
                break;
            case "BRZERO":
                if (acc == 0) {
                    if (labels.containsKey(operand)) {
                        pc = labels.get(operand);
                    } else {
                        throw new RuntimeException("Label " + operand + " não encontrado");
                    }
                } else {
                    pc++;
                }
                break;
            case "BRNEG":
                if (acc < 0) {
                    if (labels.containsKey(operand)) {
                        pc = labels.get(operand);
                    } else {
                        throw new RuntimeException("Label " + operand + " não encontrado");
                    }
                } else {
                    pc++;
                }
                break;
            case "SYSCALL":
                int index = Integer.parseInt(operand);
                if (systemCallHandler != null) {
                    systemCallHandler.handle(index, this);
                }
                pc++;
                break;
            default:
                throw new RuntimeException("Instrução desconhecida: " + opcode);
        }

        return true;
    }

    public boolean handleInput(String value) {
        if (waitingForInput) {
            acc = Integer.parseInt(value);
            waitingForInput = false;

            // Verificar a condição especial para imprimir "Ola"
            if (value.equals("0101")) {
                System.out.println("Ola");
                if (systemCallHandler != null) {
                    systemCallHandler.handle(-1, this); // código especial para "Ola"
                }
            }

            return true;
        }
        return false;
    }

    // Getters and setters
    public int getAcc() {
        return acc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public Map<String, Integer> getData() {
        return new HashMap<>(data);
    }

    public void setData(Map<String, Integer> data) {
        this.data = new HashMap<>(data);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public int getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(int blockTime) {
        this.blockTime = blockTime;
    }

    public boolean isWaitingForInput() {
        return waitingForInput;
    }

    public void setWaitingForInput(boolean waitingForInput) {
        this.waitingForInput = waitingForInput;
    }
}

