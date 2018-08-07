/*******************************************************************************
 * Copyright (c) 2017, 2018 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.tooling.boot.ls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.springsource.ide.eclipse.commons.livexp.core.LiveSetVariable;

/**
 * Boot-Java LS extension plugin
 * 
 * @author Alex Boyko
 *
 */
public class BootLanguageServerPlugin extends AbstractUIPlugin {
	
	public static final String ID = "org.springframework.tooling.boot.java.ls";

	private static final Object LSP4E_COMMAND_SYMBOL_IN_WORKSPACE = "org.eclipse.lsp4e.symbolinworkspace";
	
	// The shared instance
	private static BootLanguageServerPlugin plugin;

	public BootLanguageServerPlugin() {
		// Empty
	}

	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		super.start(context);
		
		deactivateDuplicateKeybindings();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}
	
	public static BootLanguageServerPlugin getDefault() {
		return plugin;
	}

	private void deactivateDuplicateKeybindings() {
		IBindingService service = PlatformUI.getWorkbench().getService(IBindingService.class);
		if (service != null) {
			List<Binding> newBindings = new ArrayList<>();
			Binding[] bindings = service.getBindings();

			for (Binding binding : bindings) {
				String commandId = null;

				if (binding != null && binding.getParameterizedCommand() != null && binding.getParameterizedCommand().getCommand() != null) {
					commandId = binding.getParameterizedCommand().getCommand().getId();

					if (commandId == null) {
						newBindings.add(binding);
					}
					else if (!commandId.equals(LSP4E_COMMAND_SYMBOL_IN_WORKSPACE)) {
						newBindings.add(binding);
					}
				}
				else {
					newBindings.add(binding);
				}
			}

			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						service.savePreferences(service.getActiveScheme(),
								newBindings.toArray(new Binding[newBindings.size()]));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	private static LiveSetVariable<Pair<String,String>> remoteBootApps = new LiveSetVariable<>();
	
	public static LiveSetVariable<Pair<String,String>> getRemoteBootApps() {
		return remoteBootApps;
	}

}
