package info.smart_tools.smartactors.core.exception_handling_receivers;

import info.smart_tools.smartactors.core.ifield_name.IFieldName;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.message_processing.IMessageReceiver;

/**
 * Base class for implementations of {@link IMessageReceiver} that may be used as parts of exceptional chains to handle
 * exceptions.
 *
 * @see info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence#catchException(Throwable, IObject)
 */
public abstract class ExceptionHandlingReceiver implements IMessageReceiver {
    private final IFieldName causeLevelFieldName;
    private final IFieldName causeStepFieldName;
    private final IFieldName catchLevelFieldName;
    private final IFieldName catchStepFieldName;
    private final IFieldName exceptionFieldName;

    /**
     * The constructor.
     *
     * @throws ResolutionException if fails to resolve any dependencies
     */
    protected ExceptionHandlingReceiver() throws ResolutionException {
        causeLevelFieldName = IOC.resolve(IOC.resolve(IOC.getKeyForKeyStorage(), IFieldName.class.toString()), "causeLevel");
        causeStepFieldName = IOC.resolve(IOC.resolve(IOC.getKeyForKeyStorage(), IFieldName.class.toString()), "causeStep");
        catchLevelFieldName = IOC.resolve(IOC.resolve(IOC.getKeyForKeyStorage(), IFieldName.class.toString()), "catchLevel");
        catchStepFieldName = IOC.resolve(IOC.resolve(IOC.getKeyForKeyStorage(), IFieldName.class.toString()), "catchStep");
        exceptionFieldName = IOC.resolve(IOC.resolve(IOC.getKeyForKeyStorage(), IFieldName.class.toString()), "exception");
    }

    /**
     * Get a {@code "causeLevel"} value from message context.
     *
     * @param context    the message context
     * @return the value from context
     * @throws ReadValueException if error occurs reading value from context
     * @throws InvalidArgumentException if incoming argument is null
     * @see info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence#catchException(Throwable, IObject)
     */
    protected int getCauseLevel(final IObject context) throws ReadValueException, InvalidArgumentException {
        return (Integer) context.getValue(causeLevelFieldName);
    }

    /**
     * Get a {@code "causeStep"} value from message context.
     *
     * @param context    the message context
     * @return the value from context
     * @throws ReadValueException if error occurs reading value from context
     * @throws InvalidArgumentException if incoming argument is null
     * @see info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence#catchException(Throwable, IObject)
     */
    protected int getCauseStep(final IObject context) throws ReadValueException, InvalidArgumentException {
        return (Integer) context.getValue(causeStepFieldName);
    }

    /**
     * Get a {@code "catchLevel"} value from message context.
     *
     * @param context    the message context
     * @return the value from context
     * @throws ReadValueException if error occurs reading value from context
     * @throws InvalidArgumentException if incoming argument is null
     * @see info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence#catchException(Throwable, IObject)
     */
    protected int getCatchLevel(final IObject context) throws ReadValueException, InvalidArgumentException {
        return (Integer) context.getValue(catchLevelFieldName);
    }

    /**
     * Get a {@code "catchStep"} value from message context.
     *
     * @param context    the message context
     * @return the value from context
     * @throws ReadValueException if error occurs reading value from context
     * @throws InvalidArgumentException if incoming argument is null
     * @see info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence#catchException(Throwable, IObject)
     */
    protected int getCatchStep(final IObject context)
        throws ReadValueException, InvalidArgumentException {
        return (Integer) context.getValue(catchStepFieldName);
    }

    /**
     * Get exception saved in context by a message processing sequence.
     *
     * @param context    the message context
     * @return exception saved in message context
     * @throws ReadValueException if error occurs reading value from context
     * @throws InvalidArgumentException if incoming argument is null
     * @see info.smart_tools.smartactors.core.message_processing.IMessageProcessingSequence#catchException(Throwable, IObject)
     */
    protected Throwable getException(final IObject context)
        throws ReadValueException, InvalidArgumentException {
        return (Throwable) context.getValue(exceptionFieldName);
    }
}
