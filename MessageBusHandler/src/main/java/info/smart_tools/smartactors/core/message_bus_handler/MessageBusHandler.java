package info.smart_tools.smartactors.core.message_bus_handler;

import info.smart_tools.smartactors.ioc.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.imessage_bus_handler.IMessageBusHandler;
import info.smart_tools.smartactors.core.imessage_bus_handler.exception.MessageBusHandlerException;
import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.iobject.iobject.exception.ChangeValueException;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.core.iqueue.IQueue;
import info.smart_tools.smartactors.core.itask.ITask;
import info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence;
import info.smart_tools.smartactors.core.message_processing.IMessageProcessor;
import info.smart_tools.smartactors.core.message_processing.IReceiverChain;

/**
 *
 */
public class MessageBusHandler implements IMessageBusHandler {

    private final IQueue<ITask> taskQueue;
    private final int stackDepth;
    private final IReceiverChain chain;

    /**
     *
     * @param taskQueue Queue of the tasks
     * @param stackDepth Stack depth of the {@link IMessageProcessor}
     * @param receiverChain the chain for processing incoming message
     * @throws InvalidArgumentException if there is invalid arguments
     */
    public MessageBusHandler(final IQueue<ITask> taskQueue, final int stackDepth, final IReceiverChain receiverChain)
            throws InvalidArgumentException {
        if (null == taskQueue) {
            throw new InvalidArgumentException("Task queue should not be null.");
        }
        if (stackDepth < 0) {
            throw new InvalidArgumentException("Stack depth should be positive number.");
        }
        if (null == receiverChain) {
            throw new InvalidArgumentException("ReceiverChain should not be null.");
        }
        this.stackDepth = stackDepth;
        this.taskQueue = taskQueue;
        this.chain = receiverChain;
    }

    @Override
    public void handle(final IObject message)
            throws MessageBusHandlerException {
        try {
            IMessageProcessingSequence processingSequence =
                    IOC.resolve(
                            IOC.resolve(IOC.getKeyForKeyStorage(), IMessageProcessingSequence.class.getCanonicalName()),
                            this.stackDepth,
                            this.chain
                    );
            IMessageProcessor messageProcessor =
                    IOC.resolve(
                            IOC.resolve(IOC.getKeyForKeyStorage(), IMessageProcessor.class.getCanonicalName()),
                            this.taskQueue,
                            processingSequence
                    );
            IObject context = IOC.resolve(
                    IOC.resolve(IOC.getKeyForKeyStorage(), IObject.class.getCanonicalName())
            );
            // TODO: in future need to put sender id to the context
            messageProcessor.process(message, context);
        } catch (ResolutionException | InvalidArgumentException | ChangeValueException e) {
            throw new MessageBusHandlerException("Failed to handle message to MessageBus.", e);
        }
    }
}
