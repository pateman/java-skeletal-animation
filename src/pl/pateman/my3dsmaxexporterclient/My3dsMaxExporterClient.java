package pl.pateman.my3dsmaxexporterclient;

import com.google.gson.Gson;
import pl.pateman.my3dsmaxexporterclient.command.BeginNodeCommand;
import pl.pateman.my3dsmaxexporterclient.command.FinishNodeCommand;
import pl.pateman.my3dsmaxexporterclient.command.NodeDataCommand;
import pl.pateman.my3dsmaxexporterclient.command.NodeGeometryDataCommand;

import java.io.PrintWriter;
import java.io.RandomAccessFile;

/**
 * Created by pateman.
 */
public class My3dsMaxExporterClient {
    private static final CommandManager commandManager = new CommandManager();

    static {
        commandManager.registerCommandHandler("BEGIN_NODE", new BeginNodeCommand());
        commandManager.registerCommandHandler(new NodeDataCommand(), "NAME", "INDEX", "PARENT", "TYPE");
        commandManager.registerCommandHandler(new NodeGeometryDataCommand(), "VERTEX", "NORMAL", "FACE", "TEXCOORD");
        commandManager.registerCommandHandler("FINISH_NODE", new FinishNodeCommand());
    }

    public static void main(String[] args) {
        try (final RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\" + args[0], "r")) {
            //  Create an instance of the context.
            final CommandContext commandContext = new CommandContext();
            commandContext.outputFile = args[1];

            while (true) {
                //  Read a command from the pipe.
                final String commandLine = pipe.readLine();

                //  Split the command by whitespace. The first part contains the name of the command. Before we look up
                //  the command though, check if the 3dsmax plugin hasn't issued the "END" command.
                final String[] split = commandLine.split(" ");

                if (split[0].equals("END")) {
                    //  If we're ending, serialize the context's node information to JSON and bail out.
                    final Gson gson = new Gson();
                    final String json = gson.toJson(commandContext.nodes);

                    try (PrintWriter writer = new PrintWriter(commandContext.outputFile)) {
                        writer.print(json);
                    }
                    break;
                }

                final ClientCommand clientCommand = commandManager.getCommandHandler(split[0]);
                if (clientCommand == null) {
                    System.out.printf("Unrecognized command name '%s'\n", split[0]);
                    continue;
                }

                //  Pass parameters to the context if there are any.
                commandContext.commandParameters = split;

                //  Execute the command.
                clientCommand.execute(commandContext);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new java.util.Scanner(System.in).nextLine();
        }
    }
}
