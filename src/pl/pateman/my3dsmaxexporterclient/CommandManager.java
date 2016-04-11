package pl.pateman.my3dsmaxexporterclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pateman.
 */
public final class CommandManager {
    private final Map<String, ClientCommand> commands;

    public CommandManager() {
        this.commands = new HashMap<>();
    }

    public void registerCommandHandler(final String commandName, final ClientCommand command) {
        this.commands.put(commandName, command);
    }

    public void registerCommandHandler(final ClientCommand command, final String... commandNames) {
        for (final String commandName : commandNames) {
            this.registerCommandHandler(commandName, command);
        }
    }

    public ClientCommand getCommandHandler(final String commandName) {
        return this.commands.get(commandName);
    }
}
