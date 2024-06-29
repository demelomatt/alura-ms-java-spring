package com.myorg;

import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class AluraAwsInfraStack extends Stack {
    public AluraAwsInfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AluraAwsInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "AluraJavaMsVpc")
                .maxAzs(3)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "AluraJavaMsCluster")
                .vpc(vpc).build();

        ApplicationLoadBalancedFargateService.Builder.create(this, "AluraJavaMsFargate")
                .serviceName("alura-service-hello")
                .cluster(cluster)
                .cpu(256)
                .desiredCount(1)
                .listenerPort(8080)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromRegistry("jacquelineoliveira/ola:1.0"))
                                .containerPort(8080)
                                .containerName("app_hello")
                                .build())
                .memoryLimitMiB(512)
                .publicLoadBalancer(true)
                .build();
    }
}
