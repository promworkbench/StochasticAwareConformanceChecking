package org.processmining.stochasticawareconformancechecking.cli;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionID;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.packages.PackageDescriptor;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginContextID;
import org.processmining.framework.plugin.PluginDescriptor;
import org.processmining.framework.plugin.PluginDescriptorID;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginManager;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.plugin.ProMFuture;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.RecursiveCallException;
import org.processmining.framework.plugin.events.Logger.ListenerList;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.framework.plugin.impl.FieldSetException;
import org.processmining.framework.providedobjects.ProvidedObjectDeletedException;
import org.processmining.framework.providedobjects.ProvidedObjectID;
import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.behavioral.MarkingsetNetConnection;
import org.processmining.models.connections.petrinets.structural.PlaceInvariantConnection;

import gnu.trove.set.hash.THashSet;

public class FakeContext implements PluginContext {

	public static Executor executor = Executors.newCachedThreadPool();

	@SuppressWarnings("unchecked")
	public <T, C extends Connection> T tryToFindOrConstructFirstObject(Class<T> type, Class<C> connectionType,
			String role, Object... input) throws ConnectionCannotBeObtained {
		//make woflan work
		throw new ConnectionCannotBeObtained("it's fake", connectionType, input);
	}

	public <T, C extends Connection> T tryToFindOrConstructFirstNamedObject(Class<T> type, String name,
			Class<C> connectionType, String role, Object... input) throws ConnectionCannotBeObtained {
		return null;
	}

	public <T, C extends Connection> Collection<T> tryToFindOrConstructAllObjects(Class<T> type,
			Class<C> connectionType, String role, Object... input) throws ConnectionCannotBeObtained {
		return null;
	}

	public void invokePlugin(PluginDescriptor plugin, int index, Object... objects) {

	}

	public void invokeBinding(PluginParameterBinding binding, Object... objects) {

	}

	public ProvidedObjectManager getProvidedObjectManager() {
		return new ProvidedObjectManager() {

			public void setEnabled(boolean enabled) {
				// TODO Auto-generated method stub

			}

			public void relabelProvidedObject(ProvidedObjectID id, String label) throws ProvidedObjectDeletedException {
				// TODO Auto-generated method stub

			}

			public boolean isEnabled() {
				// TODO Auto-generated method stub
				return false;
			}

			public List<ProvidedObjectID> getProvidedObjects() {
				return new ArrayList<>();
			}

			public Class<?> getProvidedObjectType(ProvidedObjectID id) throws ProvidedObjectDeletedException {
				// TODO Auto-generated method stub
				return null;
			}

			public Object getProvidedObjectObject(ProvidedObjectID id, boolean waitIfFuture)
					throws ProvidedObjectDeletedException {
				// TODO Auto-generated method stub
				return null;
			}

			public org.processmining.framework.plugin.events.ProvidedObjectLifeCycleListener.ListenerList getProvidedObjectLifeCylceListeners() {
				// TODO Auto-generated method stub
				return null;
			}

			public String getProvidedObjectLabel(ProvidedObjectID id) throws ProvidedObjectDeletedException {
				// TODO Auto-generated method stub
				return null;
			}

			public void deleteProvidedObject(ProvidedObjectID id) throws ProvidedObjectDeletedException {
				// TODO Auto-generated method stub

			}

			public List<ProvidedObjectID> createProvidedObjects(PluginContext context) {
				// TODO Auto-generated method stub
				return null;
			}

			public <T> ProvidedObjectID createProvidedObject(String name, T object, Class<? super T> type,
					PluginContext context) {
				// TODO Auto-generated method stub
				return null;
			}

			public <T> ProvidedObjectID createProvidedObject(String name, T object, PluginContext context) {
				// TODO Auto-generated method stub
				return null;
			}

			public void clear() {
				// TODO Auto-generated method stub

			}

			public void changeProvidedObjectObject(ProvidedObjectID id, Object newObject)
					throws ProvidedObjectDeletedException {
				// TODO Auto-generated method stub

			}
		};
	}

