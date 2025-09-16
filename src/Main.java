import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

public class Main {
    private static ProcessManager processManager;
    private static ProcessScheduler scheduler;
    private static Scanner scanner;
    private static boolean simulationRunning = false;
    private static Timer simulationTimer;
    private static AtomicBoolean waitingForInput = new AtomicBoolean(false);
    private static Process waitingProcess = null;

    public static void main(String[] args) {
        processManager = new ProcessManager();
        scheduler = new ProcessScheduler(processManager);
        scanner = new Scanner(System.in);

        System.out.println("===== Simulador de Escalonamento de Processos =====");

        boolean exit = false;
        while (!exit) {
            displayMenu();
            int option = readIntInput("Escolha uma opção: ");

            switch (option) {
                case 1 -> configureQuantum();
                case 2 -> addProcess();
                case 3 -> startSimulation();
                case 4 -> pauseSimulation();
                case 5 -> simulationStep();
                case 6 -> resetSimulation();
                case 7 -> displayProcesses();
                case 8 -> exit = true;
                default -> System.out.println("Opção inválida!");
            }
        }

        if (simulationTimer != null) {
            simulationTimer.cancel();
        }
        scanner.close();
        System.out.println("Simulador encerrado!");
    }

    private static void displayMenu() {
        System.out.println("\n----- Menu -----");
        System.out.println("1. Configurar quantum");
        System.out.println("2. Adicionar processo");
        System.out.println("3. Iniciar simulação");
        System.out.println("4. Pausar simulação");
        System.out.println("5. Avançar um passo");
        System.out.println("6. Reiniciar simulação");
        System.out.println("7. Exibir processos");
        System.out.println("8. Sair");
        System.out.println("----------------");
        System.out.println("Tempo atual: " + processManager.getCurrentTime());
    }

    private static void configureQuantum() {
        int quantum = readIntInput("Digite o valor do quantum para processos de tempo real: ");
        scheduler.setQuantum(quantum);
        System.out.println("Quantum configurado para: " + quantum);
    }

    private static void addProcess() {
        try {
            System.out.print("Nome do processo (ou vazio para nome padrão): ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                name = "Processo " + processManager.getNextProcessId();
            }

            int arrivalTime = readIntInput("Tempo de chegada: ");

            System.out.println("Tipo do processo:");
            System.out.println("1. Tempo real (realtime)");
            System.out.println("2. Melhor esforço (besteffort)");
            int typeOption = readIntInput("Escolha o tipo: ");
            String type = (typeOption == 1) ? "realtime" : "besteffort";

            int priority = 1;
            if (type.equals("realtime")) {
                System.out.println("Prioridade para processo de tempo real:");
                System.out.println("1. Alta (0)");
                System.out.println("2. Baixa (1)");
                int priorityOption = readIntInput("Escolha a prioridade: ");
                priority = (priorityOption == 1) ? 0 : 1;
            }

            System.out.println("Digite o código assembly do processo (termine com 'END'):");
            StringBuilder codeBuilder = new StringBuilder();
            String line;
            while (!(line = scanner.nextLine()).equalsIgnoreCase("END")) {
                codeBuilder.append(line).append("\n");
            }
            String code = codeBuilder.toString().trim();

            if (code.isEmpty()) {
                System.out.println("O código não pode estar vazio!");
                return;
            }

            Process process = processManager.createProcess(name, code, arrivalTime, type, priority);
            configureSystemCallHandler(process);

            System.out.println("Processo \"" + name + "\" adicionado com sucesso!");
        } catch (NumberFormatException e) {
            System.out.println("Erro: Entrada numérica inválida.");
        } catch (Exception e) {
            System.out.println("Erro ao adicionar processo: " + e.getMessage());
        }
    }

    private static void configureSystemCallHandler(Process process) {
        process.getInterpreter().setSystemCallHandler((index, interpreter) -> {
            switch (index) {
                case -1: // Special code for "Ola"
                    System.out.println("[" + processManager.getCurrentTime() + "] Ola");
                    break;
                case 0: // Terminate process
                    processManager.terminateProcess(process);
                    System.out.println("[" + processManager.getCurrentTime() + "] Processo " +
                        process.getName() + " terminado.");
                    break;
                case 1: // Print value
                    System.out.println("[" + processManager.getCurrentTime() + "] " +
                        process.getName() + " imprime: " + interpreter.getAcc());
                    // Block for random time between 3 and 5
                    int printBlockTime = (int)(Math.random() * 3) + 3;
                    processManager.blockProcess(process, printBlockTime);
                    System.out.println("[" + processManager.getCurrentTime() + "] " +
                        process.getName() + " bloqueado por " + printBlockTime + " unidades de tempo.");
                    break;
                case 2: // Read value
                    System.out.println("[" + processManager.getCurrentTime() + "] " +
                        process.getName() + " aguarda entrada...");
                    interpreter.setWaitingForInput(true);
                    waitingForInput.set(true);
                    waitingProcess = process;
                    break;
            }
        });
    }

