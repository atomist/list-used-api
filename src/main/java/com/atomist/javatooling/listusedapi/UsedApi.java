package com.atomist.javatooling.listusedapi;

import com.google.common.collect.Sets;

import java.util.Set;

public class UsedApi {
	private Set<String> methods;
	private Set<String> classes;
	private Set<String> annotations;

	public UsedApi() {
		this.methods = Sets.newTreeSet();
		this.classes = Sets.newTreeSet();
		this.annotations = Sets.newTreeSet();
	}

	public UsedApi(Set<String> methods, Set<String> classes, Set<String> annotations) {
		this.methods = methods;
		this.classes = classes;
		this.annotations = annotations;
	}

	public static UsedApi merge(UsedApi a, UsedApi b) {
		UsedApi merged = new UsedApi();
		merged.annotations.addAll(a.annotations);
		merged.annotations.addAll(b.annotations);
		merged.methods.addAll(a.methods);
		merged.methods.addAll(b.methods);
		merged.classes.addAll(a.classes);
		merged.classes.addAll(b.classes);
		return merged;
	}

	public Set<String> getMethods() {
		return methods;
	}

	public Set<String> getClasses() {
		return classes;
	}

	public Set<String> getAnnotations() {
		return annotations;
	}
}
