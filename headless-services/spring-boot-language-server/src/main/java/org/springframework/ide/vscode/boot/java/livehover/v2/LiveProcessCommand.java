/*******************************************************************************
 * Copyright (c) 2019 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.livehover.v2;

/**
 * @author Martin Lippert
 */
public class LiveProcessCommand {
	
	private String processKey;
	private String label;
	private String action;
	private String projectName;
	
	public LiveProcessCommand(String action, String processKey, String label, String projectName) {
		super();
		this.processKey = processKey;
		this.label = label;
		this.action = action;
		this.projectName = projectName;
	}

	public String getProcessKey() {
		return processKey;
	}

	public String getLabel() {
		return label;
	}
	
	public String getAction() {
		return action;
	}

	@Override
	public String toString() {
		return "LiveProcessCommand [processKey=" + processKey + ", label=" + label + ", action=" + action + "]";
	}

	public String getProjectName() {
		return projectName;
	}
}
