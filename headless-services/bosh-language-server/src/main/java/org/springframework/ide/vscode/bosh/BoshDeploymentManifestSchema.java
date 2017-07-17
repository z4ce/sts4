/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.bosh;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.ide.vscode.commons.util.Assert;
import org.springframework.ide.vscode.commons.util.Renderable;
import org.springframework.ide.vscode.commons.util.Renderables;
import org.springframework.ide.vscode.commons.util.ValueParsers;
import org.springframework.ide.vscode.commons.yaml.schema.YType;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeFactory;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeFactory.AbstractType;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeFactory.YAtomicType;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeFactory.YBeanType;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeFactory.YContextSensitive;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeFactory.YTypedPropertyImpl;
import org.springframework.ide.vscode.commons.yaml.schema.YTypeUtil;
import org.springframework.ide.vscode.commons.yaml.schema.YTypedProperty;
import org.springframework.ide.vscode.commons.yaml.schema.YamlSchema;
import org.springframework.ide.vscode.commons.yaml.schema.constraints.Constraints;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Kris De Volder
 */
public class BoshDeploymentManifestSchema implements YamlSchema {

	private final YBeanType V2_TOPLEVEL_TYPE;
	private final YBeanType V1_TOPLEVEL_TYPE;
	private final YContextSensitive TOPLEVEL_TYPE;
	private final YTypeUtil TYPE_UTIL;

	private static final ImmutableSet<String> DEPRECATED_V1_PROPS = ImmutableSet.of("resource_pools", "networks", "compilation", "jobs", "disk_pools", "cloud_provider");
	private static final ImmutableSet<String>  SHARED_V1_V2_PROPS = ImmutableSet.of("name", "director_uuid", "releases", "update", "properties");
	private ImmutableList<YType> definitionTypes = null;
		//Note: 'director_uuid' is also deprecated. But its treated separately since it is deprecated and ignored by V2 client no matter what (i.e. deprecated in both schemas)

	public final YTypeFactory f = new YTypeFactory()
			.enableTieredProposals(false)
			.suggestDeprecatedProperties(false);
	public final YType t_string = f.yatomic("String");
	public final YType t_ne_string = f.yatomic("String")
			.parseWith(ValueParsers.NE_STRING);

	public final YType t_strings = f.yseq(t_string);

	public final YAtomicType t_boolean = f.yenum("boolean", "true", "false");
	public final YType t_any = f.yany("Object");
	public final YType t_params = f.ymap(t_string, t_any);
	public final YType t_string_params = f.ymap(t_string, t_string);
	public final YType t_pos_integer = f.yatomic("Positive Integer")
			.parseWith(ValueParsers.POS_INTEGER);
	public final YType t_strictly_pos_integer = f.yatomic("Strictly Positive Integer")
			.parseWith(ValueParsers.integerAtLeast(1));
	public final YType t_uuid = f.yatomic("UUID").parseWith(UUID::fromString);
	public final YType t_integer_or_range = f.yatomic("Integer or Range")
			.parseWith(BoshValueParsers.INTEGER_OR_RANGE);
	private YType t_instance_group_name_def;
	private YType t_stemcell_alias_name_def;
	private YType t_release_name_def;

	public BoshDeploymentManifestSchema() {
		TYPE_UTIL = f.TYPE_UTIL;

		V2_TOPLEVEL_TYPE = createV2Schema();
		V1_TOPLEVEL_TYPE = createV1Schema(V2_TOPLEVEL_TYPE);

		TOPLEVEL_TYPE = f.contextAware("DeploymenManifestV1orV2", (dc) -> {
			boolean looksLikeV1 = dc.getDefinedProperties().stream().anyMatch(DEPRECATED_V1_PROPS::contains);
			return looksLikeV1 ? V1_TOPLEVEL_TYPE : V2_TOPLEVEL_TYPE;
		});
	}

	private YBeanType createV1Schema(AbstractType v2Schema) {
		YBeanType v1Schema = f.ybean("DeploymentManifestV1");
		Map<String, YTypedProperty> v2properties = v2Schema.getPropertiesMap();
		ImmutableSet<String> v1Props = ImmutableSet.<String>builder()
				.addAll(DEPRECATED_V1_PROPS)
				.addAll(SHARED_V1_V2_PROPS)
				.build();
		for (String name : v1Props) {
			YTypedProperty prop = v2properties.get(name);
			Assert.isNotNull(prop);
			v1Schema.addProperty(prop);
		}
		return v1Schema;
	}

