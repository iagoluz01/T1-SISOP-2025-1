import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessManager {
    private List<Process> processes;
    private int nextProcessId;
    private int currentTime;

    public ProcessManager() {
        this.processes = new ArrayList<>();
        this.nextProcessId = 1;
        this.currentTime = 0;
    }

    public Process createProcess(String name, String code, int arrivalTime, String type, int priority) {
        Process process = new Process(nextProcessId++, name, code, arrivalTime, type, priority);
        processes.add(process);
        return process;
    }

    public List<Process> getProcessesByState(String state) {
        return processes.stream()
                .filter(p -> p.getState().equals(state))
                .collect(Collectors.toList());
    }

    public List<Process> getReadyProcesses() {
        return processes.stream()
                .filter(p -> p.getState().equals("ready"))
                .collect(Collectors.toList());
    }

    public Process getRunningProcess() {
        return processes.stream()
                .filter(p -> p.getState().equals("running"))
                .findFirst()
                .orElse(null);
    }

    public List<Process> getBlockedProcesses() {
        return processes.stream()
                .filter(p -> p.getState().equals("blocked"))
                .collect(Collectors.toList());
    }

    public List<Process> getFinishedProcesses() {
        return processes.stream()
                .filter(p -> p.getState().equals("exit"))
                .collect(Collectors.toList());
    }

    public Process getProcessById(int id) {
        return processes.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public void advanceTime() {
        currentTime++;

        // Verificar processos que chegaram
        processes.forEach(process -> {
            if (process.getState().equals("new") && process.getArrivalTime() <= currentTime) {
                process.setState("ready");
            }
        });

        // Verificar processos bloqueados
        getBlockedProcesses().forEach(process -> {
            if (process.getBlockEndTime() <= currentTime) {
                process.setState("ready");
                process.getInterpreter().setBlocked(false);
            }
        });

        // Atualizar tempo de espera para processos prontos
        getReadyProcesses().forEach(Process::incrementWaitingTime);

        // Atualizar tempo de execução para o processo em execução
        Process runningProcess = getRunningProcess();
        if (runningProcess != null) {
            runningProcess.incrementExecutionTime();
            runningProcess.decrementQuantumLeft();
        }
    }

    public void blockProcess(Process process, int duration) {
        if (process != null && process.getState().equals("running")) {
            process.setState("blocked");
            process.getInterpreter().setBlocked(true);
            process.setBlockEndTime(currentTime + duration);
        }
    }

    public void terminateProcess(Process process) {
        if (process != null) {
            process.setState("exit");
            process.getInterpreter().setRunning(false);
        }
    }

    public void reset() {
        processes.clear();
        nextProcessId = 1;
        currentTime = 0;
    }

    // Getters
    public int getCurrentTime() {
        return currentTime;
    }

    public int getNextProcessId() {
        return nextProcessId;
    }
}

