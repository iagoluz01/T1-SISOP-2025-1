import java.util.Map;

public class Process {
    private int id;
    private String name;
    private String code;
    private int arrivalTime;
    private String type;  // 'realtime' ou 'besteffort'
    private int priority; // 0 (alta) ou 1 (baixa) para tempo real
    private String state; // new, ready, running, blocked, exit
    private AssemblyInterpreter interpreter;
    private int blockEndTime;
    private int quantumLeft;
    private int executionTime; // Tempo total de execução
    private int waitingTime;   // Tempo total de espera

    public Process(int id, String name, String code, int arrivalTime, String type, int priority) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.arrivalTime = arrivalTime;
        this.type = type;
        this.priority = priority;
        this.state = "new";
        this.interpreter = new AssemblyInterpreter();
        this.interpreter.parseProgram(code);
        this.blockEndTime = 0;
        this.quantumLeft = 0;
        this.executionTime = 0;
        this.waitingTime = 0;
    }

    public Map<String, Object> saveState() {
        Map<String, Object> state = new java.util.HashMap<>();
        state.put("acc", interpreter.getAcc());
        state.put("pc", interpreter.getPc());
        state.put("data", interpreter.getData());
        state.put("running", interpreter.isRunning());
        state.put("blocked", interpreter.isBlocked());
        state.put("blockTime", interpreter.getBlockTime());
        state.put("waitingForInput", interpreter.isWaitingForInput());
        return state;
    }

    @SuppressWarnings("unchecked")
    public void restoreState(Map<String, Object> state) {
        interpreter.setAcc((Integer) state.get("acc"));
        interpreter.setPc((Integer) state.get("pc"));
        interpreter.setData((Map<String, Integer>) state.get("data"));
        interpreter.setRunning((Boolean) state.get("running"));
        interpreter.setBlocked((Boolean) state.get("blocked"));
        interpreter.setBlockTime((Integer) state.get("blockTime"));
        interpreter.setWaitingForInput((Boolean) state.get("waitingForInput"));
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public String getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public AssemblyInterpreter getInterpreter() {
        return interpreter;
    }

    public int getBlockEndTime() {
        return blockEndTime;
    }

    public void setBlockEndTime(int blockEndTime) {
        this.blockEndTime = blockEndTime;
    }

    public int getQuantumLeft() {
        return quantumLeft;
    }

    public void setQuantumLeft(int quantumLeft) {
        this.quantumLeft = quantumLeft;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void incrementExecutionTime() {
        this.executionTime++;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void incrementWaitingTime() {
        this.waitingTime++;
    }

    public void decrementQuantumLeft() {
        this.quantumLeft--;
    }
}