	private YBeanType createV2Schema() {
		YBeanType v2Schema = f.ybean("BoshDeploymentManifest");
		addProp(v2Schema, "name", t_ne_string).isPrimary(true);
		addProp(v2Schema, "director_uuid", t_uuid).isDeprecated(
				"bosh v2 CLI no longer checks or requires director_uuid in the deployment manifest. " +
				"To achieve similar safety make sure to give unique deployment names across environments."
		);

		t_instance_group_name_def = f.yatomic("InstanceGroupName")
				.parseWith(ValueParsers.NE_STRING);

		t_stemcell_alias_name_def = f.yatomic("StemcellAliasName")
				.parseWith(ValueParsers.NE_STRING);

		t_release_name_def = f.yatomic("ReleaseName")
				.parseWith(ValueParsers.NE_STRING);

		YAtomicType t_ip_address = f.yatomic("IPAddress"); //TODO: some kind of checking?
		t_ip_address.parseWith(ValueParsers.NE_STRING);

		YAtomicType t_url = f.yatomic("URL");
		t_url.parseWith(BoshValueParsers.url("http", "https", "file"));

		YAtomicType t_network_name = f.yatomic("NetworkName"); //TODO: resolve from 'cloud config' https://www.pivotaltracker.com/story/show/148712155
		t_network_name.parseWith(ValueParsers.NE_STRING);

		YAtomicType t_disk_type = f.yatomic("DiskType"); //TODO: resolve from 'cloud config' https://www.pivotaltracker.com/story/show/148704001
		t_disk_type.parseWith(ValueParsers.NE_STRING);


		YAtomicType t_stemcell_alias = f.yatomic("StemcellAlias"); //TODO: resolve from 'stemcells block' https://www.pivotaltracker.com/story/show/148706041
		t_stemcell_alias.parseWith(ValueParsers.NE_STRING);

		YAtomicType t_vm_extension = f.yatomic("VMExtension"); //TODO: resolve dynamically from 'cloud config' ? https://www.pivotaltracker.com/story/show/148703877
		t_vm_extension.parseWith(ValueParsers.NE_STRING);

		YAtomicType t_vm_type = f.yatomic("VMType"); //TODO: resolve dynamically from 'cloud config' ? https://www.pivotaltracker.com/story/show/148686169
		t_vm_type.parseWith(ValueParsers.NE_STRING);

		YAtomicType t_az = f.yatomic("AvailabilityZone"); //TODO: resolve dynamically from 'cloud config': https://www.pivotaltracker.com/story/show/148704481
		t_az.parseWith(ValueParsers.NE_STRING);

		YBeanType t_network = f.ybean("Network");
		addProp(t_network, "name", t_network_name).isRequired(true);
		addProp(t_network, "static_ips", f.yseq(t_ip_address));
		addProp(t_network, "default", f.yseq(t_ne_string)); //TODO: Can we determine the set of valid values? How?

		YBeanType t_instance_group_env = f.ybean("InstanceGroupEnv");
		addProp(t_instance_group_env, "bosh", t_params);
		addProp(t_instance_group_env, "password", t_ne_string);

		YAtomicType t_version = f.yatomic("Version");
		t_version.addHints("latest");
		t_version.parseWith(ValueParsers.NE_STRING);

		YBeanType t_release = f.ybean("Release");
		addProp(t_release, "name", t_release_name_def).isPrimary(true);
		addProp(t_release, "version", t_version);
		//TODO: the checking here is just 'my best guess'. Unclarity remains:
		//   See: https://github.com/cloudfoundry/docs-bosh/issues/330
		addProp(t_release, "url", t_url);
		addProp(t_release, "sha1", t_ne_string);
		addProp(v2Schema, "releases", f.yseq(t_release)).isRequired(true);
		t_release.require(Constraints.requireAtLeastOneOf("url", "version")); //allthough docs seem to imply you shouldn't define both url and version..
																					//... it seems bosh tolerates it.
		t_release.require(BoshConstraints.SHA1_REQUIRED_FOR_HTTP_URL);

		YBeanType t_stemcell = f.ybean("Stemcell");
		addProp(t_stemcell, "alias", t_stemcell_alias_name_def).isRequired(true);
		addProp(t_stemcell, "version", t_ne_string).isRequired(true);
		addProp(t_stemcell, "name", t_ne_string);
		addProp(t_stemcell, "os", t_ne_string);
		t_stemcell.requireOneOf("name", "os");
		addProp(v2Schema, "stemcells", f.yseq(t_stemcell)).isRequired(true);

		YBeanType t_update = f.ybean("Update");
		addProp(t_update, "canaries", t_strictly_pos_integer).isRequired(true);
		addProp(t_update, "max_in_flight", t_pos_integer).isRequired(true);
		addProp(t_update, "canary_watch_time", t_integer_or_range).isRequired(true);
		addProp(t_update, "update_watch_time", t_integer_or_range).isRequired(true);
		addProp(t_update, "serial", t_boolean);
		addProp(v2Schema, "update", t_update).isRequired(true);

		YBeanType t_job = f.ybean("Job");
		addProp(t_job, "name", t_ne_string).isRequired(true);
		addProp(t_job, "release", t_ne_string).isRequired(true);
		addProp(t_job, "consumes", t_params);
		addProp(t_job, "provides", t_params);
		addProp(t_job, "properties", t_params);

		YBeanType t_instance_group = f.ybean("InstanceGroup");
		addProp(t_instance_group, "name", t_instance_group_name_def).isPrimary(true);
		addProp(t_instance_group, "azs", f.yseq(t_az)).isRequired(true);
		addProp(t_instance_group, "instances", t_pos_integer).isRequired(true); //Strictly positive? Or zero is okay?
		addProp(t_instance_group, "jobs", f.yseq(t_job)).isRequired(true);
		addProp(t_instance_group, "vm_type", t_vm_type).isRequired(true);
		addProp(t_instance_group, "vm_extensions", f.yseq(t_vm_extension));
		addProp(t_instance_group, "stemcell", t_stemcell_alias).isRequired(true);
		addProp(t_instance_group, "persistent_disk_type", t_disk_type);
		addProp(t_instance_group, "networks", f.yseq(t_network)).isRequired(true);
		YType t_update_override = f.ybean("UpdateOverrides", t_update.getProperties()
				.stream()
				.map((YTypedProperty prop) ->
					f.yprop(prop).isRequired(false)
				)
				.toArray(sz -> new YTypedProperty[sz])
		);
		addProp(t_instance_group, "update", t_update_override);
		YType t_migration = t_params; //TODO: https://www.pivotaltracker.com/story/show/148712595
		addProp(t_instance_group, "migrated_from", f.yseq(t_migration));
		addProp(t_instance_group, "lifecycle", f.yenum("WorkloadType", "service", "errand"));
		addProp(t_instance_group, "properties", t_params).isDeprecated("Deprecated in favor of job level properties and links");
		addProp(t_instance_group, "env", t_instance_group_env);

		addProp(v2Schema, "instance_groups", f.yseq(t_instance_group)).isRequired(true);
		addProp(v2Schema, "properties", t_params).isDeprecated("Deprecated in favor of job level properties and links");

		YBeanType t_variable = f.ybean("Variable");
		addProp(t_variable, "name", t_ne_string).isPrimary(true);
		addProp(t_variable, "type", f.yenum("VariableType", "certificate", "password", "rsa", "ssh")).isRequired(true);
		addProp(t_variable, "options", t_params);
		addProp(v2Schema, "variables", f.yseq(t_variable));

		addProp(v2Schema, "tags", t_params);

		for (String v1Prop : DEPRECATED_V1_PROPS) {
			addProp(v2Schema, v1Prop, t_any).isDeprecated("Deprecated: '"+v1Prop+"' is a V1 schema property. Consider migrating your deployment manifest to V2");
		}
		return v2Schema;
	}

