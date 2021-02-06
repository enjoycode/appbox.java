package appbox.design.services.debug;

import appbox.channel.IClientMessage;
import appbox.design.IDeveloperSession;
import appbox.design.services.debug.variables.Variable;
import appbox.design.services.debug.variables.VariableFormatter;
import appbox.serialization.IOutputStream;

import java.util.List;

final class DebugPauseEvent implements IClientMessage {

    private final long           threadId;
    private final int            line;
    private final List<Variable> variables;

    public DebugPauseEvent(long threadId, int line, List<Variable> variables) {
        this.threadId  = threadId;
        this.line      = line;
        this.variables = variables;
    }

    @Override
    public void writeTo(IOutputStream bs) {
        bs.writeInt(IDeveloperSession.DEBUG_EVENT);
        bs.writeByte(DebugEventType.HIT_BREAKPOINT); //TODO: fix

        bs.writeLong(threadId);
        bs.writeInt(line);

        if (variables == null || variables.size() == 0) {
            bs.writeVariant(0);
        } else {
            bs.writeVariant(variables.size());
            var formatter = VariableFormatter.DEFAULT;
            var options   = formatter.getDefaultOptions();
            for (var v : variables) {
                bs.writeString(v.name);
                bs.writeString(formatter.valueToString(v.value, options));
                bs.writeString(formatter.typeToString(v.value == null ? null : v.value.type(), options));
            }
        }
    }

}
