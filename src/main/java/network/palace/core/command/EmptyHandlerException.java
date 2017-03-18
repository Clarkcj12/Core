package network.palace.core.command;

/**
 * The type Empty handler exception.
 */
public final class EmptyHandlerException extends CommandException implements FriendlyException {

    /**
     * Instantiates a new Empty handler exception.
     */
    public EmptyHandlerException() {
        super("command.error.invalid.handler");
    }

    @Override
    public String getFriendlyMessage() {
        return getMessage();
    }
}
