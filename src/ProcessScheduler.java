import java.util.Comparator;
import java.util.List;

public class ProcessScheduler {
    private ProcessManager processManager;
    private int quantum;
    private Process currentProcess;

    public ProcessScheduler(ProcessManager processManager) {
        this.processManager = processManager;
        this.quantum = 2; // Quantum padrão
        this.currentProcess = null;
    }

    public void setQuantum(int quantum) {
        this.quantum = Math.max(1, quantum);
    }

    public Process schedule() {
        // Verificar se há algum processo em execução
        Process runningProcess = processManager.getRunningProcess();

        // Se houver processo em execução
        if (runningProcess != null) {
            // Verificar se o quantum acabou
            if (runningProcess.getQuantumLeft() <= 0 && runningProcess.getType().equals("realtime")) {
                runningProcess.setState("ready");
                currentProcess = null;
            }
            // Se o processo estiver esperando input, não fazemos nada
            else if (runningProcess.getInterpreter().isWaitingForInput()) {
                return runningProcess;
            }
            // Se o processo terminou ou foi bloqueado, já foi tratado pelo sistema
            else if (runningProcess.getState().equals("exit") || runningProcess.getState().equals("blocked")) {
                currentProcess = null;
            }
        }

        // Se não há processo em execução, seleciona o próximo
        if (currentProcess == null) {
            // Primeiro, verifica se há processos de tempo real prontos
            List<Process> realtimeProcesses = processManager.getReadyProcesses().stream()
                    .filter(p -> p.getType().equals("realtime"))
                    .sorted(Comparator
                            .comparing(Process::getPriority)
                            .thenComparing(Process::getArrivalTime))
                    .toList();

            // Se houver processos de tempo real prontos
            if (!realtimeProcesses.isEmpty()) {
                currentProcess = realtimeProcesses.get(0);
                currentProcess.setState("running");
                currentProcess.setQuantumLeft(quantum);
            } else {
                // Senão, verificar se há processos de melhor esforço prontos (FCFS)
                List<Process> besteffortProcesses = processManager.getReadyProcesses().stream()
                        .filter(p -> p.getType().equals("besteffort"))
                        .sorted(Comparator.comparing(Process::getArrivalTime))
                        .toList();

                if (!besteffortProcesses.isEmpty()) {
                    currentProcess = besteffortProcesses.get(0);
                    currentProcess.setState("running");
                    // Processos de melhor esforço não têm quantum limitado
                    currentProcess.setQuantumLeft(Integer.MAX_VALUE);
                }
            }
        }

        // Verificar se um processo de tempo real ficou pronto enquanto um de melhor esforço está executando
        if (currentProcess != null && currentProcess.getType().equals("besteffort")) {
            boolean realtimeReady = processManager.getReadyProcesses().stream()
                    .anyMatch(p -> p.getType().equals("realtime"));

            if (realtimeReady) {
                // Preempção: interrompe o processo de melhor esforço
                currentProcess.setState("ready");
                currentProcess = null;
                // O próximo ciclo de escalonamento selecionará o processo de tempo real
                return schedule();
            }
        }

        return currentProcess;
    }

    public boolean executeCurrentProcess() {
        if (currentProcess == null || !currentProcess.getState().equals("running")) {
            return false;
        }

        try {
            return currentProcess.getInterpreter().executeInstruction();
        } catch (Exception error) {
            System.err.println("Erro na execução do processo " + currentProcess.getId() + ": " + error.getMessage());
            processManager.terminateProcess(currentProcess);
            return false;
        }
    }

    public Process getCurrentProcess() {
        return currentProcess;
    }
}
