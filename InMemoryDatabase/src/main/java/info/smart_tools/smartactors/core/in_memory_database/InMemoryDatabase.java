package info.smart_tools.smartactors.core.in_memory_database;

import info.smart_tools.smartactors.core.ds_object.DSObject;
import info.smart_tools.smartactors.core.field_name.FieldName;
import info.smart_tools.smartactors.core.idatabase.IDataBase;
import info.smart_tools.smartactors.core.idatabase.exception.IDataBaseException;
import info.smart_tools.smartactors.core.ifield_name.IFieldName;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.iobject.exception.ChangeValueException;
import info.smart_tools.smartactors.core.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.core.iobject.exception.SerializeException;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of data base on list
 */
public class InMemoryDatabase implements IDataBase {

    Map<String, IConditionVerifier> verifierMap = new HashMap<>();

    public InMemoryDatabase() {

        verifierMap.put("$eq", (condition, document) -> {
                    IFieldName fieldName = condition.iterator().next().getKey();
                    try {
                        Object entry = document.getValue(fieldName);
                        Object reference = ((IObject) condition.getValue(fieldName)).getValue(new FieldName("$eq"));
                        return entry.equals(reference);
                    } catch (ReadValueException | InvalidArgumentException e) {
                    }
                    return false;
                }
        );

        verifierMap.put("$and", (condition, document) -> {
                    boolean result = true;
                    try {
                        List<IObject> conditions = (List<IObject>) condition.getValue(new FieldName("$and"));
                        for (IObject conditionItem : conditions) {
                            result &= verifierMap.get(conditionItem.iterator().next().getKey())
                                    .verify((IObject) conditionItem.iterator().next().getValue(), document);
                        }
                    } catch (ReadValueException | InvalidArgumentException e) {
                    }
                    return result;
                }
        );

    }

    private List<DataBaseItem> list = new LinkedList<>();

    @Override
    public void upsert(final IObject document, final String collectionName) throws IDataBaseException {
        DataBaseItem item = null;
        try {
            item = new DataBaseItem(document, collectionName);
        } catch (ResolutionException | ReadValueException | InvalidArgumentException e) {
            throw new IDataBaseException("Failed to create DataBaseItem", e);
        }
        if (null == item.getId()) {
            insert(item);
        } else {
            update(item);
        }
    }

    @Override
    public void insert(final IObject document, final String collectionName) throws IDataBaseException {
        DataBaseItem item = null;
        try {
            item = new DataBaseItem(document, collectionName);
        } catch (ResolutionException | ReadValueException | InvalidArgumentException e) {
            throw new IDataBaseException("Failed to create DataBaseItem", e);
        }
        insert(item);
    }

    @Override
    public void update(final IObject document, final String collectionName) throws IDataBaseException {
        DataBaseItem item = null;
        try {
            item = new DataBaseItem(document, collectionName);
        } catch (ResolutionException | ReadValueException | InvalidArgumentException e) {
            throw new IDataBaseException("Failed to create DataBaseItem", e);
        }
        update(item);
    }

    private void update(final DataBaseItem item) {
        for (int i = 0; i < list.size(); i++) {
            DataBaseItem inBaseElem = list.get(i);
            if (inBaseElem.getId().equals(item.getId()) && inBaseElem.getCollectionName().equals(item.getCollectionName())) {
                list.remove(i);
                list.add(i, item);
            }
        }
    }

    private void insert(final DataBaseItem item) throws IDataBaseException {
        try {
            item.setId(nextId());
        } catch (ChangeValueException | InvalidArgumentException e) {
            throw new IDataBaseException("Failed to set id to DataBaseItem", e);
        }
        list.add(item);
    }

    @Override
    public IObject getById(final Object id, final String collectionName) {
        for (DataBaseItem item : list) {
            if (item.getId().equals(id) && item.getCollectionName().equals(collectionName)) {
                return item.getDocument();
            }
        }
        return null;
    }

    private Object nextId() {
        return list.size() + 1;
    }

    @Override
    public List<IObject> select(final IObject condition, final String collectionName) throws IDataBaseException {
        List<IObject> outputList = new LinkedList<>();
        for (DataBaseItem item : list) {
            if (Objects.equals(item.getCollectionName(), collectionName)) {
                try {
                    if (IOC.resolve(Keys.getOrAdd("compare_iobject"), condition, item.getDocument())) {
                        outputList.add(clone(item.getDocument()));
                    }
                } catch (ResolutionException e) {
                    throw new IDataBaseException("Failed to compare iobject", e);
                }
            }
        }
        return outputList;
    }

    private IObject clone(final IObject iObject) throws IDataBaseException {
        try {
            return IOC.resolve(Keys.getOrAdd(DSObject.class.getCanonicalName()), iObject.serialize());
        } catch (ResolutionException | SerializeException e) {
            throw new IDataBaseException("Failed to clone IObject", e);
        }
    }

    @Override
    public void delete(IObject document, String collectionName) {

    }
}
