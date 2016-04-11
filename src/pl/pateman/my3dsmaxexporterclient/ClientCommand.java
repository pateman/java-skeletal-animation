package pl.pateman.my3dsmaxexporterclient;

/**
 * Created by pateman.
 */
public interface ClientCommand {
    void execute(final CommandContext context) throws Exception;
}
