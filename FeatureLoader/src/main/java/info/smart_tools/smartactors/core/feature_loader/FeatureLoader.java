package info.smart_tools.smartactors.core.feature_loader;

import info.smart_tools.smartactors.core.bootstrap.Bootstrap;
import info.smart_tools.smartactors.core.iaction.IBiAction;
import info.smart_tools.smartactors.core.iaction.exception.ActionExecuteException;
import info.smart_tools.smartactors.core.ibootstrap.IBootstrap;
import info.smart_tools.smartactors.core.ibootstrap.exception.ProcessExecutionException;
import info.smart_tools.smartactors.core.ibootstrap.exception.RevertProcessExecutionException;
import info.smart_tools.smartactors.core.iconfiguration_manager.IConfigurationManager;
import info.smart_tools.smartactors.core.iconfiguration_manager.exceptions.ConfigurationProcessingException;
import info.smart_tools.smartactors.core.ifeature_loader.IFeatureLoader;
import info.smart_tools.smartactors.core.ifeature_loader.IFeatureStatus;
import info.smart_tools.smartactors.core.ifeature_loader.exceptions.FeatureLoadException;
import info.smart_tools.smartactors.core.ifield_name.IFieldName;
import info.smart_tools.smartactors.core.iioccontainer.exception.ResolutionException;
import info.smart_tools.smartactors.core.invalid_argument_exception.InvalidArgumentException;
import info.smart_tools.smartactors.core.iobject.IObject;
import info.smart_tools.smartactors.core.iobject.exception.ReadValueException;
import info.smart_tools.smartactors.core.ioc.IOC;
import info.smart_tools.smartactors.core.ipath.IPath;
import info.smart_tools.smartactors.core.iplugin.IPlugin;
import info.smart_tools.smartactors.core.iplugin.exception.PluginException;
import info.smart_tools.smartactors.core.iplugin_creator.IPluginCreator;
import info.smart_tools.smartactors.core.iplugin_creator.exception.PluginCreationException;
import info.smart_tools.smartactors.core.iplugin_loader.IPluginLoader;
import info.smart_tools.smartactors.core.iplugin_loader.exception.PluginLoaderException;
import info.smart_tools.smartactors.core.iplugin_loader_visitor.IPluginLoaderVisitor;
import info.smart_tools.smartactors.core.named_keys_storage.Keys;
import info.smart_tools.smartactors.core.plugin_loader_from_jar.ExpansibleURLClassLoader;
import info.smart_tools.smartactors.core.plugin_loader_from_jar.PluginLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class FeatureLoader implements IFeatureLoader {
    private final ConcurrentHashMap<String, FeatureStatusImpl> statuses = new ConcurrentHashMap<>();

    private final IFieldName featureNameFieldName;
    private final IFieldName afterFeaturesFieldName;
    private final ExpansibleURLClassLoader classLoader = new ExpansibleURLClassLoader(new URL[0], getClass().getClassLoader());
    private final IPluginCreator pluginCreator;
    private final IPluginLoaderVisitor<String> pluginLoaderVisitor;
    private final IConfigurationManager configurationManager;

    private final IBiAction<IObject, IPath> featureLoadAction = this::loadPluginsAndConfig;

    /**
     * The constructor.
     *
     * @throws ResolutionException if fails to resolve any dependencies
     */
    public FeatureLoader()
            throws ResolutionException {
        featureNameFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "featureName");
        afterFeaturesFieldName = IOC.resolve(Keys.getOrAdd(IFieldName.class.getCanonicalName()), "afterFeatures");
        pluginCreator = IOC.resolve(Keys.getOrAdd("plugin creator"));
        pluginLoaderVisitor = IOC.resolve(Keys.getOrAdd("plugin loader visitor"));
        configurationManager = IOC.resolve(Keys.getOrAdd(IConfigurationManager.class.getCanonicalName()));
    }

    @Override
    public void loadGroup(final IPath groupPath)
            throws FeatureLoadException {
        try {
            File groupDir = new File(groupPath.getPath());
            Set<FeatureStatusImpl> statuses = new HashSet<>();

            if (!groupDir.isDirectory()) {
                throw new FeatureLoadException(
                        MessageFormat.format("Feature group directory ({0}) is not a directory.", groupPath.getPath()));
            }

            //noinspection ConstantConditions
            for (File file : groupDir.listFiles(File::isDirectory)) {
                IPath path = IOC.resolve(Keys.getOrAdd(IPath.class.getCanonicalName()), file.getCanonicalPath());
                FeatureStatusImpl status = loadFeature0(path);

                statuses.add(status);
                statuses.addAll(status.getDependencies());
            }

            for (FeatureStatusImpl dependencyStatus : statuses) {
                if (!dependencyStatus.isInitialized()) {
                    throw new FeatureLoadException(
                            MessageFormat.format("Feature named {0} not found.", dependencyStatus.getId()));
                }
            }

            for (FeatureStatusImpl status : statuses) {
                status.load();
            }
        } catch (ResolutionException | IOException | FeatureLoadException | ActionExecuteException e) {
            throw new FeatureLoadException(
                    MessageFormat.format("Error occurred loading features from ''{0}''.", groupPath.getPath()), e);
        }
    }

    @Override
    public IFeatureStatus loadFeature(final IPath featurePath)
            throws FeatureLoadException {
        FeatureStatusImpl status = loadFeature0(featurePath);

        for (FeatureStatusImpl dependencyStatus : status.getDependencies()) {
            if (!dependencyStatus.isLoaded()) {
                throw new FeatureLoadException(
                        MessageFormat.format("Feature ''{0}'' required by ''{1}'' is not loaded.",
                                dependencyStatus.getId(), status.getId()));
            }
        }

        try {
            status.load();
        } catch (ActionExecuteException e) {
            throw new FeatureLoadException("Error occurred loading the feature.", e);
        }

        return status;
    }

    @Override
    public IFeatureStatus getFeatureStatus(final String featureId) {
        return getFeatureStatus0(featureId);
    }

    private FeatureStatusImpl loadFeature0(final IPath featurePath)
            throws FeatureLoadException {
        try {
            IObject featureConfig = readFeatureConfig(featurePath);

            String featureName = (String) featureConfig.getValue(featureNameFieldName);
            List<String> featureDependencies = (List<String>) featureConfig.getValue(afterFeaturesFieldName);

            FeatureStatusImpl status = getFeatureStatus0(featureName);

            if (status.isInitialized()) {
                throw new FeatureLoadException(
                        MessageFormat.format("There already is a feature named ''{0}'' (in directory {1}).",
                                featureName, status.getPath().getPath()));
            }

            status.init(featurePath, featureConfig);

            for (String dependencyId : featureDependencies) {
                status.addDependency(getFeatureStatus0(dependencyId));
            }

            return status;
        } catch (ReadValueException | InvalidArgumentException | FeatureLoadException e) {
            throw new FeatureLoadException(
                    MessageFormat.format("Error occurred loading a feature from ''{0}''.", featurePath.getPath()), e);
        }
    }

    private List<IPath> listJarsIn(final IPath dirPath)
            throws FeatureLoadException {
        try {
            File directory = new File(dirPath.getPath());

            if (!directory.isDirectory()) {
                throw new IOException(MessageFormat.format("File ''{0}'' is not a directory.", directory.getAbsolutePath()));
            }

            File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));

            List<IPath> paths = new LinkedList<>();

            for (File file : files) {
                if (file.isFile()) {
                    IPath path = IOC.resolve(Keys.getOrAdd(IPath.class.getCanonicalName()), file.getCanonicalPath());
                    paths.add(path);
                }
            }

            return paths;
        } catch (IOException | ResolutionException e) {
            throw new FeatureLoadException(MessageFormat.format("Could not list jar files in {0}", dirPath.getPath()), e);
        }
    }

    private FeatureStatusImpl getFeatureStatus0(final String featureId) {
        return statuses.computeIfAbsent(featureId, id -> new FeatureStatusImpl(id, featureLoadAction));
    }

    private IObject readFeatureConfig(final IPath featurePath)
            throws FeatureLoadException {
        try {
            Path configPath = Paths.get(featurePath.getPath(), "config.json");
            String configString = new String(Files.readAllBytes(configPath));
            return IOC.resolve(Keys.getOrAdd("configuration object"), configString);
        } catch (IOException | ResolutionException e) {
            throw new FeatureLoadException("Error occurred reading feature configuration file.", e);
        }
    }

    private void loadPluginsAndConfig(final IObject config, final IPath directory)
            throws ActionExecuteException {
        try {
            loadPluginsFrom(listJarsIn(directory));
            configurationManager.applyConfig(config);
        } catch (FeatureLoadException | InvalidArgumentException | PluginLoaderException | ProcessExecutionException |
                 ConfigurationProcessingException e) {
            throw new ActionExecuteException(e);
        }
    }

    private void loadPluginsFrom(final List<IPath> jars)
            throws InvalidArgumentException, PluginLoaderException, ProcessExecutionException {
        IBootstrap bootstrap = new Bootstrap();
        IPluginLoader<Collection<IPath>> pluginLoader = new PluginLoader(
                classLoader,
                clz -> {
                    try {
                        IPlugin plugin = pluginCreator.create(clz, bootstrap);
                        plugin.load();
                    } catch (PluginCreationException | PluginException e) {
                        throw new ActionExecuteException(e);
                    }
                },
                pluginLoaderVisitor);
        pluginLoader.loadPlugin(jars);

        try {
            bootstrap.start();
        } catch (ProcessExecutionException e) {
            try {
                bootstrap.revert();
            } catch (RevertProcessExecutionException ee) {
                e.addSuppressed(ee);
            }

            throw e;
        }
    }
}
