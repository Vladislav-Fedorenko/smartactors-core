package info.smart_tools.smartactors.checkpoint.checkpoint_actor;

import info.smart_tools.smartactors.base.exception.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.checkpoint.checkpoint_actor.wrappers.EnteringMessage;
import info.smart_tools.smartactors.checkpoint.checkpoint_actor.wrappers.FeedbackMessage;
import info.smart_tools.smartactors.iobject.ifield_name.IFieldName;
import info.smart_tools.smartactors.iobject.iobject.IObject;
import info.smart_tools.smartactors.iobject.iobject.exception.ChangeValueException;
import info.smart_tools.smartactors.iobject.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.iobject.iobject.exception.SerializeException;
import info.smart_tools.smartactors.ioc.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.ioc.ioc.IOC;
import info.smart_tools.smartactors.ioc.named_keys_storage.Keys;
import info.smart_tools.smartactors.message_bus.interfaces.imessage_bus_container.exception.SendingMessageException;
import info.smart_tools.smartactors.message_bus.message_bus.MessageBus;
import info.smart_tools.smartactors.scheduler.interfaces.ISchedulerEntry;
import info.smart_tools.smartactors.scheduler.interfaces.ISchedulerEntryStorage;
import info.smart_tools.smartactors.scheduler.interfaces.exceptions.EntryScheduleException;
import info.smart_tools.smartactors.scheduler.interfaces.exceptions.EntryStorageAccessException;
import info.smart_tools.smartactors.task.interfaces.iqueue.IQueue;
import info.smart_tools.smartactors.task.interfaces.itask.ITask;
import info.smart_tools.smartactors.task.interfaces.itask.exception.TaskExecutionException;

/**
 * Checkpoint actor.
 *
 * <p>
 *     Checkpoint actor uses only thread-safe components so may be created as object without synchronization (i.e. "stateless actor").
 * </p>
 */
public class CheckpointActor {
    private static final String FEEDBACK_CHAIN_NAME = "checkpoint_feedback_chain";
    private static final String CHECKPOINT_ACTION = "checkpoint scheduler action";
    private static final long COMPLETE_ENTRY_RESCHEDULE_DELAY = 1000;

    private final IQueue<ITask> taskQueue;
    private final ISchedulerEntryStorage storage;
    private final Object feedbackChainId;

    private final CheckpointSchedulerEntryStorageObserver storageObserver;

    private final IFieldName responsibleCheckpointIdFieldName;
    private final IFieldName checkpointEntryIdFieldName;
    private final IFieldName schedulingFieldName;
    private final IFieldName messageFieldName;
    private final IFieldName prevCheckpointEntryIdFieldName;
    private final IFieldName prevCheckpointIdFieldName;
    private final IFieldName actionFieldName;
    private final IFieldName recoverFieldName;
    private final IFieldName completedFieldName;

    /**
     * Task downloading entries from remote storage.
     */
    private class DownloadEntriesTask implements ITask {

