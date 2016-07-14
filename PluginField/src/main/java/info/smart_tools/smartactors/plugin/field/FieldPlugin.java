package info.smart_tools.smartactors.plugin.field;

import info.smart_tools.smartactors.core.bootstrap_item.BootstrapItem;
import info.smart_tools.smartactors.core.create_new_instance_strategy.CreateNewInstanceStrategy;
import info.smart_tools.smartactors.core.ibootstrap.IBootstrap;
import info.smart_tools.smartactors.core.ibootstrap_item.IBootstrapItem;
import info.smart_tools.smartactors.core.ifield.IField;
import info.smart_tools.smartactors.core.ifield_name.IFieldName;
import info.smart_tools.smartactors.core.iioccontainer.exception.RegistrationException;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.ikey.IKey;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.iplugin.IPlugin;
import info.smart_tools.smartactors.core.iplugin.exception.PluginException;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import info.smart_tools.smartactors.core.wrapper_generator.Field;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Plugin for registration of IOC strategy for Field
 */
public class FieldPlugin implements IPlugin {

    private final IBootstrap<IBootstrapItem<String>> bootstrap;

    /**
     * Constructor
     * @param bootstrap bootstrap element
     */
    public FieldPlugin(final IBootstrap<IBootstrapItem<String>> bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void load() throws PluginException {

        try {
            ConcurrentMap<String, IField> fieldMap = new ConcurrentHashMap<>();
            IBootstrapItem<String> item = new BootstrapItem("FieldPlugin");
            item
                .after("IOC")
                .process(() -> {
                    try {
                        IKey fieldKey = Keys.getOrAdd(IField.class.toString());
                        IOC.register(fieldKey, new CreateNewInstanceStrategy(
                            (args) -> {
                                String fieldName = String.valueOf(args[0]);
                                IField field = fieldMap.get(fieldName);
                                if (field == null) {
                                    try {
                                        //TODO:: clarify key name
                                        field = new Field(IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), fieldName));
                                        fieldMap.putIfAbsent(fieldName, field);
                                    } catch (InvalidArgumentException | ResolutionException e) {
                                        throw new RuntimeException("Can't resolve field: ", e);
                                    }
                                }
                                return field;
                            }));
                    } catch (RegistrationException | InvalidArgumentException | ResolutionException e) {
                        throw new RuntimeException(e);
                    }
                });
            bootstrap.add(item);
        } catch (InvalidArgumentException e) {
            throw new PluginException("Can't load field plugin", e);
        }
    }
}
