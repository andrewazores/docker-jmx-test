package es.andrewazor.containertest.commands;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import es.andrewazor.containertest.commands.Command;
import es.andrewazor.containertest.commands.CommandRegistry;

@Singleton
public class CommandRegistry {

    private final Map<String, Command> commandMap = new TreeMap<>();

    CommandRegistry(Set<Command> commands) {
        for (Command command : commands) {
            String commandName = command.getName();
            if (commandMap.containsKey(commandName)) {
                throw new CommandDefinitionException(commandName, command.getClass(), commandMap.get(commandName).getClass());
            }
            commandMap.put(commandName, command);
        }
    }

    public Set<String> getRegisteredCommandNames() {
        return this.commandMap.keySet();
    }

    public Set<String> getAvailableCommandNames() {
        return this.commandMap
            .values()
            .stream()
            .filter(Command::isAvailable)
            .map(Command::getName)
            .collect(Collectors.toSet())
            ;
    }

    public void execute(String commandName, String[] args) throws Exception {
        if (!isCommandRegistered(commandName) || !isCommandAvailable(commandName)) {
            return;
        }
        commandMap.get(commandName).execute(args);
    }

    public boolean validate(String commandName, String[] args) throws Exception {
        return isCommandRegistered(commandName) && commandMap.get(commandName).validate(args);
    }

    private boolean isCommandRegistered(String commandName) {
        boolean registered = getRegisteredCommandNames().contains(commandName);
        if (!registered) {
            System.out.println(String.format("Command \"%s\" not recognized", commandName));
        }
        return registered;
    }

    private boolean isCommandAvailable(String commandName) {
        boolean available = getAvailableCommandNames().contains(commandName);
        if (!available) {
            System.out.println(String.format("Command \"%s\" not available", commandName));
        }
        return available;
    }

    public static class CommandDefinitionException extends RuntimeException {
        public CommandDefinitionException(String commandName, Class<? extends Command> cmd1, Class<? extends Command> cmd2) {
            super(String.format("\"%s\" command definitions provided by class %s AND class %s",
                        commandName, cmd1.getCanonicalName(), cmd2.getCanonicalName()));
        }
    }
}