    private static void startSimulation() {
        if (simulationRunning) {
            System.out.println("A simulação já está em execução!");
            return;
        }

        simulationRunning = true;
        System.out.println("Simulação iniciada!");

        simulationTimer = new Timer();
        simulationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (waitingForInput.get()) {
                    // Se estiver esperando input, verificamos se o usuário forneceu
                    try {
                        if (System.in.available() > 0) {
                            handleProcessInput();
                        }
                    } catch (IOException e) {
                        System.out.println("Erro de I/O ao verificar entrada: " + e.getMessage());
                    }
                } else {
                    simulationStep();
                }
            }
        }, 0, 1000);
    }

    private static void pauseSimulation() {
        if (!simulationRunning) {
            System.out.println("A simulação não está em execução!");
            return;
        }

        if (simulationTimer != null) {
            simulationTimer.cancel();
            simulationTimer = null;
        }
        simulationRunning = false;
        System.out.println("Simulação pausada!");
    }

    private static void simulationStep() {
        if (waitingForInput.get()) {
            System.out.println("Um processo está aguardando entrada. Forneça um valor:");
            handleProcessInput();
            return;
        }

        processManager.advanceTime();
        scheduler.schedule();
        boolean executed = scheduler.executeCurrentProcess();

        System.out.println("Tempo atual: " + processManager.getCurrentTime());
        Process runningProcess = processManager.getRunningProcess();
        if (runningProcess != null) {
            System.out.println("Processo em execução: " + runningProcess.getName() +
                " (ID: " + runningProcess.getId() + ")");
        } else {
            System.out.println("Nenhum processo em execução");
        }

        // Verifica se um processo está aguardando entrada após a execução
        if (runningProcess != null && runningProcess.getInterpreter().isWaitingForInput()) {
            waitingForInput.set(true);
            waitingProcess = runningProcess;
            System.out.println("Digite um valor para o processo " + runningProcess.getName() + ":");
        }
    }

    private static void handleProcessInput() {
        try {
            if (waitingProcess != null && waitingProcess.getInterpreter().isWaitingForInput()) {
                System.out.print("Digite um valor: ");
                String input = scanner.nextLine().trim();

                waitingProcess.getInterpreter().handleInput(input);
                System.out.println("[" + processManager.getCurrentTime() + "] Entrada recebida: " + input);

                // Special handling for "0101" input
                if (input.equals("0101")) {
                    System.out.println("[" + processManager.getCurrentTime() + "] Ola");
                }

                // Block for random time between 3 and 5
                int inputBlockTime = (int)(Math.random() * 3) + 3;
                processManager.blockProcess(waitingProcess, inputBlockTime);
                System.out.println("[" + processManager.getCurrentTime() + "] " +
                    waitingProcess.getName() + " bloqueado por " + inputBlockTime + " unidades de tempo.");

                waitingForInput.set(false);
                waitingProcess = null;
            } else {
                System.out.println("Nenhum processo está esperando entrada!");
                waitingForInput.set(false);
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar entrada: " + e.getMessage());
            waitingForInput.set(false);
        }
    }

    private static void resetSimulation() {
        if (simulationRunning) {
            pauseSimulation();
        }

        processManager.reset();
        waitingForInput.set(false);
        waitingProcess = null;
        System.out.println("Simulação reiniciada!");
    }

    private static void displayProcesses() {
        System.out.println("\n----- Estado dos Processos -----");

        // Processos em estado "ready"
        List<Process> readyRealtimeProcesses = processManager.getReadyProcesses().stream()
            .filter(p -> p.getType().equals("realtime"))
            .toList();

        System.out.println("Fila de Tempo Real (" + readyRealtimeProcesses.size() + "):");
        if (readyRealtimeProcesses.isEmpty()) {
            System.out.println("  Fila vazia");
        } else {
            for (Process p : readyRealtimeProcesses) {
                displayProcessInfo(p);
            }
        }

        List<Process> readyBesteffortProcesses = processManager.getReadyProcesses().stream()
            .filter(p -> p.getType().equals("besteffort"))
            .toList();

        System.out.println("Fila de Melhor Esforço (" + readyBesteffortProcesses.size() + "):");
        if (readyBesteffortProcesses.isEmpty()) {
            System.out.println("  Fila vazia");
        } else {
            for (Process p : readyBesteffortProcesses) {
                displayProcessInfo(p);
            }
        }

        // Processo em execução
        Process runningProcess = processManager.getRunningProcess();
        System.out.println("Processo em Execução:");
        if (runningProcess == null) {
            System.out.println("  Nenhum processo em execução");
        } else {
            displayProcessInfo(runningProcess);
            System.out.println("  Quantum restante: " + runningProcess.getQuantumLeft());
        }

        // Processos bloqueados
        List<Process> blockedProcesses = processManager.getBlockedProcesses();
        System.out.println("Processos Bloqueados (" + blockedProcesses.size() + "):");
        if (blockedProcesses.isEmpty()) {
            System.out.println("  Nenhum processo bloqueado");
        } else {
            for (Process p : blockedProcesses) {
                displayProcessInfo(p);
                System.out.println("  Desbloqueio em: " + p.getBlockEndTime());
            }
        }

        // Processos finalizados
        List<Process> finishedProcesses = processManager.getFinishedProcesses();
        System.out.println("Processos Finalizados (" + finishedProcesses.size() + "):");
        if (finishedProcesses.isEmpty()) {
            System.out.println("  Nenhum processo finalizado");
        } else {
            for (Process p : finishedProcesses) {
                displayProcessInfo(p);
            }
        }
    }

    private static void displayProcessInfo(Process process) {
        System.out.println("  " + process.getName() + " (ID: " + process.getId() + ")");
        System.out.println("    Tipo: " + (process.getType().equals("realtime") ? "Tempo Real" : "Melhor Esforço"));
        if (process.getType().equals("realtime")) {
            System.out.println("    Prioridade: " + (process.getPriority() == 0 ? "Alta (0)" : "Baixa (1)"));
        }
        System.out.println("    Chegada: " + process.getArrivalTime());
        System.out.println("    Tempo de execução: " + process.getExecutionTime());
        System.out.println("    Tempo de espera: " + process.getWaitingTime());
    }

    private static int readIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um número válido!");
            }
        }
    }
}
