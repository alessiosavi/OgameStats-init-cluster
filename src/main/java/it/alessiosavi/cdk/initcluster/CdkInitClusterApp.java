package it.alessiosavi.cdk.initcluster;

import software.amazon.awscdk.core.App;

import java.util.Arrays;

public class CdkInitClusterApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkInitClusterStack(app, "OgameStatsCluster");

        app.synth();
    }
}
