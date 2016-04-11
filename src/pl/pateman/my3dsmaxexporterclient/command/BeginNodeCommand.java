package pl.pateman.my3dsmaxexporterclient.command;

import pl.pateman.my3dsmaxexporterclient.ClientCommand;
import pl.pateman.my3dsmaxexporterclient.CommandContext;
import pl.pateman.skeletal.mesh.Mesh;

/**
 * Created by pateman.
 */
public final class BeginNodeCommand implements ClientCommand {
    @Override
    public void execute(final CommandContext context) throws Exception {
        context.mesh = new Mesh();
    }
}