	public PluginManager getPluginManager() {
		return new PluginManager() {

			public void removeListener(PluginManagerListener listener) {

			}

			public void register(URL url, PackageDescriptor pack, ClassLoader loader) {

			}

			public void register(URL url, PackageDescriptor pack) {

			}

			public boolean isParameterAssignable(Class<?> instanceType, Class<?> requestedType) {
				return false;
			}

			public Set<Pair<Integer, PluginDescriptor>> getPluginsResultingIn(Class<? extends Object> resultType,
					Class<? extends PluginContext> contextType, boolean mustBeUserVisible) {
				return null;
			}

			public Set<PluginParameterBinding> getPluginsAcceptingOrdered(Class<? extends PluginContext> contextType,
					boolean mustBeUserVisible, Class<?>... parameters) {
				return null;
			}

			public Set<PluginParameterBinding> getPluginsAcceptingInAnyOrder(Class<? extends PluginContext> contextType,
					boolean mustBeUserVisible, Class<?>... parameters) {
				return null;
			}

			public Set<PluginParameterBinding> getPluginsAcceptingAtLeast(Class<? extends PluginContext> contextType,
					boolean mustBeUserVisible, Class<?>... parameters) {
				return null;
			}

			public PluginDescriptor getPlugin(String id) {
				return null;
			}

			public PluginDescriptor getPlugin(PluginDescriptorID id) {
				return null;
			}

			public Set<Class<?>> getKnownObjectTypes() {
				return null;
			}

			public Set<Class<?>> getKnownClassesAnnotatedWith(Class<? extends Annotation> annotationType) {
				return null;
			}

			public SortedSet<PluginDescriptor> getAllPlugins(boolean mustBeVisible) {
				return null;
			}

			public SortedSet<PluginDescriptor> getAllPlugins() {
				return null;
			}

			public Set<Pair<Integer, PluginParameterBinding>> find(Class<? extends Annotation> annotation,
					Class<?> resultType, Class<? extends PluginContext> contextType, boolean totalMatch,
					boolean orderedParameters, boolean mustBeUserVisible, Class<?>... args) {
				THashSet<Pair<Integer, PluginParameterBinding>> result = new THashSet<>();
				return result;
			}

			public void addListener(PluginManagerListener listener) {

			}

			public Set<Pair<Integer, PluginParameterBinding>> find(Class<? extends Annotation> annotation,
					Class<?>[] resultTypes, Class<? extends PluginContext> contextType, boolean totalMatch,
					boolean orderedParameters, boolean mustBeUserVisible, Class<?>... parameters) {
				return null;
			}
		};
	}

	public Class<? extends PluginContext> getPluginContextType() {
		return null;
	}

	public ConnectionManager getConnectionManager() {
		return new ConnectionManager() {

			public void setEnabled(boolean isEnabled) {

			}

			public boolean isEnabled() {
				return false;
			}

			public <T extends Connection> T getFirstConnection(Class<T> connectionType, PluginContext context,
					Object... objects) throws ConnectionCannotBeObtained {
				//hack to get Woflan to work
				if (connectionType == PlaceInvariantConnection.class) {
					return null;
//				} else if (connectionType == InitialMarkingConnection.class) {
//					return null;
				} else if (connectionType == MarkingsetNetConnection.class) {
					return null;
				}
				throw new ConnectionCannotBeObtained("it's fake", connectionType, objects);
			}

			public <T extends Connection> Collection<T> getConnections(Class<T> connectionType, PluginContext context,
					Object... objects) throws ConnectionCannotBeObtained {
				throw new ConnectionCannotBeObtained("it's fake", connectionType, objects);
			}

			public org.processmining.framework.plugin.events.ConnectionObjectListener.ListenerList getConnectionListeners() {
				return null;
			}

			public Collection<ConnectionID> getConnectionIDs() {
				return null;
			}

			public Connection getConnection(ConnectionID id) throws ConnectionCannotBeObtained {
				return null;
			}

			public void clear() {

			}

			public <T extends Connection> T addConnection(T connection) {
				return null;
			}
		};
	}

	public PluginContextID createNewPluginContextID() {
		return null;
	}

	public void setPluginDescriptor(PluginDescriptor descriptor, int methodIndex)
			throws FieldSetException, RecursiveCallException {

	}

	public void setFuture(PluginExecutionResult resultToBe) {

	}

	public void log(String message, MessageLevel level) {

	}

	public void log(Throwable exception) {

	}

	public void log(String message) {

	}

	public boolean isDistantChildOf(PluginContext context) {
		return false;
	}

	public boolean hasPluginDescriptorInPath(PluginDescriptor descriptor, int methodIndex) {
		return false;
	}

	public PluginContext getRootContext() {
		return null;
	}

	public PluginExecutionResult getResult() {
		return null;
	}

	public org.processmining.framework.plugin.events.ProgressEventListener.ListenerList getProgressEventListeners() {
		return null;
	}

	public Progress getProgress() {
		return new Progress() {

			public void setValue(int value) {

			}

			public void setMinimum(int value) {

			}

			public void setMaximum(int value) {

			}

			public void setIndeterminate(boolean makeIndeterminate) {

			}

			public void setCaption(String message) {

			}

			public boolean isIndeterminate() {
				return false;
			}

			public boolean isCancelled() {
				return false;
			}

			public void inc() {

			}

			public int getValue() {
				return 0;
			}

			public int getMinimum() {
				return 0;
			}

			public int getMaximum() {
				return 0;
			}

			public String getCaption() {
				return null;
			}

			public void cancel() {

			}
		};
	}

	public org.processmining.framework.plugin.events.PluginLifeCycleEventListener.List getPluginLifeCycleEventListeners() {
		return null;
	}

	public Pair<PluginDescriptor, Integer> getPluginDescriptor() {
		return null;
	}

	public PluginContext getParentContext() {
		return null;
	}

	public ListenerList getLoggingListeners() {
		return null;
	}

	public String getLabel() {
		return null;
	}

	public PluginContextID getID() {
		return null;
	}

	public ProMFuture<?> getFutureResult(int i) {
		return new ProMFuture<Object>(Object.class, "") {
			protected Object doInBackground() throws Exception {
				return null;
			}
		};
	}

	public Executor getExecutor() {
		return executor;
	}

	public List<PluginContext> getChildContexts() {
		return null;
	}

	public boolean deleteChild(PluginContext child) {
		return false;
	}

	public PluginContext createChildContext(String label) {
		return null;
	}

	public void clear() {

	}

	public <T extends Connection> T addConnection(T c) {
		return null;
	}

}
