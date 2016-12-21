module org.apache.felix.gogo.runtime {
	exports org.apache.felix.gogo.runtime;
	exports org.apache.felix.gogo.runtime.activator;
	exports org.apache.felix.gogo.runtime.threadio;
	exports org.apache.felix.service.command;
	exports org.apache.felix.service.threadio;
	requires java.logging;
	requires static org.osgi.service.event;
	requires static org.osgi.core;
}