        @Override
        public void execute() throws TaskExecutionException {
            try {
                if (!storage.downloadNextPage(0)) {
                    taskQueue.put(DownloadEntriesTask.this);
                }
            } catch (EntryStorageAccessException e) {
                // TODO: Handle
                throw new TaskExecutionException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * The constructor.
     *
     * @param args    actor description object
     * @throws ResolutionException if error occurs resolving any dependencies
     * @throws ReadValueException if error occurs reading actor description
     * @throws InvalidArgumentException if some methods do not accept our arguments
     * @throws InterruptedException if thread is interrupted while the actor tries to put entries downloading task to task queue
     */
    public CheckpointActor(final IObject args)
            throws ResolutionException, ReadValueException, InvalidArgumentException, InterruptedException {
        //
        responsibleCheckpointIdFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "responsibleCheckpointId");
        checkpointEntryIdFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "checkpointEntryId");
        schedulingFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "scheduling");
        messageFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "message");
        prevCheckpointIdFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "prevCheckpointId");
        prevCheckpointEntryIdFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "prevCheckpointEntryId");
        actionFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "action");
        recoverFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "recover");
        completedFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "completed");

        String connectionOptionsDependency = (String) args.getValue(
                IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "connectionOptionsDependency"));
        String connectionPoolDependency = (String) args.getValue(
                IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "connectionPoolDependency"));
        String collectionName = (String) args.getValue(
                IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "collectionName"));

        taskQueue = IOC.resolve(Keys.getOrAdd("task_queue"));

        storageObserver = new CheckpointSchedulerEntryStorageObserver();

        Object connectionOptions = IOC.resolve(Keys.getOrAdd(connectionOptionsDependency));
        Object connectionPool = IOC.resolve(Keys.getOrAdd(connectionPoolDependency), connectionOptions);
        storage = IOC.resolve(Keys.getOrAdd(ISchedulerEntryStorage.class.getCanonicalName()),
                connectionPool,
                collectionName,
                storageObserver);

        // Start downloading entries from remote storage ...
        taskQueue.put(new DownloadEntriesTask());

        //
        feedbackChainId = IOC.resolve(Keys.getOrAdd("chain_id_from_map_name"), FEEDBACK_CHAIN_NAME);
    }

    /**
     * Handler for messages reached the checkpoint.
     *
     * @param message    the message
     * @throws ReadValueException if error occurs reading something
     * @throws ChangeValueException if error occurs writing something
     * @throws InvalidArgumentException if something goes wrong
     * @throws ResolutionException if error occurs resolving any dependency
     * @throws SendingMessageException if error occurs sending feedback message
     * @throws SerializeException if error occurs serializing message
     */
    public void enter(final EnteringMessage message)
            throws ReadValueException, InvalidArgumentException, ResolutionException, ChangeValueException,
            SendingMessageException, SerializeException {
        IObject originalCheckpointStatus = message.getCheckpointStatus();

        if (null != originalCheckpointStatus) {
            ISchedulerEntry presentEntry = storageObserver
                    .getPresentEntry(originalCheckpointStatus.getValue(checkpointEntryIdFieldName).toString());

            if (null != presentEntry) {
                // If this checkpoint already received the message and has entry for it ...
                // Notify (again) previous checkpoint
                sendFeedback(originalCheckpointStatus, message.getCheckpointId(),
                        presentEntry.getId());

                // And stop processing of the message
                message.getProcessor().getSequence().end();
                return;
            }
        }

        IObject entryArguments = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));

        entryArguments.setValue(schedulingFieldName, message.getSchedulingConfiguration());
        entryArguments.setValue(messageFieldName, cloneMessage(message.getMessage()));

        if (null != originalCheckpointStatus) {
            entryArguments.setValue(prevCheckpointEntryIdFieldName,
                    originalCheckpointStatus.getValue(responsibleCheckpointIdFieldName));
            entryArguments.setValue(prevCheckpointIdFieldName,
                    originalCheckpointStatus.getValue(checkpointEntryIdFieldName));
        }

        // Checkpoint action will initialize recover strategy
        entryArguments.setValue(recoverFieldName, message.getRecoverConfiguration());
        entryArguments.setValue(actionFieldName, CHECKPOINT_ACTION);

        ISchedulerEntry entry = IOC.resolve(Keys.getOrAdd("new scheduler entry"), entryArguments, storage);

        // Update checkpoint status in message.
        // Checkpoint status of re-sent messages will be set by checkpoint scheduler action.
        IObject newCheckpointStatus = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));

        newCheckpointStatus.setValue(responsibleCheckpointIdFieldName, message.getCheckpointId());
        newCheckpointStatus.setValue(checkpointEntryIdFieldName, entry.getId());

        message.setCheckpointStatus(newCheckpointStatus);

        // Send feedback message to previous checkpoint
        if (null != originalCheckpointStatus) {
            sendFeedback(originalCheckpointStatus, message.getCheckpointId(), entry.getId());
        }
    }

    /**
     * Handler for feedback messages.
     *
     * @param message    the message sent by the next checkpoint
     * @throws ReadValueException if error occurs reading values from message
     * @throws EntryScheduleException if error occurs cancelling the entry
     * @throws InvalidArgumentException if something goes wrong
     * @throws ChangeValueException if error occurs updating entry state
     */
    public void feedback(final FeedbackMessage message)
            throws ReadValueException, ChangeValueException, InvalidArgumentException, EntryScheduleException {
        try {
            ISchedulerEntry entry = storage.getEntry(message.getPrevCheckpointEntryId());

            if (null != entry.getState().getValue(completedFieldName)) {
                // If the entry is already completed then ignore the feedback message
                return;
            }

            entry.scheduleNext(System.currentTimeMillis() + COMPLETE_ENTRY_RESCHEDULE_DELAY);

            entry.getState().setValue(completedFieldName, true);
        } catch (EntryStorageAccessException ignore) {
            // There is no entry with required identifier. OK
        }
    }

    private IObject cloneMessage(final IObject message)
            throws SerializeException, ResolutionException {
        String serialized = message.serialize();
        return IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()), serialized);
    }

    private void sendFeedback(final IObject checkpointStatus, final String fromCheckpoint, final String newId)
            throws ResolutionException, SendingMessageException, InvalidArgumentException, ChangeValueException, ReadValueException {
        IObject feedbackMessage = IOC.resolve(Keys.getOrAdd(IObject.class.getCanonicalName()));

        feedbackMessage.setValue(responsibleCheckpointIdFieldName, fromCheckpoint);
        feedbackMessage.setValue(checkpointEntryIdFieldName, newId);
        feedbackMessage.setValue(prevCheckpointIdFieldName, checkpointStatus.getValue(responsibleCheckpointIdFieldName));
        feedbackMessage.setValue(prevCheckpointEntryIdFieldName, checkpointStatus.getValue(checkpointEntryIdFieldName));

        MessageBus.send(feedbackMessage, feedbackChainId);
    }
}