	@Override
	public YType getTopLevelType() {
		return TOPLEVEL_TYPE;
	}

	@Override
	public YTypeUtil getTypeUtil() {
		return TYPE_UTIL;
	}

	private YTypedPropertyImpl prop(AbstractType beanType, String name, YType type) {
		YTypedPropertyImpl prop = f.yprop(name, type);
		prop.setDescriptionProvider(descriptionFor(beanType, name));
		return prop;
	}

	public static Renderable descriptionFor(YType owner, String propName) {
		String typeName = owner.toString();
		return Renderables.fromClasspath(BoshDeploymentManifestSchema.class, "/desc/"+typeName+"/"+propName);
	}

	private YTypedPropertyImpl addProp(AbstractType bean, String name, YType type) {
		return addProp(bean, bean, name, type);
	}

	private YTypedPropertyImpl addProp(AbstractType superType, AbstractType bean, String name, YType type) {
		YTypedPropertyImpl p = prop(superType, name, type);
		bean.addProperty(p);
		return p;
	}

	public Collection<YType> getDefinitionTypes() {
		if (definitionTypes==null) {
			definitionTypes = ImmutableList.of(
					t_instance_group_name_def,
					t_stemcell_alias_name_def,
					t_release_name_def
			);
		}
		return definitionTypes;
	}

}
