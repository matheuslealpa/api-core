package io.github.matheuslealpa.apicore.config;

/**
 * api-core version read from the jar manifest ({@code Implementation-Version}).
 */
final class ApiCoreVersion {

    private ApiCoreVersion() {
    }

    static String get() {
        String version = ApiCoreVersion.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }
}
