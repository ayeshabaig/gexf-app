package edu.umuc.swen670.gexf.internal.io;

import java.io.InputStream;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.group.CyGroupSettingsManager;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskIterator;

public class GEXFNetworkReaderFactory implements InputStreamTaskFactory {

	private final CyFileFilter _cyFileFilter;
	private final CyNetworkViewFactory _cyNetworkViewFactory;
	private final CyNetworkFactory _cyNetworkFactory;
	private final CyNetworkManager _cyNetworkManager;
	private final CyRootNetworkManager _cyRootNetworkManager;
	private final CyEventHelper _cyEventHelper;
	private final CyGroupFactory _cyGroupFactory;
	private final CyGroupManager _cyGroupManager;
	private final CyGroupSettingsManager _cyGroupSettingsManager;
	private final VisualMappingFunctionFactory _passthroughMapper;
	private final VisualMappingManager _visualMappingManager;

	public GEXFNetworkReaderFactory(final CyFileFilter cyFileFilter, final CyNetworkViewFactory cyNetworkViewFactory,
			final CyNetworkFactory cyNetworkFactory, final CyNetworkManager cyNetworkManager,
			final CyRootNetworkManager cyRootNetworkManager, final CyEventHelper cyEventHelper,
			final CyGroupFactory cyGroupFactory, final CyGroupManager cyGroupManager, final CyGroupSettingsManager cyGroupSettingsManager,
			final VisualMappingFunctionFactory passthroughMapper, final VisualMappingManager visualMappingManager) {
		_cyFileFilter = cyFileFilter;
		_cyNetworkViewFactory = cyNetworkViewFactory;
		_cyNetworkFactory = cyNetworkFactory;
		_cyNetworkManager = cyNetworkManager;
		_cyRootNetworkManager = cyRootNetworkManager;
		_cyEventHelper = cyEventHelper;
		_cyGroupFactory = cyGroupFactory;
		_cyGroupManager = cyGroupManager;
		_cyGroupSettingsManager = cyGroupSettingsManager;
		_passthroughMapper = passthroughMapper;
		_visualMappingManager = visualMappingManager;
	}


	@Override
	public CyFileFilter getFileFilter() {
		return _cyFileFilter;
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {
		return new TaskIterator(new GEXFNetworkReader(is, _cyNetworkViewFactory, _cyNetworkFactory, _cyNetworkManager, 
													  _cyRootNetworkManager, _cyEventHelper, _cyGroupFactory, _cyGroupManager, 
													  _cyGroupSettingsManager, _passthroughMapper, _visualMappingManager));
	}

	@Override
	public boolean isReady(InputStream is, String inputName) {
		return true;
	}

}
